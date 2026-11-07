#!/usr/bin/env python3
"""
Differential runner for Kotlin/Native incremental compilation (IC) scenarios.

A scenario uses the same layout as native/native.tests/testData/caches/ic/*:
project.info, one directory per module with module.info, and versioned sources (file.kt, file.1.kt, ...).
Cache expectation directives ("added cache: ...") are accepted and ignored: the oracle here is differential.

For every step the runner compiles the klibs once and links the program three times:
  * incrementally, into an IC cache directory that persists across steps (one per variant);
  * cleanly, into a fresh IC cache directory (the reference for staleness findings - it still
    runs the IC pipeline, just from scratch);
  * non-incrementally, with no caches at all (the reference for defects that are already
    present in a from-scratch IC build, which the fresh-cache link cannot expose).
Then it compares link success, compiler diagnostics, program output and the per-file cache state.
After the last step it relinks with no edits and expects the IC to rebuild nothing.

Commands:
  warmup                      build the distribution caches for the link flavor (run once, before parallel runs)
  run <scenario> --out DIR    run a scenario, write DIR/result.json and DIR/summary.txt, print the summary
  reduce <scenario> --out DIR shrink a failing scenario (steps, edits, files) while the same finding reproduces
  corpus [--filter S]         run scenarios from the existing IC test corpus (harness self-validation)

Python 3.9 compatible, no third-party packages.
"""

import argparse
import copy
import difflib
import json
import os
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
DEFAULT_DIST = REPO / "kotlin-native" / "dist"
CORPUS_DIR = REPO / "native" / "native.tests" / "testData" / "caches" / "ic"

MAIN_MODULE = "main"
EXTERNAL_PREFIX = "external"

# Same flags as AbstractNativeIncrementalCompilationTest: without it some inline function bugs are hidden.
LIBRARY_FLAGS = ["-XXLanguage:-IrIntraModuleInlinerBeforeKlibSerialization"]
# Debug link as in KGP: IC is only enabled for debug binaries. Partial linkage errors as in the test fixture.
LINK_FLAGS = ["-g", "-Xpartial-linkage-loglevel=error"]

VARIANTS = {
    "default": [],
    "unbatched": ["-Xbinary=batchedCacheBuild=false"],
    "threads4": ["-Xbackend-threads=4"],
}

COMPILE_TIMEOUT = 900
RUN_TIMEOUT = 30

STEP_RE = re.compile(r"^\s*STEP\s+(\d+)\.*(\d+)?\s*:?$")
MOD_RE = re.compile(r"^([UD])\s*:(.+)$")

# Bug findings break the "IC == clean build" contract. Latent ones show stale IC state that did not (yet) change the binary.
SEVERITY = {
    "IC_CRASH": "bug",
    "IC_LINK_FAILURE": "bug",
    "IC_ACCEPTS_BROKEN": "bug",
    "OUTPUT_MISMATCH": "bug",
    "DIAGNOSTICS_MISMATCH": "bug",
    "COMPILER_CRASH": "bug",  # Crashes the clean build as well: a compiler bug, but not an IC one.
    # The IC link fails (or crashes) where a non-IC build of the same klibs succeeds: a defect of the
    # IC pipeline itself, present in a from-scratch IC build too (the fresh-cache link cannot see it).
    "IC_ONLY_LINK_FAILURE": "bug",
    # A from-scratch IC build already produces a different program than a non-IC build.
    "IC_ONLY_OUTPUT_MISMATCH": "bug",
    "CACHE_STATE_MISMATCH": "latent",
    "STALE_CACHE_DIR": "latent",
    "NOOP_REBUILD": "perf",
    "UNSTABLE_FINGERPRINT": "perf",
    # By design: a file that IC did not rebuild keeps the dependencies recorded for its (still valid) old code.
    "DEPS_DRIFT": "info",
    # The cache of a library that left the project stays on disk.
    "STALE_LIBRARY_CACHE": "info",
    # The non-IC link fails, e.g. on a step that relies on partial linkage stubbing; the non-IC
    # comparison does not apply there. Reported for completeness; does not change the verdict.
    "NOIC_LINK_FAILURE": "info",
}
# "info" findings are reported but do not change the verdict.
SEVERITY_ORDER = ["bug", "latent", "invalid", "perf", "pass"]


# ---------------------------------------------------------------------------------------------------------------------
# Scenario model, mirrors compiler/tests-compiler-utils/testFixtures/org/jetbrains/kotlin/codegen/TestModel.kt

class ModuleStep(object):
    def __init__(self):
        self.dependencies = []
        self.friends = []
        self.modifications = []  # ("U", from, to) or ("D", file, None)
        self.arguments = []
        self.directives = {}  # Other directives, e.g. "added cache" or "expected compilation failure".


class Scenario(object):
    def __init__(self, root):
        self.root = Path(root)
        self.name = self.root.name
        self.modules = []
        self.steps = []  # [(step id, [module order])]
        self.module_steps = {}  # module -> {step id -> ModuleStep}


def _content_lines(path):
    for raw in path.read_text().splitlines():
        line = raw.strip()
        if not line or line.startswith("//"):
            continue
        yield line


def _split_list(value):
    return [it for it in re.split(r"[,\s]+", value.strip()) if it]


def _native_op(op):
    # "libs.js" style ops address one target only; keep the target-independent ones.
    parts = op.split(".")
    return parts[0].strip() if len(parts) == 1 else None


def parse_scenario(root):
    scenario = Scenario(root)
    project_info = scenario.root / "project.info"
    if not project_info.exists():
        raise ValueError("no project.info in %s" % root)

    current = None
    for line in _content_lines(project_info):
        if line == "MUTED":
            continue
        m = STEP_RE.match(line)
        if m:
            first, last = int(m.group(1)), int(m.group(2) or m.group(1))
            current = []
            for step_id in range(first, last + 1):
                scenario.steps.append((step_id, current))
            continue
        if ":" not in line:
            raise ValueError("syntax error in project.info: %r" % line)
        op, value = line.split(":", 1)
        op = _native_op(op)
        if op is None:
            continue
        if op == "MODULES":
            scenario.modules += _split_list(value)
        elif op == "libs" and current is not None:
            current += _split_list(value)
    for index, (step_id, _) in enumerate(scenario.steps):
        if step_id != index:
            raise ValueError("steps must be numbered 0, 1, 2, ...; got %d at position %d" % (step_id, index))
    if MAIN_MODULE not in scenario.modules:
        raise ValueError("MODULES must contain '%s'" % MAIN_MODULE)

    for module in scenario.modules:
        info = scenario.root / module / "module.info"
        if not info.exists():
            raise ValueError("no module.info for module %s" % module)
        scenario.module_steps[module] = parse_module_info(info)
    return scenario


