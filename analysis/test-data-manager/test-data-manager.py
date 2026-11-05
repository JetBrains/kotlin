#!/usr/bin/env python3
#
# Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

"""
A thin wrapper over `checkTestData` and `updateTestData` that aggregates the module reports of one run.

It runs the task in the modules with managed test data in one Gradle call, keeps the Gradle output in a log, and reads
the report every module prints the path of. The options are passed to the runner as is: the runner validates them.
On top of the module reports, it reports what a single module can't see: a module that wrote no report (the task didn't
run, or its JVM crashed) and filters that match no test in any module.

The summary goes to `build/test-data-manager/<runId>.summary.json`, whose path is the last line of the output: an index
of the run (status, counts, the module reports), while the problems and file changes themselves stay in the module
reports. The text output lists at most the first 50 entries of each kind.

With `--path`, only the modules whose generated tests may have test data under the paths run, so the others aren't even
built; see `TestDataIndex`. A path that doesn't exist fails the run before Gradle starts.

Exit codes: 0 when everything passed, 1 when there are test data problems or test errors, 2 when the run itself failed.

See README.md next to this script.
"""

from __future__ import annotations

import argparse
import bisect
import functools
import json
import os
import re
import subprocess
import sys
import uuid
from collections import defaultdict
from pathlib import Path
from typing import Dict, Iterator, List, Optional, Set, Tuple

OPTION_PREFIX = "org.jetbrains.kotlin.testDataManager.options"
REPORT_LINE = re.compile(r"^Test data manager report: (.+\.json)$", re.MULTILINE)

# The `@TestMetadata` of a generated test class, the test data directory of its tests; the annotations and modifiers
# between it and `class` rule out the ones of methods, which name a file in that directory
CLASS_TEST_METADATA = re.compile(
    r'@TestMetadata\("([^"]*)"\)\s*(?:@[\w.]+(?:\([^)]*\))?\s*)*(?:(?:public|static|abstract|final)\s+)*class\s'
)
BUILD_GENERATED_TESTS = re.compile(r"generateTestsInBuildDirectory\s*=\s*true")

EXIT_PASSED, EXIT_FAILED, EXIT_ERROR = 0, 1, 2

# How many entries of each list the text output prints, how many runs `build/test-data-manager` keeps
LIST_LIMIT = 50
KEPT_RUNS = 5
LISTS = ["problems", "testErrors", "recoveredTests", "fileActions"]


def managed_modules(root: Path) -> List[str]:
    """The project paths of the modules applying the `test-data-manager` plugin; their directories are default ones."""
    scripts = subprocess.run(
        ["git", "grep", "-l", "-F", 'id("test-data-manager")', "--", "*build.gradle.kts"],
        cwd=root, check=True, capture_output=True, text=True,
    ).stdout.split()
    return sorted(":" + script.rsplit("/", 1)[0].replace("/", ":") for script in scripts if "/" in script)


def module_directory(root: Path, module: str) -> Path:
    return root / module[1:].replace(":", "/")


def generated_test_directories(root: Path, module: str) -> Optional[Set[str]]:
    """
    The test data directories of the generated test classes of [module], from their class-level `@TestMetadata`, or
    `None` when they are unknown: no generated tests found, or tests generated into `build` that may be stale.

    Generated tests are committed in `tests-gen`, or, with `generateTestsInBuildDirectory`, written to `build/tests-gen`
    each time the module is built. The latter are used only when they are newer than every tracked file of the module
    outside `testData`, where its generator lives: a new test data file in a known directory is found by its ancestor
    anyway, but a suite added to the generator is not in `build/tests-gen` until the module is built again.
    """
    directory = module_directory(root, module)
    sources = [directory / "tests-gen"]
    build_script = directory / "build.gradle.kts"
    if build_script.is_file() and BUILD_GENERATED_TESTS.search(build_script.read_text(encoding="utf-8")):
        generated = directory / "build" / "tests-gen"
        if not generated_after_sources(root, directory, generated):
            return None
        sources.append(generated)

    files = [file for source in sources for file in source.rglob("*Generated.java")]
    if not files:
        return None
    return {
        test_data_directory
        for file in files
        for test_data_directory in CLASS_TEST_METADATA.findall(file.read_text(encoding="utf-8", errors="replace"))
    }