def parse_module_info(path):
    steps = {}
    current = []  # ModuleSteps of the current STEP line (a range shares one object).
    in_modifications = False
    for line in _content_lines(path):
        m = STEP_RE.match(line)
        if m:
            first, last = int(m.group(1)), int(m.group(2) or m.group(1))
            step = ModuleStep()
            current = [step]
            for step_id in range(first, last + 1):
                if step_id in steps:
                    raise ValueError("%s: step %d redeclared" % (path, step_id))
                steps[step_id] = step
            in_modifications = False
            continue
        if not current:
            raise ValueError("%s: content before the first STEP: %r" % (path, line))
        step = current[0]
        mm = MOD_RE.match(line)
        if in_modifications and mm:
            if mm.group(1) == "U":
                source, target = [it.strip() for it in mm.group(2).split("->")]
                step.modifications.append(("U", source, target))
            else:
                step.modifications.append(("D", mm.group(2).strip(), None))
            continue
        in_modifications = False
        if ":" not in line:
            raise ValueError("%s: syntax error: %r" % (path, line))
        op, value = line.split(":", 1)
        op = _native_op(op)
        if op is None:
            continue
        if op == "dependencies":
            step.dependencies += _split_list(value)
        elif op == "friends":
            step.friends += _split_list(value)
        elif op == "modifications":
            in_modifications = True
        elif op == "arguments":
            step.arguments += value.split()
        else:
            step.directives.setdefault(op, []).extend(_split_list(value))
    return steps


def write_scenario(scenario, out_dir):
    """Writes the scenario model back, copying the referenced source files. Used by the reducer."""
    out_dir = Path(out_dir)
    if out_dir.exists():
        shutil.rmtree(out_dir)
    out_dir.mkdir(parents=True)
    lines = ["MODULES: " + ", ".join(scenario.modules), ""]
    for step_id, order in scenario.steps:
        lines += ["STEP %d:" % step_id, "    libs: " + ", ".join(order), ""]
    (out_dir / "project.info").write_text("\n".join(lines))
    for module in scenario.modules:
        module_dir = out_dir / module
        module_dir.mkdir()
        lines = []
        for step_id in sorted(scenario.module_steps[module]):
            step = scenario.module_steps[module][step_id]
            lines.append("STEP %d:" % step_id)
            if step.dependencies:
                lines.append("    dependencies: " + ", ".join(step.dependencies))
            if step.friends:
                lines.append("    friends: " + ", ".join(step.friends))
            if step.arguments:
                lines.append("    arguments: " + " ".join(step.arguments))
            if step.modifications:
                lines.append("    modifications:")
                for kind, a, b in step.modifications:
                    lines.append("        U : %s -> %s" % (a, b) if kind == "U" else "        D : %s" % a)
                    if kind == "U":
                        source = scenario.root / module / a
                        if source.exists() and not (module_dir / a).exists():
                            shutil.copy2(source, module_dir / a)
            for directive, values in step.directives.items():
                lines.append("    %s: %s" % (directive, ", ".join(values)))
            lines.append("")
        (module_dir / "module.info").write_text("\n".join(lines))
    # Keep auxiliary files (expected-output pattern files, headers) as they are.
    for module in scenario.modules:
        for path in (scenario.root / module).iterdir():
            target = out_dir / module / path.name
            if path.is_file() and not target.exists() and path.suffix not in (".kt", ".def") and path.name != "module.info":
                shutil.copy2(path, target)


# ---------------------------------------------------------------------------------------------------------------------
# Running tools

class Tools(object):
    def __init__(self, dist):
        self.dist = Path(dist)
        self.kotlinc = self.dist / "bin" / "kotlinc-native"
        self.cinterop = self.dist / "bin" / "cinterop"
        if not self.kotlinc.exists():
            raise SystemExit("kotlinc-native not found at %s; build the dist or pass --dist" % self.kotlinc)
        # Older compilers (e.g. to check that known, already fixed bugs are found) lack some of the flags.
        help_text = subprocess.run([str(self.kotlinc), "-X"], stdout=subprocess.PIPE, stderr=subprocess.STDOUT).stdout
        self.can_dump_built_caches = b"-Xdump-built-caches-to" in help_text


def run_process(cmd, log_path, timeout, cwd=None):
    started = time.time()
    try:
        proc = subprocess.run(cmd, cwd=cwd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=timeout)
        output = proc.stdout.decode("utf-8", "replace")
        code = proc.returncode
    except subprocess.TimeoutExpired as e:
        output = (e.stdout or b"").decode("utf-8", "replace") + "\n<<TIMEOUT after %ds>>" % timeout
        code = -999
    elapsed = time.time() - started
    if log_path is not None:
        Path(log_path).write_text("$ " + " ".join(str(c) for c in cmd) + "\n" + output)
    return code, output, elapsed


FRAME_RE = re.compile(r"^\s*at (org\.jetbrains\.kotlin\.[\w.$<>]+)\(", re.MULTILINE)
EXCEPTION_RE = re.compile(r"\b((?:java|kotlin|org\.jetbrains\.kotlin)[\w.$]*\.[A-Z]\w*(?:Exception|Error))\b")


# Thrown for ordinary compilation errors, e.g. the expected partial linkage errors in a spawned cache build.
BENIGN_EXCEPTIONS = {"org.jetbrains.kotlin.analyzer.CompilationErrorException"}


def crash_exceptions(output):
    return [e for e in EXCEPTION_RE.findall(output) if e not in BENIGN_EXCEPTIONS]


def is_crash(output):
    if "internal compiler error" in output or "Please report this problem" in output:
        return True
    # An exception name alone may come from a diagnostic message; a crash also prints a stack trace.
    return bool(crash_exceptions(output)) and re.search(r"^\s+at [\w.$<>]+\(", output, re.MULTILINE) is not None


def crash_signature(output):
    exceptions = crash_exceptions(output)
    frames = []
    for frame in FRAME_RE.findall(output):
        if frame not in frames:
            frames.append(frame)
        if len(frames) == 3:
            break
    return "%s @ %s" % (exceptions[0] if exceptions else "?", " < ".join(frames) or "?")


def normalize_line(line, out_dir):
    line = line.replace(str(out_dir), "<OUT>")
    line = re.sub(r"/[^\s:'\"]*/kotlin-native/dist", "<DIST>", line)
    line = re.sub(r"\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b", "<UUID>", line)
    line = re.sub(r"\(\d+ ms( total)?\)", "(N ms)", line)
    return line


def diagnostics(output, out_dir):
    """Compiler warnings and errors, normalized; the -verbose 'logging:' noise is dropped."""
    result = []
    for line in output.splitlines():
        if re.match(r"^(w|e|warning|error): ", line) and not line.startswith("logging:"):
            # The cache directories and executables differ between the two links by construction.
            line = re.sub(r"<OUT>/(ic|auto|clean)/[^\s:]*", "<CACHE_DIR>", normalize_line(line, out_dir))
            line = re.sub(r"<OUT>/steps/[^\s:]*\.kexe", "<EXE>", line)
            result.append(line)
    return result


def first_error(output, out_dir):
    lines = output.splitlines()
    # Linker errors name the symbol on the line after the header; that is the useful part.
    for i, line in enumerate(lines):
        if line.startswith("Undefined symbols") or line.startswith("duplicate symbol"):
            return normalize_line(" ".join(l.strip() for l in lines[i:i + 3]), out_dir)[:300]
    for line in lines:
        if re.match(r"^(e|error): ", line) or line.startswith("ld: "):
            return normalize_line(line, out_dir)[:300]
    return "?"


def normalize_run_output(output, out_dir):
    """Stack traces of uncaught exceptions contain the executable name and ASLR-dependent addresses."""
    result = []
    for line in output.splitlines():
        line = normalize_line(line, out_dir)
        # The trace pads the executable name to a fixed column, so `clean.kexe` and `clean2.kexe` leave different gaps.
        line = re.sub(r"\b[\w-]+\.kexe\b\s*", "<EXE> ", line)
        line = re.sub(r"0x[0-9a-f]+", "0xADDR", line)
        line = re.sub(r" \+ \d+\s*$", " + N", line)
        result.append(line.rstrip())
    return "\n".join(result)


# ---------------------------------------------------------------------------------------------------------------------
# IC introspection: the -verbose "IC analysis results" block and the -Xdump-built-caches-to list

IC_SECTIONS = ["CACHED", "CLEAN BUILD", "FULL REBUILD", "ADDED FILES", "REMOVED FILES", "CHANGED FILES", "DIRTY FILES"]


def parse_ic_analysis(output):
    sections = {}
    current = None
    batches = []
    stale = []
    for raw in output.splitlines():
        if not raw.startswith("logging: "):
            continue
        text = raw[len("logging: "):]
        stripped = text.strip()
        if stripped.rstrip(":") in IC_SECTIONS and text.startswith("    ") and not text.startswith("        "):
            current = stripped.rstrip(":")
            sections[current] = []
        elif text.startswith("        ") and current is not None:
            sections[current].append(stripped)
        else:
            current = None
            if stripped.startswith("CACHING BATCH"):
                batches.append(stripped)
            m = re.match(r"Incremental cache for .* (has no metadata|was produced .*|does not match .*); rebuilding it", stripped)
            if m:
                stale.append(m.group(1))
    return {"sections": sections, "batches": batches, "stale_reasons": stale}


def user_built_caches(dump_file, dist):
    if not dump_file.exists():
        return []
    return sorted(line.strip() for line in dump_file.read_text().splitlines()
                  if line.strip() and not line.strip().startswith(str(dist)))


def rebuilt_cache_entries(analysis, dump, tools):
    """{(cache dir name, file id or None)} rebuilt by an IC link: from -Xdump-built-caches-to, else from the IC analysis."""
    entries = set()
    if tools.can_dump_built_caches:
        for path in user_built_caches(dump, tools.dist):
            archive = Path(path)
            if archive.parents[2].name.endswith("-per-file-cache"):
                entries.add((archive.parents[2].name, archive.parents[1].name))
            else:
                entries.add((archive.parents[1].name, None))
        return entries
    sections = analysis["sections"]
    for item in sections.get("DIRTY FILES", []):
        library, _, file_id = item.partition("|")
        entries.add((library + "-per-file-cache", file_id))
    for path in sections.get("FULL REBUILD", []) + sections.get("CLEAN BUILD", []):
        entries.add((Path(path).stem + "-per-file-cache", "*"))
    return entries


def coverage_features(ic):
    sections = ic["sections"]
    features = set()
    changed = set(sections.get("CHANGED FILES", []))
    added = set(sections.get("ADDED FILES", []))
    removed = set(sections.get("REMOVED FILES", []))
    dirty = set(sections.get("DIRTY FILES", []))
    if changed:
        features.add("changed-file")
    if added:
        features.add("added-file")
    if removed:
        features.add("removed-file")
    propagated = dirty - changed - added
    if propagated:
        features.add("dirty-propagated")
        libs = set(it.split("|")[0] for it in propagated)
        sources = set(it.split("|")[0] for it in changed | removed)
        if libs - sources:
            features.add("dirty-propagated-cross-library")
    if changed and not dirty - added:
        features.add("changed-without-dirty")
    if sections.get("FULL REBUILD") and sections.get("CACHED"):
        features.add("full-rebuild-of-cached")
    if ic["batches"]:
        features.add("batched-build")
    for reason in ic["stale_reasons"]:
        features.add("stale:" + reason.split(" ")[0] + "-" + reason.split(" ")[-1])
    return features


# ---------------------------------------------------------------------------------------------------------------------
# Cache state comparison

def cache_entries(flavor_root):
    """{(cache dir name, file id or None): directory}. Per-file caches are keyed by file, monolithic ones by library."""
    entries = {}
    if not flavor_root.exists():
        return entries
    for cache in sorted(flavor_root.iterdir()):
        if cache.name.endswith("-per-file-cache"):
            for file_dir in sorted(cache.iterdir()):
                entries[(cache.name, file_dir.name)] = file_dir
        elif cache.name.endswith("-cache"):
            entries[(cache.name, None)] = cache
    return entries


def compared_files(entry_dir):
    files = []
    for path in sorted(entry_dir.rglob("*")):
        if not path.is_file():
            continue
        rel = path.relative_to(entry_dir)
        # Archives embed timestamps; their content is checked through the program output instead.
        if path.suffix in (".a", ".o", ".dylib", ".bc", ".lock"):
            continue
        files.append(rel)
    return files


def flavor_dirs(ic_dir):
    return [d for d in ic_dir.iterdir() if d.is_dir()] if ic_dir.exists() else []


def compare_cache_state(ic_dir, clean_dir, rebuilt):
    """Returns a list of (kind, detail). Under-invalidation leaves IC cache artifacts different from a clean build's.
    [rebuilt] holds the (cache dir name, file id) entries that the IC link has just rebuilt."""
    problems = []
    ic_flavors = {d.name: d for d in flavor_dirs(ic_dir)}
    for clean_flavor in flavor_dirs(clean_dir):
        ic_flavor = ic_flavors.get(clean_flavor.name)
        if ic_flavor is None:
            problems.append(("CACHE_STATE_MISMATCH", "flavor %s missing in IC dir" % clean_flavor.name))
            continue
        clean_entries = cache_entries(clean_flavor)
        ic_entries = cache_entries(ic_flavor)
        clean_libraries = set(key[0] for key in clean_entries)
        for key in sorted(set(ic_entries) - set(clean_entries), key=str):
            kind = "STALE_CACHE_DIR" if key[0] in clean_libraries else "STALE_LIBRARY_CACHE"
            problems.append((kind, "%s/%s exists only in the IC dir" % key))
        for key in sorted(set(clean_entries) - set(ic_entries), key=str):
            problems.append(("CACHE_STATE_MISMATCH", "%s/%s missing in the IC dir" % key))
        for key in sorted(set(clean_entries) & set(ic_entries), key=str):
            clean_files = compared_files(clean_entries[key])
            ic_files = compared_files(ic_entries[key])
            if clean_files != ic_files:
                problems.append(("CACHE_STATE_MISMATCH", "%s/%s: file lists differ" % key))
                continue
            for rel in clean_files:
                a = (clean_entries[key] / rel).read_bytes()
                b = (ic_entries[key] / rel).read_bytes()
                if a != b:
                    a = a.replace(str(clean_dir).encode(), b"<IC_DIR>")
                    b = b.replace(str(ic_dir).encode(), b"<IC_DIR>")
                    if a != b:
                        drift = rel.name == "bitcode_deps" and key not in rebuilt and (key[0], "*") not in rebuilt
                        problems.append(("DEPS_DRIFT" if drift else "CACHE_STATE_MISMATCH",
                                         "%s/%s: %s differs" % (key[0], key[1], rel)))
    return problems