def generated_after_sources(root: Path, directory: Path, generated: Path) -> bool:
    """Whether the files in [generated] exist and are newer than the tracked files of [directory] outside `testData`."""
    generated_files = [file for file in generated.rglob("*") if file.is_file()]
    if not generated_files:
        return False
    generated_at = min(file.stat().st_mtime for file in generated_files)
    tracked = subprocess.run(
        ["git", "ls-files", "-z", "--", str(directory.relative_to(root))],
        cwd=root, check=True, capture_output=True, text=True,
    ).stdout.split("\0")
    sources = (root / file for file in tracked if file and "testData" not in file.split("/"))
    return all(source.stat().st_mtime <= generated_at for source in sources if source.is_file())


def name_prefixes(directory: str) -> Iterator[str]:
    """
    The paths that match [directory] at a name boundary: the directory and its ancestors, each also cut before a `.`
    in its last name (`a/b` for `a/b.c`), as the runner matches a file named without its extension.
    """
    names = directory.split("/")
    for end, name in enumerate(names):
        parent = "/".join(names[:end])
        for cut in [index for index, char in enumerate(name) if char == "."] + [len(name)]:
            if cut > 0:
                yield f"{parent}/{name[:cut]}" if parent else name[:cut]


class TestDataIndex:
    """
    Which modules may have tests of a `testDataPath` entry, from the test data directories of their generated test
    classes ([generated_test_directories]).

    The runner includes a test when its path, the class directory and the file name, starts with an entry at a name
    boundary (`TestMetadataFilter`). So a module may have tests of an entry that is a class directory, its ancestor, or a
    path inside it: the first two are looked up among the [name_prefixes] of all class directories, the last one among
    the ancestors of the entry. The cost is linear in the number of entries and of class directories, times the depth.
    The answer may include a module without such tests, but never misses one. A module whose directories are unknown
    may have tests of any entry.
    """

    def __init__(self, directories: Dict[str, Optional[Set[str]]]) -> None:
        self.modules = set(directories)
        self.unknown = {module for module, module_directories in directories.items() if module_directories is None}
        self.by_directory: Dict[str, Set[str]] = defaultdict(set)
        self.by_prefix: Dict[str, Set[str]] = defaultdict(set)
        for module, module_directories in directories.items():
            for directory in module_directories or ():
                self.by_directory[directory].add(module)
                for prefix in name_prefixes(directory):
                    self.by_prefix[prefix].add(module)

    def modules_for(self, path: str) -> Set[str]:
        """The modules that may have tests of [path], a repository-relative path; the empty one is the whole repository."""
        path = path.rstrip("/")
        if not path:
            return set(self.modules)
        found = self.unknown | self.by_prefix.get(path, set())
        ancestor = ""
        for name in path.split("/")[:-1]:
            ancestor = f"{ancestor}/{name}" if ancestor else name
            found |= self.by_directory.get(ancestor, set())
        return found


@functools.lru_cache(maxsize=None)
def sorted_entries(directory: Path) -> List[str]:
    return sorted(entry.name for entry in directory.iterdir())


def exists_as_test_data_path(path: Path) -> bool:
    """Whether [path] exists as a directory, a file, or a file named without its extension, as the runner checks it."""
    if path.exists():
        return True
    if not path.parent.is_dir():
        return False
    entries = sorted_entries(path.parent)
    index = bisect.bisect_left(entries, path.name + ".")
    return index < len(entries) and entries[index].startswith(path.name + ".")