# ---------------------------------------------------------------------------------------------------------------------
# The differential runner

class Runner(object):
    def __init__(self, scenario, out_dir, tools, variants, check_cache_state=True, noop_check=True,
                 stop_at_first_bug=False, keep=False, self_check=False, noic_check=True):
        self.scenario = scenario
        self.out = Path(out_dir).resolve()
        self.tools = tools
        self.variants = variants
        self.check_cache_state = check_cache_state
        self.noop_check = noop_check
        self.noic_check = noic_check
        self.stop_at_first_bug = stop_at_first_bug
        self.keep = keep
        self.self_check = self_check
        self.findings = []
        self.invalid = None
        self.step_reports = []
        self.features = set()
        self.klibs = {}

    # -- paths
    def src(self, module):
        return self.out / "src" / module

    def klib_path(self, module):
        directory = self.out / ("ext" if module.startswith(EXTERNAL_PREFIX) else "klibs")
        return directory / (module + ".klib")

    def step_dir(self, step_id):
        d = self.out / "steps" / str(step_id)
        d.mkdir(parents=True, exist_ok=True)
        return d

    # -- helpers
    def add_finding(self, kind, step, variant, detail, signature=None):
        self.findings.append({
            "kind": kind,
            "severity": SEVERITY[kind],
            "step": step,
            "variant": variant,
            "detail": detail[:2000],
            "signature": signature or kind,
        })

    def module_step(self, module, step_id):
        return self.scenario.module_steps[module].get(step_id)

    def dependency_closure(self, module, step_id, seen=None):
        """Direct and transitive dependencies in dependency-first order."""
        seen = [] if seen is None else seen
        step = self.module_step(module, step_id) or self._last_known_step(module, step_id)
        for dep in (step.dependencies if step else []):
            if dep not in seen:
                self.dependency_closure(dep, step_id, seen)
                seen.append(dep)
        return seen

    def _last_known_step(self, module, step_id):
        known = [s for s in self.scenario.module_steps[module] if s <= step_id]
        return self.scenario.module_steps[module][max(known)] if known else None

    def apply_modifications(self, step_id):
        for module in self.scenario.modules:
            step = self.module_step(module, step_id)
            if step is None:
                continue
            target_dir = self.src(module)
            target_dir.mkdir(parents=True, exist_ok=True)
            for kind, a, b in step.modifications:
                if kind == "U":
                    source = self.scenario.root / module / a
                    if not source.exists():
                        raise ValueError("step %d, module %s: no file %s" % (step_id, module, a))
                    target = target_dir / b
                    target.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copyfile(source, target)
                else:
                    target = target_dir / a
                    if target.exists():
                        target.unlink()

    # -- compilation
    def compile_module(self, module, step_id, log_dir):
        """Returns None on success or (kind, detail) for a failure."""
        step = self.module_step(module, step_id) or self._last_known_step(module, step_id)
        src = self.src(module)
        output = self.klib_path(module)
        output.parent.mkdir(parents=True, exist_ok=True)
        if output.exists():
            output.unlink() if output.is_file() else shutil.rmtree(output)
        defs = sorted(src.glob("*.def"))
        if defs:
            # As CInteropCompilation in the test fixture: no platform libraries, a private clang modules cache.
            cmd = [str(self.tools.cinterop), "-def", str(defs[0]), "-o", str(output), "-no-default-libs",
                   "-compiler-option", "-I" + str(src),
                   "-compiler-option", "-fmodules-cache-path=%s" % (self.out / "modulesCache")]
        else:
            sources = sorted(str(p) for p in src.rglob("*.kt"))
            if not sources:
                return ("invalid", "module %s has no sources at step %d" % (module, step_id))
            cmd = [str(self.tools.kotlinc), "-p", "library", "-o", str(output), "-module-name", module] + LIBRARY_FLAGS
            for dep in self.dependency_closure(module, step_id):
                cmd += ["-l", str(self.klib_path(dep))]
            if step and step.friends:
                cmd += ["-friend-modules", os.pathsep.join(str(self.klib_path(f)) for f in step.friends)]
            cmd += (step.arguments if step else []) + sources
        code, out, elapsed = run_process(cmd, log_dir / ("klib-%s.log" % module), COMPILE_TIMEOUT)
        if code != 0 or not output.exists():
            if is_crash(out):
                return ("crash", out)
            return ("invalid", "klib compilation of %s failed:\n%s" % (module, "\n".join(diagnostics(out, self.out)[:20]) or out[-1500:]))
        return None

    def uses_test_runner(self):
        return any("@Test" in p.read_text() for p in self.src(MAIN_MODULE).rglob("*.kt"))

    def link(self, step_id, ic_dir, auto_dir, executable, log_path, dump_path, extra_flags):
        ic_dir.mkdir(parents=True, exist_ok=True)
        auto_dir.mkdir(parents=True, exist_ok=True)
        (self.out / "ext").mkdir(exist_ok=True)
        main_step = self.module_step(MAIN_MODULE, step_id) or self._last_known_step(MAIN_MODULE, step_id)
        cmd = [str(self.tools.kotlinc), "-p", "program", "-o", str(executable), "-Xinclude=%s" % self.klib_path(MAIN_MODULE)]
        for dep in self.dependency_closure(MAIN_MODULE, step_id):
            cmd += ["-l", str(self.klib_path(dep))]
        if main_step and main_step.friends:
            cmd += ["-friend-modules", os.pathsep.join(str(self.klib_path(f)) for f in main_step.friends)]
        if self.uses_test_runner():
            cmd.append("-generate-test-runner")
        cmd += LINK_FLAGS + [
            "-Xauto-cache-from=%s" % (self.out / "ext"),
            "-Xauto-cache-dir=%s" % auto_dir,
            "-Xenable-incremental-compilation",
            "-Xic-cache-dir=%s" % ic_dir,
            "-verbose",
        ] + (main_step.arguments if main_step else []) + extra_flags
        if self.tools.can_dump_built_caches:
            cmd.append("-Xdump-built-caches-to=%s" % dump_path)
        if dump_path.exists():
            dump_path.unlink()
        code, out, elapsed = run_process(cmd, log_path, COMPILE_TIMEOUT)
        ok = code == 0 and Path(str(executable)).exists()
        return {"ok": ok, "code": code, "output": out, "secs": round(elapsed, 1)}

    def link_noic(self, step_id, executable, log_path):
        """A truly non-IC link: no incremental compilation, no caches, no auto-cache. Proves that
        the klibs build and run without the caching machinery, which the fresh-cache clean link
        cannot (it runs the IC pipeline too, just from scratch)."""
        main_step = self.module_step(MAIN_MODULE, step_id) or self._last_known_step(MAIN_MODULE, step_id)
        cmd = [str(self.tools.kotlinc), "-p", "program", "-o", str(executable), "-Xinclude=%s" % self.klib_path(MAIN_MODULE)]
        for dep in self.dependency_closure(MAIN_MODULE, step_id):
            cmd += ["-l", str(self.klib_path(dep))]
        if main_step and main_step.friends:
            cmd += ["-friend-modules", os.pathsep.join(str(self.klib_path(f)) for f in main_step.friends)]
        if self.uses_test_runner():
            cmd.append("-generate-test-runner")
        cmd += LINK_FLAGS + (main_step.arguments if main_step else [])
        code, out, elapsed = run_process(cmd, log_path, COMPILE_TIMEOUT)
        ok = code == 0 and Path(str(executable)).exists()
        return {"ok": ok, "code": code, "output": out, "secs": round(elapsed, 1)}

    def run_binary(self, executable, log_path):
        code, out, elapsed = run_process([str(executable)], log_path, RUN_TIMEOUT)
        return {"code": code, "stdout": normalize_run_output(out, self.out)}

    # -- main loop
    def run(self):
        if self.out.exists():
            shutil.rmtree(self.out)
        self.out.mkdir(parents=True)
        started = time.time()
        for step_id, order in self.scenario.steps:
            report = {"step": step_id}
            self.step_reports.append(report)
            if not self.run_step(step_id, order, report):
                break
            if self.stop_at_first_bug and any(f["severity"] == "bug" for f in self.findings):
                break
        else:
            if self.noop_check and self.invalid is None:
                self.run_noop_checks()
        return self.finish(time.time() - started)

    def run_step(self, step_id, order, report):
        log_dir = self.step_dir(step_id)
        try:
            self.apply_modifications(step_id)
        except ValueError as e:
            self.invalid = str(e)
            return False

        to_compile = [m for m in order if m != MAIN_MODULE] + [MAIN_MODULE]
        for module in to_compile:
            failure = self.compile_module(module, step_id, log_dir)
            if failure is None:
                continue
            kind, detail = failure
            if kind == "crash":
                self.add_finding("COMPILER_CRASH", step_id, None, "klib compilation of %s crashed: %s" % (module, crash_signature(detail)),
                                 "COMPILER_CRASH klib " + crash_signature(detail))
            else:
                self.invalid = "step %d: %s" % (step_id, detail)
            return False

        expects_failure = any("expected compilation failure" in (self.module_step(m, step_id) or ModuleStep()).directives
                              for m in self.scenario.modules)

        # The clean reference: a fresh IC and auto-cache directory for every step.
        clean_root = self.out / "clean" / str(step_id)
        clean = self.link(step_id, clean_root / "ic", clean_root / "auto", log_dir / "clean.kexe", log_dir / "clean.log",
                          log_dir / "clean-built.txt", [])
        report["clean"] = {"ok": clean["ok"], "secs": clean["secs"]}
        clean_run = None
        if clean["ok"]:
            clean_run = self.run_binary(log_dir / "clean.kexe", log_dir / "clean.stdout")
            again = self.run_binary(log_dir / "clean.kexe", log_dir / "clean.again.stdout")
            if again != clean_run:
                self.invalid = "step %d: the program output is not deterministic" % step_id
                return False
            report["clean"]["exit"] = clean_run["code"]

        # A truly non-IC build of the same klibs: the reference for defects that are present in a
        # from-scratch IC build as well. Steps that rely on partial linkage stubbing (a stale klib
        # calling a symbol that is gone) legitimately fail this link, so its failure is only an
        # info note; its success can exonerate the program where the IC links fail.
        noic = None
        noic_run = None
        if self.noic_check:
            noic = self.link_noic(step_id, log_dir / "noic.kexe", log_dir / "noic.log")
            report["noic"] = {"ok": noic["ok"], "secs": noic["secs"]}
            if noic["ok"]:
                noic_run = self.run_binary(log_dir / "noic.kexe", log_dir / "noic.stdout")
                report["noic"]["exit"] = noic_run["code"]
                if clean["ok"] and clean_run != noic_run:
                    self.add_finding("IC_ONLY_OUTPUT_MISMATCH", step_id, None,
                                     "a from-scratch IC build differs from a non-IC build:\n" +
                                     unified_diff(noic_run, clean_run, "noic", "clean IC"))
            else:
                self.add_finding("NOIC_LINK_FAILURE", step_id, None,
                                 "the non-IC link fails (expected on steps that rely on partial linkage): " +
                                 first_error(noic["output"], self.out))

        if self.self_check:
            self.run_self_check(step_id, clean, clean_run, clean_root, log_dir)

        continue_steps = True
        for variant, flags in self.variants:
            ic_dir = self.out / "ic" / variant
            dump = log_dir / ("ic-%s-built.txt" % variant)
            ic = self.link(step_id, ic_dir, self.out / "auto" / variant, log_dir / ("ic-%s.kexe" % variant),
                           log_dir / ("ic-%s.log" % variant), dump, flags)
            analysis = parse_ic_analysis(ic["output"])
            self.features |= coverage_features(analysis)
            rebuilt = rebuilt_cache_entries(analysis, dump, self.tools)
            vreport = {
                "ok": ic["ok"],
                "secs": ic["secs"],
                "dirty": analysis["sections"].get("DIRTY FILES", []),
                "changed": analysis["sections"].get("CHANGED FILES", []),
                "rebuilt": sorted("%s/%s" % key for key in rebuilt if key[1] is not None),
            }
            report[variant] = vreport

            if not clean["ok"] and not ic["ok"]:
                if not expects_failure and noic is not None and noic["ok"]:
                    # The program is fine without the caching machinery: a from-scratch IC defect,
                    # invisible to the fresh-cache clean link, which runs the IC pipeline too.
                    # Steps that declare an expected compilation failure are exempt: some IC
                    # features deliberately fail there where a non-IC build silently stubs the
                    # reference (partial linkage reports issues only when caches are built).
                    if is_crash(clean["output"]):
                        sig = crash_signature(clean["output"])
                        self.add_finding("IC_ONLY_LINK_FAILURE", step_id, variant,
                                         "the IC link crashes, a non-IC build does not: " + sig,
                                         "IC_ONLY_LINK_FAILURE " + sig)
                    else:
                        err = first_error(clean["output"], self.out)
                        self.add_finding("IC_ONLY_LINK_FAILURE", step_id, variant,
                                         "the IC link fails, a non-IC build of the same klibs does not: " + err,
                                         "IC_ONLY_LINK_FAILURE " + re.sub(r"\d+", "N", err)[:120])
                    continue
                if is_crash(clean["output"]):
                    sig = crash_signature(clean["output"])
                    self.add_finding("COMPILER_CRASH", step_id, variant, "the clean link crashes too: " + sig, "COMPILER_CRASH link " + sig)
                    continue_steps = False
                elif not expects_failure:
                    self.invalid = "step %d: the clean link fails: %s" % (step_id, first_error(clean["output"], self.out))
                    continue_steps = False
                continue
            if clean["ok"] and not ic["ok"]:
                if is_crash(ic["output"]):
                    sig = crash_signature(ic["output"])
                    self.add_finding("IC_CRASH", step_id, variant, "the IC link crashes, the clean one does not: " + sig, "IC_CRASH " + sig)
                else:
                    err = first_error(ic["output"], self.out)
                    self.add_finding("IC_LINK_FAILURE", step_id, variant, "the IC link fails, the clean one does not: " + err,
                                     "IC_LINK_FAILURE " + re.sub(r"\d+", "N", err)[:120])
                continue
            if not clean["ok"] and ic["ok"]:
                self.add_finding("IC_ACCEPTS_BROKEN", step_id, variant,
                                 "the IC link succeeds, the clean one fails: " + first_error(clean["output"], self.out))
                continue

            ic_run = self.run_binary(log_dir / ("ic-%s.kexe" % variant), log_dir / ("ic-%s.stdout" % variant))
            if ic_run != clean_run:
                self.add_finding("OUTPUT_MISMATCH", step_id, variant, unified_diff(clean_run, ic_run))
            clean_diag, ic_diag = diagnostics(clean["output"], self.out), diagnostics(ic["output"], self.out)
            if clean_diag != ic_diag:
                self.add_finding("DIAGNOSTICS_MISMATCH", step_id, variant,
                                 "clean:\n  %s\nIC:\n  %s" % ("\n  ".join(clean_diag) or "-", "\n  ".join(ic_diag) or "-"))
            if self.check_cache_state:
                for kind, detail in compare_cache_state(ic_dir, clean_root / "ic", rebuilt):
                    self.add_finding(kind, step_id, variant, detail, kind + " " + re.sub(r"\.[-\w]+/", "./", detail.split(":")[-1]).strip())

        if not self.keep:
            shutil.rmtree(clean_root, ignore_errors=True)
        return continue_steps

    def run_self_check(self, step_id, clean, clean_run, clean_root, log_dir):
        """Links cleanly a second time: any difference between two clean builds is harness noise, not an IC bug."""
        root2 = self.out / "clean2" / str(step_id)
        clean2 = self.link(step_id, root2 / "ic", root2 / "auto", log_dir / "clean2.kexe", log_dir / "clean2.log",
                           log_dir / "clean2-built.txt", [])
        noise = []
        if clean2["ok"] != clean["ok"]:
            noise.append("link success differs")
        elif clean["ok"]:
            if self.run_binary(log_dir / "clean2.kexe", log_dir / "clean2.stdout") != clean_run:
                noise.append("program output differs")
            if diagnostics(clean["output"], self.out) != diagnostics(clean2["output"], self.out):
                noise.append("diagnostics differ")
            noise += [d for k, d in compare_cache_state(root2 / "ic", clean_root / "ic", set()) if k != "DEPS_DRIFT"]
        if noise:
            self.findings.append({"kind": "HARNESS_NOISE", "severity": "invalid", "step": step_id, "variant": None,
                                  "detail": "; ".join(noise)[:2000], "signature": "HARNESS_NOISE"})
        shutil.rmtree(root2, ignore_errors=True)

    def run_noop_checks(self):
        """After the last step: relinking with no edits, then with freshly recompiled klibs, must rebuild no cache."""
        last = self.scenario.steps[-1][0]
        log_dir = self.step_dir("noop")
        for variant, flags in self.variants:
            if not self.step_reports[-1].get(variant, {}).get("ok"):
                continue  # The last IC link failed already, that finding is enough.
            ic_dir = self.out / "ic" / variant
            for phase in ("relink", "recompile"):
                if phase == "recompile":
                    for module in [m for m in self.scenario.modules if m != MAIN_MODULE] + [MAIN_MODULE]:
                        if self.compile_module(module, last, log_dir) is not None:
                            return
                dump = log_dir / ("%s-%s-built.txt" % (phase, variant))
                ic = self.link(last, ic_dir, self.out / "auto" / variant, log_dir / ("%s-%s.kexe" % (phase, variant)),
                               log_dir / ("%s-%s.log" % (phase, variant)), dump, flags)
                if not ic["ok"]:
                    self.add_finding("IC_LINK_FAILURE", "noop-" + phase, variant, "a no-op IC relink fails: " + first_error(ic["output"], self.out))
                    return
                analysis = parse_ic_analysis(ic["output"])
                dirty = analysis["sections"].get("DIRTY FILES", [])
                rebuilt = user_built_caches(dump, self.tools.dist)
                if dirty or rebuilt:
                    kind = "NOOP_REBUILD" if phase == "relink" else "UNSTABLE_FINGERPRINT"
                    self.add_finding(kind, "noop-" + phase, variant, "dirty: %s; rebuilt: %d archives" % (dirty[:10], len(rebuilt)))

    def finish(self, elapsed):
        severities = [f["severity"] for f in self.findings]
        if self.invalid is not None:
            severities.append("invalid")
        verdict = next((s for s in SEVERITY_ORDER if s in severities), "pass").upper()
        result = {
            "scenario": str(self.scenario.root),
            "verdict": verdict,
            "invalid_reason": self.invalid,
            "findings": self.findings,
            "coverage": sorted(self.features),
            "steps": self.step_reports,
            "variants": [v for v, _ in self.variants],
            "seconds": round(elapsed, 1),
            "out": str(self.out),
        }
        (self.out / "result.json").write_text(json.dumps(result, indent=2))
        summary = format_summary(result)
        (self.out / "summary.txt").write_text(summary)
        return result


def unified_diff(clean_run, ic_run, a_name="clean", b_name="ic"):
    lines = []
    if clean_run["code"] != ic_run["code"]:
        lines.append("exit code: %s %s, %s %s" % (a_name, clean_run["code"], b_name, ic_run["code"]))
    diff = difflib.unified_diff(clean_run["stdout"].splitlines(), ic_run["stdout"].splitlines(), a_name, b_name, lineterm="", n=1)
    lines += list(diff)[:40]
    return "\n".join(lines)


def format_summary(result):
    lines = ["verdict: %s  (%ss, variants: %s)" % (result["verdict"], result["seconds"], ", ".join(result["variants"]))]
    if result["invalid_reason"]:
        lines.append("invalid: " + result["invalid_reason"][:1500])
    for f in result["findings"]:
        detail = f["detail"].replace("\n", "\n      ")
        lines.append("- [%s] %s at step %s (%s)\n      %s" % (f["severity"], f["kind"], f["step"], f["variant"], detail[:1200]))
    lines.append("coverage: " + (", ".join(result["coverage"]) or "-"))
    for step in result["steps"]:
        parts = ["step %s:" % step["step"]]
        if "clean" in step:
            parts.append("clean %s%s" % ("ok" if step["clean"]["ok"] else "FAIL",
                                         "" if step["clean"].get("exit", 0) == 0 else " (exit %s)" % step["clean"].get("exit")))
        if "noic" in step:
            parts.append("noic %s" % ("ok" if step["noic"]["ok"] else "FAIL"))
        for v in result["variants"]:
            if v in step:
                parts.append("%s %s dirty=%d" % (v, "ok" if step[v]["ok"] else "FAIL", len(step[v]["dirty"])))
        lines.append("  " + " ".join(parts))
    lines.append("details: %s/result.json, logs in %s/steps/" % (result["out"], result["out"]))
    return "\n".join(lines) + "\n"