def is_inside(path: Path, root: Path) -> bool:
    return path == root or root in path.parents


def resolve_path(raw_path: str, root: Path, cwd: Path, modules: List[str]) -> Tuple[Optional[str], Optional[str]]:
    """
    [raw_path] relative to [root], as `@TestMetadata` paths are, or the error the runner would report for it; for a path
    that exists relative to [cwd] or to the directory of one of [modules] instead, the error suggests its
    repository-relative form, as the runner does for its module.
    """
    path = Path(os.path.normpath(root / raw_path))
    if not is_inside(path, root):
        return None, f"testDataPath '{raw_path}' is outside the repository root {root}"
    if not exists_as_test_data_path(path):
        message = f"testDataPath '{raw_path}' does not exist; paths are relative to the repository root {root}"
        bases = [cwd] + [module_directory(root, module) for module in modules]
        alternatives = dict.fromkeys(
            alternative.relative_to(root).as_posix()
            for alternative in (Path(os.path.normpath(base / raw_path)) for base in bases)
            if alternative != root and is_inside(alternative, root) and exists_as_test_data_path(alternative)
        )
        if alternatives:
            message += ". Did you mean " + " or ".join(f"'{alternative}'" for alternative in alternatives) + "?"
        return None, message
    return ("" if path == root else path.relative_to(root).as_posix()), None


def select_modules(root: Path, modules: List[str], paths: List[str], cwd: Path) -> Tuple[List[str], List[str]]:
    """
    The modules of [modules] that may have tests of the `testDataPath` entries [paths] ([TestDataIndex]), and the errors
    of the entries. An entry that doesn't exist fails the whole run, as it would fail every module; an entry no module
    has tests of fails the run, but the modules of the other entries still run.
    """
    entries = [entry.strip() for path in paths for entry in path.split(",") if entry.strip()]
    resolved = [(entry, *resolve_path(entry, root, cwd, modules)) for entry in entries]
    errors = [error for _, _, error in resolved if error is not None]
    if errors:
        return [], errors

    index = TestDataIndex({module: generated_test_directories(root, module) for module in modules})
    selected: Set[str] = set()
    for entry, path, _ in resolved:
        found = index.modules_for(path)
        if not found:
            errors.append(f"testDataPath '{entry}' matches no managed test in any module")
        selected |= found
    return [module for module in modules if module in selected], errors


def gradle_command(root: Path, args: argparse.Namespace, modules: List[str], run_id: str) -> List[str]:
    task = "checkTestData" if args.mode == "check" else "updateTestData"
    options = {
        "runId": run_id,
        "testDataPath": ",".join(args.paths) or None,
        "testClassPattern": args.class_pattern,
        "goldenOnly": "true" if args.golden_only else None,
        "incremental": "true" if args.incremental else None,
        "output": "verbose" if args.verbose else None,
    }
    return [
        str(root / "gradlew"), "-q", "--continue",
        *(f"{module}:{task}" for module in modules),
        *(f"-P{OPTION_PREFIX}.{key}={value}" for key, value in options.items() if value is not None),
        *args.gradle_args,
    ]


def load_reports(gradle_output: str, run_id: str) -> Dict[str, dict]:
    """The reports of this run printed in [gradle_output], by project; an unreadable one counts as missing."""
    reports = {}
    for path in REPORT_LINE.findall(gradle_output):
        try:
            report = json.loads(Path(path).read_text())
        except (OSError, ValueError):
            continue
        if report.get("runId") == run_id:
            reports[report["project"]] = dict(report, reportFile=path)
    return reports


def merge(modules: List[str], reports: Dict[str, dict]) -> Dict[str, list]:
    """The entries of each list of the module reports, with their project."""
    return {
        key: [dict(entry, project=module) for module in modules if module in reports for entry in reports[module][key]]
        for key in LISTS
    }