# ---------------------------------------------------------------------------------------------------------------------
# Reduction: deterministic shrinking of steps, edits and files while the target finding still reproduces

def finding_key(finding):
    return (finding["kind"], finding["variant"])


def reduce_scenario(scenario_dir, out_dir, tools, variants, max_runs, check_cache_state, with_roundtrip, kind=None,
                    noic_check=True):
    out_dir = Path(out_dir).resolve()
    work = out_dir / "work"
    original = parse_scenario(scenario_dir)
    if with_roundtrip:
        original = roundtrip(original)
    base = Runner(original, work / "baseline", tools, variants, check_cache_state=check_cache_state,
                  noic_check=noic_check).run()
    targets = [f for f in base["findings"] if f["severity"] in ("bug", "latent") and kind in (None, f["kind"])]
    if not targets:
        print("nothing to reduce: verdict %s" % base["verdict"])
        return 1
    # A latent finding often comes first (e.g. at step 0), and keeping it would reduce the real bug away.
    target = min(targets, key=lambda f: SEVERITY_ORDER.index(f["severity"]))
    print("target: %s at step %s (%s)" % (target["kind"], target["step"], target["variant"]))
    runs = [0]

    def reproduces(candidate, label):
        if runs[0] >= max_runs:
            return False
        runs[0] += 1
        candidate_dir = work / ("cand-%d-src" % runs[0])
        write_scenario(candidate, candidate_dir)
        try:
            parsed = parse_scenario(candidate_dir)
        except ValueError:
            return False
        result = Runner(parsed, work / ("cand-%d" % runs[0]), tools,
                        [v for v in variants if v[0] == target["variant"]] or variants,
                        check_cache_state=check_cache_state, noop_check=isinstance(target["step"], str),
                        stop_at_first_bug=True, noic_check=noic_check).run()
        ok = any(finding_key(f) == finding_key(target) for f in result["findings"])
        print("  [%d] %-40s -> %s" % (runs[0], label, "reproduces" if ok else result["verdict"].lower()))
        return ok

    current = original
    fail_step = target["step"] if isinstance(target["step"], int) else None

    # 1. Drop the steps after the failing one.
    if fail_step is not None and fail_step < len(current.steps) - 1:
        candidate = truncate_steps(current, fail_step)
        if reproduces(candidate, "truncate after step %d" % fail_step):
            current = candidate

    # 2. Merge intermediate steps into the next one (the edits stay, the intermediate build goes).
    step_index = len(current.steps) - 2
    while step_index >= 1:
        candidate = merge_step(current, step_index)
        if reproduces(candidate, "merge step %d into %d" % (step_index, step_index + 1)):
            current = candidate
        step_index -= 1

    # 3. Drop single edits of later steps.
    for step_id, _ in list(current.steps)[1:]:
        for module in current.modules:
            step = current.module_steps[module].get(step_id)
            for mod in list(step.modifications if step else []):
                candidate = drop_modification(current, module, step_id, mod)
                if candidate and reproduces(candidate, "drop %s %s at step %d" % (module, mod[1], step_id)):
                    current = candidate

    # 4. Drop whole source files.
    for module in current.modules:
        for name in sorted(set(m[2] for s in current.module_steps[module].values() for m in s.modifications if m[0] == "U")):
            candidate = drop_file(current, module, name)
            if candidate and reproduces(candidate, "drop file %s/%s" % (module, name)):
                current = candidate

    write_scenario(current, out_dir / "reduced")
    print("reduced scenario: %s (%d runs%s)" % (out_dir / "reduced", runs[0], ", run budget exhausted" if runs[0] >= max_runs else ""))
    return 0