def summarize(mode: str, run_id: str, modules: List[str], reports: Dict[str, dict], merged: Dict[str, list],
              has_filters: bool, gradle_exit_code: int, gradle_log: Optional[Path], summary_file: Path,
              skipped: List[str], selection_errors: List[str]) -> dict:
    """The index of the run: its status, the totals and counts, and the module reports holding the details."""
    run_errors = list(selection_errors)
    totals = {"discovered": 0, "passed": 0, "failed": 0, "skipped": 0}
    for module in modules:
        report = reports.get(module)
        if report is None:
            run_errors.append(f"{module}: no report for this run; the task didn't run or its JVM crashed, see the Gradle log")
            continue
        for key in totals:
            totals[key] += report["tests"][key]
        run_errors += [f"{module}: {error}" for error in report["runErrors"]]

    if has_filters and totals["discovered"] == 0 and not run_errors:
        run_errors.append("No managed test in any module matches the filters")
    if gradle_exit_code != 0 and not run_errors and not merged["problems"] and not merged["testErrors"]:
        run_errors.append(f"Gradle failed (exit code {gradle_exit_code}) without a failing report, see the Gradle log")

    def by_kind(entries: list) -> Dict[str, int]:
        return {kind: sum(1 for entry in entries if entry["kind"] == kind) for kind in sorted({entry["kind"] for entry in entries})}

    status = "ERROR" if run_errors else "FAILED" if merged["problems"] or merged["testErrors"] else "PASSED"
    return {
        "runId": run_id,
        "mode": mode,
        "status": status,
        "runErrors": run_errors,
        "tests": totals,
        "counts": {
            "problems": by_kind(merged["problems"]),
            "testErrors": len(merged["testErrors"]),
            "recoveredTests": len(merged["recoveredTests"]),
            "fileActions": by_kind(merged["fileActions"]),
        },
        "modules": [
            {"project": module, "status": reports[module]["status"] if module in reports else "MISSING",
             "tests": reports[module]["tests"] if module in reports else None,
             "report": reports[module]["reportFile"] if module in reports else None}
            for module in modules
        ] + [{"project": module, "status": "SKIPPED", "tests": None, "report": None} for module in skipped],
        "gradleLog": str(gradle_log) if gradle_log is not None else None,
        "summary": str(summary_file),
    }


def print_summary(summary: dict, merged: Dict[str, list]) -> None:
    tests = summary["tests"]
    print(f"Test data manager {summary['mode']}: {summary['status']}, runId {summary['runId']}")
    print(f"{tests['discovered']} tests: {tests['passed']} passed, {tests['failed']} failed, {tests['skipped']} skipped")
    for error in summary["runErrors"]:
        print(f"ERROR {error}")

    def print_capped(entries: list, line) -> None:
        for entry in entries[:LIST_LIMIT]:
            print(line(entry))
        if len(entries) > LIST_LIMIT:
            print(f"... and {len(entries) - LIST_LIMIT} more, see the module reports")

    print_capped(merged["problems"], lambda problem: f"{problem['kind']} {problem['file']} ({problem['test']})")
    print_capped(merged["testErrors"], lambda error: f"TEST ERROR {error['test']}: {error['exception']}" +
                 (f": {error['message'].split(chr(10))[0][:200]}" if error["message"] else ""))
    print_capped(merged["recoveredTests"], lambda test: f"RECOVERED {test['test']} "
                 f"(passed in pass {test['passedInPass']}): {'; '.join(test['failures'])}")
    print_capped(merged["fileActions"], lambda action: f"{action['kind']} {action['file']}")
    skipped = [module["project"] for module in summary["modules"] if module["status"] == "SKIPPED"]
    if skipped:
        print(f"Skipped {len(skipped)} module{'' if len(skipped) == 1 else 's'} without tests under the paths, see the summary")
    if summary["gradleLog"] is not None:
        print(f"Gradle log: {summary['gradleLog']}")
    print(f"Summary: {summary['summary']}")


def delete_old_runs(directory: Path) -> None:
    """Keeps the files of the [KEPT_RUNS] most recent runs in [directory]."""
    runs: Dict[str, float] = {}
    for file in directory.glob("*.summary.json"):
        runs[file.name[:-len(".summary.json")]] = file.stat().st_mtime
    for run_id in sorted(runs, key=runs.get, reverse=True)[KEPT_RUNS:]:
        for suffix in (".summary.json", ".gradle.log"):
            (directory / f"{run_id}{suffix}").unlink(missing_ok=True)


def main(argv: Optional[List[str]] = None, root: Optional[Path] = None) -> int:
    """Runs the wrapper with [argv] (the command line by default) in the repository at [root] (the script's one)."""
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("mode", choices=["check", "update"], help="checkTestData (read-only) or updateTestData")
    parser.add_argument("--path", action="append", default=[], dest="paths", help="testDataPath entry (repeatable)")
    parser.add_argument("--class-pattern", help="testClassPattern")
    parser.add_argument("--golden-only", action="store_true", help="goldenOnly")
    parser.add_argument("--incremental", action="store_true", help="incremental")
    parser.add_argument("--module", action="append", default=[], dest="modules",
                        help="project path of a module to run (repeatable); default: the modules with managed test data, "
                             "with --path only those whose generated tests may have test data under the paths")
    parser.add_argument("--verbose", action="store_true", help="verbose output of the modules, in the Gradle log")
    parser.add_argument("--json", action="store_true", help="print the summary file instead of the text output")
    parser.add_argument("--gradle-arg", action="append", default=[], dest="gradle_args", help="extra Gradle argument")
    args = parser.parse_args(argv)

    root = root or Path(__file__).resolve().parents[2]
    # UUIDv7 (Python 3.14+) only sorts better; any unique id names the reports of one run
    run_id = str(getattr(uuid, "uuid7", uuid.uuid4)())
    modules = args.modules or managed_modules(root)
    output_directory = root / "build" / "test-data-manager"
    summary_file = output_directory / f"{run_id}.summary.json"
    output_directory.mkdir(parents=True, exist_ok=True)
    skipped: List[str] = []
    selection_errors: List[str] = []
    if args.paths and not args.modules:
        selected, selection_errors = select_modules(root, modules, args.paths, Path.cwd())
        # Nothing is selected when a path doesn't exist: the modules aren't skipped for having no tests
        if selected or not selection_errors:
            skipped = [module for module in modules if module not in selected]
        modules = selected

    gradle_log: Optional[Path] = None
    gradle_exit_code = 0
    reports: Dict[str, dict] = {}
    if modules:
        gradle_log = output_directory / f"{run_id}.gradle.log"
        command = gradle_command(root, args, modules, run_id)
        with gradle_log.open("w") as log:
            log.write("$ " + " ".join(command) + "\n")
            log.flush()
            gradle_exit_code = subprocess.run(command, cwd=root, stdout=log, stderr=subprocess.STDOUT).returncode
        reports = load_reports(gradle_log.read_text(), run_id)

    merged = merge(modules, reports)
    has_filters = bool(args.paths or args.class_pattern)
    summary = summarize(args.mode.upper(), run_id, modules, reports, merged, has_filters, gradle_exit_code, gradle_log,
                        summary_file, skipped, selection_errors)
    summary_file.write_text(json.dumps(summary, indent=2) + "\n")
    delete_old_runs(output_directory)

    if args.json:
        print(json.dumps(summary, indent=2))
    else:
        print_summary(summary, merged)

    return {"PASSED": EXIT_PASSED, "FAILED": EXIT_FAILED}.get(summary["status"], EXIT_ERROR)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as error:  # an unexpected failure is a failed run, not test data problems
        import traceback
        traceback.print_exc()
        print(f"ERROR the script failed: {error}", file=sys.stderr)
        sys.exit(EXIT_ERROR)