def clone(scenario):
    return copy.deepcopy(scenario)


def source_states(scenario):
    """For every step, {module: {target file: scenario file it was copied from}} after the step's edits."""
    current = dict((module, {}) for module in scenario.modules)
    states = []
    for step_id, _ in scenario.steps:
        for module in scenario.modules:
            step = scenario.module_steps[module].get(step_id)
            for kind, a, b in (step.modifications if step else []):
                if kind == "U":
                    current[module][b] = a
                else:
                    current[module].pop(a, None)
        states.append(copy.deepcopy(current))
    return states


def roundtrip(scenario):
    """Appends steps that go back through the earlier source states in reverse: A, B, C becomes A, B, C, B, A.
    Rolling back to already cached states is a known weak spot (KT-87194)."""
    result = clone(scenario)
    states = source_states(scenario)
    last = len(scenario.steps) - 1
    previous = states[last]
    for offset, back_to in enumerate(range(last - 1, -1, -1)):
        new_id = last + 1 + offset
        target = states[back_to]
        for module in scenario.modules:
            template = scenario.module_steps[module].get(back_to)
            if template is None:
                continue
            step = copy.deepcopy(template)
            step.modifications = [("U", source, path) for path, source in sorted(target[module].items())
                                  if previous[module].get(path) != source]
            step.modifications += [("D", path, None) for path in sorted(previous[module]) if path not in target[module]]
            # Cache expectations of the original step don't hold on the way back; output expectations do.
            step.directives = dict((k, v) for k, v in step.directives.items() if k.startswith(("expected", "forbidden")))
            result.module_steps[module][new_id] = step
        result.steps.append((new_id, list(scenario.steps[back_to][1])))
        previous = target
    return result


def renumber(scenario, keep_ids):
    """Keeps the given step ids (sorted) and renumbers them 0..n-1."""
    result = clone(scenario)
    mapping = {old: new for new, old in enumerate(keep_ids)}
    result.steps = [(mapping[i], order) for i, order in scenario.steps if i in mapping]
    for module in result.modules:
        result.module_steps[module] = {mapping[i]: s for i, s in result.module_steps[module].items() if i in mapping}
    return result


def truncate_steps(scenario, last):
    return renumber(scenario, [i for i, _ in scenario.steps if i <= last])


def merge_step(scenario, index):
    result = clone(scenario)
    for module in result.modules:
        steps = result.module_steps[module]
        if index in steps:
            nxt = steps.get(index + 1)
            if nxt is None:
                steps[index + 1] = steps[index]
            else:
                # A STEP range shares one object between its steps; merge into a copy.
                merged = copy.copy(nxt)
                merged.modifications = steps[index].modifications + nxt.modifications
                steps[index + 1] = merged
    return renumber(result, [i for i, _ in result.steps if i != index])


def drop_modification(scenario, module, step_id, mod):
    result = clone(scenario)
    step = result.module_steps[module][step_id]
    if mod not in step.modifications:
        return None
    step.modifications.remove(mod)
    return result


def drop_file(scenario, module, name):
    result = clone(scenario)
    changed = False
    for step in result.module_steps[module].values():
        kept = [m for m in step.modifications if not ((m[0] == "U" and m[2] == name) or (m[0] == "D" and m[1] == name))]
        changed |= len(kept) != len(step.modifications)
        step.modifications = kept
    return result if changed else None


# ---------------------------------------------------------------------------------------------------------------------
# CLI

def parse_variants(values):
    variants = []
    for value in values or ["default"]:
        if ":" in value:
            name, flags = value.split(":", 1)
            flags = flags.split()
        elif value in VARIANTS:
            name, flags = value, VARIANTS[value]
        else:
            raise SystemExit("unknown variant %s; known: %s, or NAME:FLAGS" % (value, ", ".join(VARIANTS)))
        # Each variant owns its IC directory and logs, so a repeated name would only add a no-op relink
        # that overwrites the logs of the real one.
        if all(name != known for known, _ in variants):
            variants.append((name, flags))
    return variants


def warmup(tools, out_dir):
    out_dir = Path(out_dir)
    scenario_dir = out_dir / "warmup-scenario"
    if scenario_dir.exists():
        shutil.rmtree(scenario_dir)
    (scenario_dir / "main").mkdir(parents=True)
    (scenario_dir / "project.info").write_text("MODULES: main\n\nSTEP 0:\n    libs: main\n")
    (scenario_dir / "main" / "module.info").write_text("STEP 0:\n    modifications:\n        U : main.kt -> main.kt\n")
    (scenario_dir / "main" / "main.kt").write_text('fun main() { println("warm") }\n')
    started = time.time()
    result = Runner(parse_scenario(scenario_dir), out_dir / "warmup-run", tools, [("default", [])], noop_check=False).run()
    print("warmup %s in %.0fs" % (result["verdict"], time.time() - started))
    return 0 if result["verdict"] == "PASS" else 1


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--dist", default=str(DEFAULT_DIST), help="Kotlin/Native distribution (default: %(default)s)")
    sub = parser.add_subparsers(dest="command")

    p_warm = sub.add_parser("warmup")
    p_warm.add_argument("--out", default=str(REPO / "build" / "kn-ic-fuzz" / "warmup"))

    def add_run_options(p):
        p.add_argument("--variant", action="append", help="IC variant: %s, or NAME:FLAGS (repeatable)" % ", ".join(VARIANTS))
        p.add_argument("--no-cache-state", action="store_true", help="do not compare IC cache artifacts with a clean build")
        p.add_argument("--no-noic", action="store_true",
                       help="skip the non-IC link (the reference for from-scratch IC-only defects)")
        p.add_argument("--roundtrip", action="store_true", help="append steps going back through the earlier states")

    p_run = sub.add_parser("run")
    p_run.add_argument("scenario")
    p_run.add_argument("--out", required=True)
    p_run.add_argument("--no-noop", action="store_true", help="skip the no-op relink checks after the last step")
    p_run.add_argument("--keep", action="store_true", help="keep the clean-build cache directories")
    p_run.add_argument("--self-check", action="store_true", help="link cleanly twice per step to detect harness noise")
    p_run.add_argument("--stop-at-first-bug", action="store_true")
    add_run_options(p_run)

    p_red = sub.add_parser("reduce")
    p_red.add_argument("scenario")
    p_red.add_argument("--out", required=True)
    p_red.add_argument("--max-runs", type=int, default=15)
    p_red.add_argument("--kind", help="the finding kind to keep, e.g. OUTPUT_MISMATCH (default: the first bug, else the first latent)")
    add_run_options(p_red)

    p_cor = sub.add_parser("corpus")
    p_cor.add_argument("--filter", default="", help="substring of the scenario path, e.g. 'members/'")
    p_cor.add_argument("--out", default=str(REPO / "build" / "kn-ic-fuzz" / "corpus"))
    p_cor.add_argument("--self-check", action="store_true")
    add_run_options(p_cor)

    args = parser.parse_args()
    if args.command is None:
        parser.print_help()
        return 2
    tools = Tools(args.dist)

    if args.command == "warmup":
        return warmup(tools, args.out)

    variants = parse_variants(args.variant)
    if args.command == "run":
        try:
            scenario = parse_scenario(args.scenario)
        except ValueError as e:
            print("verdict: INVALID\ninvalid: malformed scenario: %s" % e)
            return 3
        if args.roundtrip:
            scenario = roundtrip(scenario)
        result = Runner(scenario, args.out, tools, variants, check_cache_state=not args.no_cache_state,
                        noop_check=not args.no_noop, stop_at_first_bug=args.stop_at_first_bug, keep=args.keep,
                        self_check=args.self_check, noic_check=not args.no_noic).run()
        sys.stdout.write(format_summary(result))
        return 0
    if args.command == "reduce":
        return reduce_scenario(args.scenario, args.out, tools, variants, args.max_runs, not args.no_cache_state, args.roundtrip,
                               args.kind, not args.no_noic)
    if args.command == "corpus":
        verdicts = {}
        for project_info in sorted(CORPUS_DIR.rglob("project.info")):
            scenario_dir = project_info.parent
            rel = str(scenario_dir.relative_to(CORPUS_DIR))
            if args.filter not in rel:
                continue
            scenario = parse_scenario(scenario_dir)
            if args.roundtrip:
                scenario = roundtrip(scenario)
            result = Runner(scenario, Path(args.out) / rel, tools, variants,
                            check_cache_state=not args.no_cache_state, self_check=args.self_check,
                            noic_check=not args.no_noic).run()
            verdicts[rel] = result["verdict"]
            kinds = sorted(set(f["kind"] for f in result["findings"]))
            print("%-60s %-8s %s %s" % (rel, result["verdict"], ",".join(kinds), (result["invalid_reason"] or "")[:100]))
            sys.stdout.flush()
        counts = {}
        for v in verdicts.values():
            counts[v] = counts.get(v, 0) + 1
        print("total: %s" % counts)
        return 0
    return 2


if __name__ == "__main__":
    sys.exit(main())
