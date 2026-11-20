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
built; see `TestDataIndex`. A path that doesn't exist fails the run before Gradle starts, and so does one no generated test
has, like an output file.

The paths of the Gradle log and of the summary are printed to stderr before Gradle starts, so a long run can be followed;
`--run-id` names them and the module reports in advance. The summary is written at once when the run ends, interrupted
runs included, so it is never read half-written. When Gradle fails for another reason than a failing module report,
the summary lists its failures and the compiler errors from the log.

Exit codes: 0 when everything passed, 1 when there are test data problems or test errors, 2 when the run itself failed,
130 when it was interrupted (Ctrl+C): Gradle is stopped, and the summary has the reports of the modules that finished.

See README.md next to this script.
"""

from __future__ import annotations

import argparse
import bisect
import functools
import json
import os
import re
import signal
import subprocess
import sys
import time
import uuid
from collections import defaultdict
from pathlib import Path
from typing import Dict, Iterator, List, NamedTuple, Optional, Set, Tuple

OPTION_PREFIX = "org.jetbrains.kotlin.testDataManager.options"
REPORT_LINE = re.compile(r"^Test data manager report: (.+\.json)$", re.MULTILINE)
# The runner's rule for `runId`: it names a report file and a directory next to it
RUN_ID = re.compile(r"[A-Za-z0-9._-]+")
# A Kotlin (`e: ...`) or Java (`File.java:12: error: ...`) compiler error in the Gradle output
COMPILER_ERROR = re.compile(r"^(?:e: .*|.*\.java:\d+: error: .*)$", re.MULTILINE)
# The first line of a Gradle failure of a task; Gradle may add `(registered by plugin '...')` after the task path
TASK_FAILURE = re.compile(r"Execution failed for task '([^']+)'")
# The most lines of each Gradle failure and the most compiler errors in the summary; the rest is in the Gradle log
MAX_REPORTED_LINES = 20
# What `cmd.exe`, which runs `gradlew.bat`, takes for its own syntax outside double quotes, besides the whitespace
# `subprocess.list2cmdline` quotes; `|` and the parentheses are common in a class pattern
CMD_SPECIAL = re.compile(r'[\s"&|<>^()]')

# What gives a generated test class its structure: the `@TestMetadata` of classes (their test data directory) and of
# methods (a file in it), the `class` keyword and the braces. Comments and literals are matched as well, so that the
# braces and words inside them don't count; `Foo.class` is no declaration.
JAVA_TOKEN = re.compile(
    r'@TestMetadata\("(?P<metadata>[^"]*)"\)|(?<![\w.])(?P<declaration>class)\b|(?P<open>\{)|(?P<close>\})'
    r'|/\*.*?\*/|//[^\n]*|"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])*\'',
    re.DOTALL,
)
BUILD_GENERATED_TESTS = re.compile(r"generateTestsInBuildDirectory\s*=\s*true")

EXIT_PASSED, EXIT_FAILED, EXIT_ERROR, EXIT_INTERRUPTED = 0, 1, 2, 130

# How many entries of each list the text output prints, how many runs `build/test-data-manager` keeps
LIST_LIMIT = 50
KEPT_RUNS = 5
# A run with a log but no summary may still be running; older than this, it was killed
UNFINISHED_RUN_AGE = 24 * 60 * 60
LISTS = ["problems", "testErrors", "recoveredTests", "fileActions"]
EMPTY_PATH_ERROR = "--path is empty: give a test data file or directory"


def project_path(module: str) -> str:
    """[module] as the project path the module reports use: `analysis:stubs` and `analysis/stubs/` are `:analysis:stubs`."""
    path = ":".join(name for name in module.strip().replace("\\", "/").replace(":", "/").split("/") if name not in ("", "."))
    if not path:
        raise argparse.ArgumentTypeError(f"'{module}' is not a module path")
    return ":" + path


def valid_run_id(value: str) -> str:
    """[value] if the runner accepts it as `runId`."""
    if not RUN_ID.fullmatch(value) or value in (".", ".."):
        raise argparse.ArgumentTypeError(
            f"'{value}' must consist of letters, digits, '.', '_' and '-', and not be '.' or '..'"
        )
    return value


def gradle_wrapper(root: Path, windows: bool = os.name == "nt") -> Path:
    return root / ("gradlew.bat" if windows else "gradlew")


def windows_command_line(command: List[str]) -> str:
    """
    [command] as a command line for `gradlew.bat`. Unlike `subprocess.list2cmdline`, it also quotes the arguments with
    characters `cmd.exe` would interpret, like the `|` of a class pattern. Inside the quotes, backslashes before a quote
    and at the end are doubled, as the Windows argument parser expects.
    """
    def quote(argument: str) -> str:
        if argument and not CMD_SPECIAL.search(argument):
            return argument
        quoted, backslashes = [], 0
        for char in argument:
            if char == "\\":
                backslashes += 1
                continue
            quoted.append("\\" * (2 * backslashes + 1) + '"' if char == '"' else "\\" * backslashes + char)
            backslashes = 0
        return '"' + "".join(quoted) + "\\" * (2 * backslashes) + '"'

    return " ".join(quote(argument) for argument in command)


def managed_modules(root: Path) -> List[str]:
    """The project paths of the modules applying the `test-data-manager` plugin; their directories are default ones."""
    scripts = subprocess.run(
        ["git", "grep", "-l", "-F", 'id("test-data-manager")', "--", "*build.gradle.kts"],
        cwd=root, check=True, capture_output=True, text=True,
    ).stdout.split()
    return sorted(":" + script.rsplit("/", 1)[0].replace("/", ":") for script in scripts if "/" in script)


def module_directory(root: Path, module: str) -> Path:
    return root / module[1:].replace(":", "/")


class GeneratedTests(NamedTuple):
    """
    The generated tests of a module: [directories], the test data directories of its outermost test classes, and
    [files], the sorted test data paths of all its tests as the runner sees them (the class directory and the method's
    file), or `None` when they may be stale.
    """
    directories: List[str]
    files: Optional[List[str]]


def parse_generated_tests(text: str) -> Tuple[List[str], List[str]]:
    """The test data directories of the outermost test classes in [text], a generated test, and the paths of its tests."""
    directories, files = [], []
    depth = 0
    # The depth of the body and the directory of each enclosing class with `@TestMetadata`
    classes: List[Tuple[int, str]] = []
    metadata: Optional[str] = None
    declaration = False
    for token in JAVA_TOKEN.finditer(text):
        kind = token.lastgroup
        if kind == "metadata":
            metadata, declaration = token.group("metadata"), False
        elif kind == "declaration":
            declaration = True
        elif kind == "open":
            depth += 1
            if metadata is not None:
                if declaration:
                    if not classes:
                        directories.append(metadata)
                    classes.append((depth, metadata))
                elif classes:
                    files.append(f"{classes[-1][1]}/{metadata}")
            metadata, declaration = None, False
        elif kind == "close":
            if classes and classes[-1][0] == depth:
                classes.pop()
            depth -= 1
    return directories, files


def generated_tests(root: Path, module: str) -> Optional[GeneratedTests]:
    """
    The generated tests of [module], or `None` when it has none: then it may have tests of any test data.

    Generated tests are committed in `tests-gen`, or, with `generateTestsInBuildDirectory`, written to `build/tests-gen`
    each time the module is built. The latter may be stale: a new test data file isn't there until the next build, which
    the run itself does. So only their outermost class directories are used, which a new file or directory of a suite
    is inside of; a suite added to the generator since is missed, a rare case.
    """
    directory = module_directory(root, module)
    sources = [directory / "tests-gen"]
    build_script = directory / "build.gradle.kts"
    generated_in_build = build_script.is_file() and bool(BUILD_GENERATED_TESTS.search(build_script.read_text(encoding="utf-8")))
    if generated_in_build:
        sources.append(directory / "build" / "tests-gen")

    parsed = [
        parse_generated_tests(file.read_text(encoding="utf-8", errors="replace"))
        for source in sources
        for file in source.rglob("*Generated.java")
    ]
    if not parsed:
        return None
    return GeneratedTests(
        directories=sorted({test_directory for test_directories, _ in parsed for test_directory in test_directories}),
        files=None if generated_in_build else sorted({file for _, test_files in parsed for file in test_files}),
    )


def has_test_of(files: List[str], path: str, directory_only: bool) -> bool:
    """
    Whether a test of [files], sorted test data paths, is one the runner includes for the `testDataPath` entry [path]:
    one that starts with it at a name boundary (`TestMetadataFilter`), inside it if it is [directory_only].
    """
    prefix = path + "/" if directory_only else path
    # `.` and `/` sort before the letters and digits, so the files at a name boundary come first
    for index in range(bisect.bisect_left(files, prefix), len(files)):
        file = files[index]
        if not file.startswith(prefix):
            return False
        if directory_only or len(file) == len(prefix) or file[len(prefix)] in "./":
            return True
    return False


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
    Which modules may have tests of a `testDataPath` entry, from their generated tests ([generated_tests]).

    The runner includes a test when its path, the class directory and the file name, starts with an entry at a name
    boundary (`TestMetadataFilter`). A module whose test paths are known has tests of an entry when one of them does
    ([has_test_of], a binary search). Otherwise, it may have tests of an entry that is one of its class directories, their
    ancestor, or a path inside one: the first two are looked up among the [name_prefixes] of the class directories, the
    last one among the ancestors of the entry; the answer may include a module without such tests, but never misses one.
    A module without generated tests may have tests of any entry.
    """

    def __init__(self, tests: Dict[str, Optional[GeneratedTests]]) -> None:
        self.modules = set(tests)
        self.unknown = {module for module, module_tests in tests.items() if module_tests is None}
        self.files = {
            module: module_tests.files
            for module, module_tests in tests.items()
            if module_tests is not None and module_tests.files is not None
        }
        self.all_files = sorted(file for files in self.files.values() for file in files)
        self.by_directory: Dict[str, Set[str]] = defaultdict(set)
        self.by_prefix: Dict[str, Set[str]] = defaultdict(set)
        for module, module_tests in tests.items():
            if module_tests is None or module_tests.files is not None:
                continue
            for directory in module_tests.directories:
                self.by_directory[directory].add(module)
                for prefix in name_prefixes(directory):
                    self.by_prefix[prefix].add(module)

    def modules_for(self, path: str, directory_only: bool = False) -> Set[str]:
        """
        The modules that may have tests of [path], a repository-relative path, only inside it if it is [directory_only];
        the empty one is the whole repository.
        """
        path = path.rstrip("/")
        if not path:
            return set(self.modules)
        found = self.unknown | self.by_prefix.get(path, set())
        found |= {module for module, files in self.files.items() if has_test_of(files, path, directory_only)}
        ancestor = ""
        for name in path.split("/")[:-1]:
            ancestor = f"{ancestor}/{name}" if ancestor else name
            found |= self.by_directory.get(ancestor, set())
        return found

    def tests_named_like(self, path: str) -> List[str]:
        """The known test data files next to [path] named like it up to the first `.`: the test of an output file."""
        directory, _, name = path.rpartition("/")
        prefix = (f"{directory}/" if directory else "") + name.split(".", 1)[0] + "."
        tests = []
        for index in range(bisect.bisect_left(self.all_files, prefix), len(self.all_files)):
            test = self.all_files[index]
            if not test.startswith(prefix):
                break
            if "/" not in test[len(prefix):] and test not in tests:
                tests.append(test)
        return tests


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
    repository-relative form, as the runner does for its module. An absolute path into the repository through a symbolic
    link counts as inside it.
    """
    path = Path(os.path.normpath(root / raw_path))
    if not is_inside(path, root) and Path(raw_path).is_absolute() and is_inside(Path(os.path.realpath(raw_path)), root):
        path = Path(os.path.realpath(raw_path))
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


def split_entries(paths: List[str]) -> List[str]:
    """The `testDataPath` entries of the `--path` values: separated by commas, without the blank ones."""
    return [entry.strip() for path in paths for entry in path.split(",") if entry.strip()]


def runner_entry(entry: str, root: Path, cwd: Path) -> str:
    """
    [entry] as the runner gets it: repository-relative when it resolves, so that one through a symbolic link to the
    checkout works there too, with its trailing `/` kept; as given otherwise, for the runner to report.
    """
    path, error = resolve_path(entry, root, cwd, [])
    if error is not None or not path:
        return entry
    return path + "/" if entry.endswith("/") else path


def select_modules(root: Path, modules: List[str], paths: List[str], cwd: Path) -> Tuple[List[str], List[str]]:
    """
    The modules of [modules] that may have tests of the `testDataPath` entries [paths] ([TestDataIndex]), and the errors
    of the entries. An entry that doesn't exist fails the whole run, as it would fail every module; an entry no module
    has tests of fails the run, but the modules of the other entries still run. Such an entry is usually an output file
    passed instead of its test data file, or a new test data file the generated tests don't have yet.
    """
    entries = split_entries(paths)
    if not entries:
        return [], [EMPTY_PATH_ERROR]
    resolved = [(entry, *resolve_path(entry, root, cwd, modules)) for entry in entries]
    errors = [error for _, _, error in resolved if error is not None]
    if errors:
        return [], errors

    index = TestDataIndex({module: generated_tests(root, module) for module in modules})
    selected: Set[str] = set()
    for entry, path, _ in resolved:
        found = index.modules_for(path, directory_only=entry.endswith("/"))
        if not found:
            errors.append(unmatched_entry_error(entry, path, index, root))
        selected |= found
    return [module for module in modules if module in selected], errors


def unmatched_entry_error(entry: str, path: str, index: TestDataIndex, root: Path) -> str:
    """The error for the `testDataPath` entry [entry], resolved to [path], that no module has tests of."""
    tests = index.tests_named_like(path) if (root / path).is_file() else []
    if tests:
        return (f"testDataPath '{entry}' is an output file, not a test data file; pass its test data file "
                + " or ".join(f"'{test}'" for test in tests))
    return (f"testDataPath '{entry}' matches no managed test in any module: its files aren't managed, "
            f"or a new test data file isn't in the generated tests yet (run `./gradlew generateTests`; the tests a module "
            f"generates into `build` are updated by its build)")


def gradle_command(root: Path, args: argparse.Namespace, modules: List[str], run_id: str) -> List[str]:
    task = task_name(args.mode)
    options = {
        "runId": run_id,
        "rerunId": args.rerun,
        "testDataPath": ",".join(args.paths) or None,
        "testClassPattern": args.class_pattern,
        "goldenOnly": "true" if args.golden_only else None,
        "incremental": "true" if args.incremental else None,
        "output": "verbose" if args.verbose else None,
    }
    return [
        str(gradle_wrapper(root)), "-q", "--continue",
        *(f"{module}:{task}" for module in modules),
        *(f"-P{OPTION_PREFIX}.{key}={value}" for key, value in options.items() if value is not None),
        *args.gradle_args,
    ]


def load_reports(gradle_output: str, run_id: str) -> Dict[str, dict]:
    """The reports of this run printed in [gradle_output], by project; an unreadable one counts as missing."""
    reports = {}
    for path in REPORT_LINE.findall(gradle_output):
        try:
            report = json.loads(Path(path).read_text(encoding="utf-8"))
        except (OSError, ValueError):
            continue
        if report.get("runId") == run_id:
            reports[report["project"]] = dict(report, reportFile=path)
    return reports


def gradle_failures(gradle_output: str, reported_modules: Set[str], task: str) -> List[str]:
    """
    The failures Gradle reported in [gradle_output] (its `* What went wrong:` sections), without the tasks of
    [reported_modules]: they fail when their report does, which the summary already shows. A failure of a script also
    names its location, from the `* Where:` section before it.
    """
    expected = {f"{module}:{task}" for module in reported_modules}
    lines = gradle_output.splitlines()

    def section(start: int) -> List[str]:
        content = []
        for line in lines[start:]:
            if not line.strip():
                break
            content.append(line)
        return content

    failures = []
    where: List[str] = []
    for index, line in enumerate(lines):
        if line == "* Where:":
            where = section(index + 1)
            continue
        if line != "* What went wrong:":
            continue
        failure = section(index + 1)
        if failure and where:
            failure.insert(1, "Where: " + " ".join(where))
        where = []
        task_failure = TASK_FAILURE.match(failure[0]) if failure else None
        if failure and not (task_failure and task_failure.group(1) in expected):
            if len(failure) > MAX_REPORTED_LINES:
                failure = failure[:MAX_REPORTED_LINES] + [f"... {len(failure) - MAX_REPORTED_LINES} more lines"]
            failures.append("\n".join(failure))
    return failures


def compiler_errors(gradle_output: str) -> List[str]:
    """The compiler errors in [gradle_output], the first [MAX_REPORTED_LINES] of them."""
    errors = COMPILER_ERROR.findall(gradle_output)
    if len(errors) > MAX_REPORTED_LINES:
        return errors[:MAX_REPORTED_LINES] + [f"... {len(errors) - MAX_REPORTED_LINES} more compiler errors"]
    return errors


def task_name(mode: str) -> str:
    return "checkTestData" if mode == "check" else "updateTestData"


def merge(modules: List[str], reports: Dict[str, dict]) -> Dict[str, list]:
    """The entries of each list of the module reports, with their project."""
    return {
        key: [dict(entry, project=module) for module in modules if module in reports for entry in reports[module][key]]
        for key in LISTS
    }


def summarize(mode: str, run_id: str, modules: List[str], reports: Dict[str, dict], merged: Dict[str, list],
              has_filters: bool, gradle_exit_code: int, gradle_log: Optional[Path], summary_file: Path,
              skipped: List[str], selection_errors: List[str], gradle_output: str = "", interrupted: bool = False) -> dict:
    """The index of the run: its status, the totals and counts, and the module reports holding the details."""
    run_errors = list(selection_errors)
    if interrupted:
        run_errors.append("Interrupted before Gradle finished; the modules without a report didn't finish")
    # An interrupted Gradle reports the cancellation as its failure
    failed_gradle = gradle_exit_code != 0 and not interrupted
    failures = gradle_failures(gradle_output, set(reports), task_name(mode.lower())) if failed_gradle else []
    totals = {"discovered": 0, "passed": 0, "failed": 0, "skipped": 0}
    missing = []
    for module in modules:
        report = reports.get(module)
        if report is None:
            missing.append(module)
            continue
        for key in totals:
            totals[key] += report["tests"][key]
        run_errors += [f"{module}: {error}" for error in report["runErrors"]]
    if missing and failures:
        run_errors.append(f"No report from {', '.join(missing)}, see the Gradle failures")
    elif not interrupted:
        run_errors += [f"{module}: no report for this run; the task didn't run or its JVM crashed, see the Gradle log"
                       for module in missing]

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
        "interrupted": interrupted,
        "runErrors": run_errors,
        "gradleFailures": failures,
        "compilerErrors": compiler_errors(gradle_output) if failed_gradle else [],
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

    def print_capped(entries: list, line, where: str = "the module reports") -> None:
        for entry in entries[:LIST_LIMIT]:
            print(line(entry))
        if len(entries) > LIST_LIMIT:
            print(f"... and {len(entries) - LIST_LIMIT} more, see {where}")

    # One per path that doesn't exist, so a mistyped common prefix of many paths gives as many
    print_capped(summary["runErrors"], lambda error: f"ERROR {error}", where="the summary")
    for failure in summary["gradleFailures"]:
        first, *rest = failure.split("\n")
        print(f"GRADLE FAILURE {first}")
        for line in rest:
            print(f"  {line}")
    for error in summary["compilerErrors"]:
        print(error)

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


def failed_test_data_paths(output_directory: Path, run_id: str) -> Tuple[List[str], List[str]]:
    """
    The test data paths of the tests that failed in the run [run_id], with a problem or a test error, from its summary
    in [output_directory] and its module reports; and the errors: what failed there and can't be rerun.
    """
    summary_file = output_directory / f"{run_id}.summary.json"
    try:
        summary = json.loads(summary_file.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return [], [f"No summary of the run '{run_id}' to rerun: {summary_file}; the {KEPT_RUNS} most recent runs are kept"]

    paths: Set[str] = set()
    errors = []
    for module in summary["modules"]:
        if module["status"] in ("PASSED", "SKIPPED"):
            continue
        if module["report"] is None:
            errors.append(f"{module['project']}: no report in the run '{run_id}', so its failures are unknown")
            continue
        try:
            report = json.loads(Path(module["report"]).read_text(encoding="utf-8"))
        except (OSError, ValueError):
            errors.append(f"{module['project']}: the report of the run '{run_id}' is gone: {module['report']}")
            continue
        if report["runErrors"]:
            errors.append(f"{module['project']}: the run '{run_id}' failed there with run errors, so its failures are "
                          f"unknown: {report['runErrors'][0]}")
        for failure in report["problems"] + report["testErrors"]:
            if failure.get("testDataPath"):
                paths.add(failure["testDataPath"])
            else:
                errors.append(f"{module['project']}: {failure['test']} failed without a test data path, so it can't be rerun")
    if not paths and not errors:
        errors.append(f"The run '{run_id}' has no failed tests to rerun")
    return sorted(paths), errors


def delete_old_runs(directory: Path, kept_run: Optional[str] = None) -> None:
    """
    Keeps the files of the [KEPT_RUNS] most recent runs in [directory], and of [kept_run]. A run with only a log is
    still running, or was killed before its summary: it counts only when older than [UNFINISHED_RUN_AGE].
    """
    suffixes = (".summary.json", ".gradle.log")
    runs: Dict[str, float] = {}
    for summary in directory.glob("*.summary.json"):
        runs[summary.name[:-len(".summary.json")]] = summary.stat().st_mtime
    unfinished_before = time.time() - UNFINISHED_RUN_AGE
    for log in directory.glob("*.gradle.log"):
        run_id, modified = log.name[:-len(".gradle.log")], log.stat().st_mtime
        if run_id in runs or modified < unfinished_before:
            runs[run_id] = max(runs.get(run_id, 0.0), modified)
    for run_id in sorted(runs, key=runs.get, reverse=True)[KEPT_RUNS:]:
        if run_id == kept_run:
            continue
        for suffix in suffixes:
            (directory / f"{run_id}{suffix}").unlink(missing_ok=True)


def run_gradle(command: List[str], root: Path, log) -> Tuple[int, bool]:
    """
    Runs [command] with its output in [log]; returns its exit code and whether the run was interrupted. On Ctrl+C,
    Gradle gets SIGINT too (the terminal sends it to the whole process group; it is forwarded for a signal sent to this
    process alone) and cancels the build, which this waits for; a second Ctrl+C kills it.
    """
    process = subprocess.Popen(
        windows_command_line(command) if os.name == "nt" else command,
        cwd=root, stdout=log, stderr=subprocess.STDOUT,
    )
    try:
        return process.wait(), False
    except KeyboardInterrupt:
        print("Interrupted, waiting for Gradle to stop; Ctrl+C again kills it", file=sys.stderr, flush=True)
        try:
            if os.name != "nt":
                process.send_signal(signal.SIGINT)
            return process.wait(), True
        except KeyboardInterrupt:
            process.kill()
            return process.wait(), True


def write_atomically(file: Path, text: str) -> None:
    """Writes [file] so that it exists only complete: whoever waits for it never reads a part."""
    temporary = file.with_name(file.name + ".tmp")
    temporary.write_text(text, encoding="utf-8")
    os.replace(temporary, file)


def main(argv: Optional[List[str]] = None, root: Optional[Path] = None) -> int:
    """Runs the wrapper with [argv] (the command line by default) in the repository at [root] (the script's one)."""
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("mode", choices=["check", "update"], help="checkTestData (read-only) or updateTestData")
    parser.add_argument("--path", action="append", default=[], dest="paths", metavar="PATH[,PATH...]",
                        help="testDataPath: test data paths relative to the repository root or absolute, separated by "
                             "commas as in the task option (repeating --path adds more)")
    parser.add_argument("--class-pattern", help="testClassPattern: a regex the fully qualified name of a test class must "
                                                "match in full, like '.*PsiParsingTestGenerated'")
    parser.add_argument("--golden-only", action="store_true", help="goldenOnly")
    parser.add_argument("--incremental", action="store_true", help="incremental")
    parser.add_argument("--module", action="append", default=[], dest="modules",
                        type=project_path,
                        help="project path of a module to run (repeatable); default: the modules with managed test data, "
                             "with --path only those whose generated tests may have test data under the paths")
    parser.add_argument("--run-id", type=valid_run_id,
                        help="id of the run, naming the module reports and the Gradle log (default: a new UUID)")
    parser.add_argument("--rerun", type=valid_run_id, metavar="RUN_ID",
                        help="instead of --path, the test data files of the tests that failed in the run RUN_ID, with a "
                             "problem or a test error; this run has its own id")
    parser.add_argument("--verbose", action="store_true", help="verbose output of the modules, in the Gradle log")
    parser.add_argument("--json", action="store_true", help="print the summary file instead of the text output")
    parser.add_argument("--gradle-arg", action="append", default=[], dest="gradle_args",
                        help="extra Gradle argument (repeatable); one starting with '-' needs '=': --gradle-arg=--offline")
    args = parser.parse_args(argv)
    if args.rerun and args.paths:
        parser.error("--rerun takes the paths of the failed tests, so it can't be combined with --path")

    root = root or Path(__file__).resolve().parents[2]
    # UUIDv7 (Python 3.14+) only sorts better; any unique id names the reports of one run
    run_id = args.run_id or str(getattr(uuid, "uuid7", uuid.uuid4)())
    # The same module given twice would run once but be reported twice
    modules = list(dict.fromkeys(args.modules)) or managed_modules(root)
    output_directory = root / "build" / "test-data-manager"
    summary_file = output_directory / f"{run_id}.summary.json"
    output_directory.mkdir(parents=True, exist_ok=True)
    skipped: List[str] = []
    selection_errors: List[str] = []
    if args.rerun:
        args.paths, selection_errors = failed_test_data_paths(output_directory, args.rerun)
        if not args.paths:
            modules = []
        print(f"Rerunning {len(args.paths)} test data file{'' if len(args.paths) == 1 else 's'} of the run {args.rerun}",
              file=sys.stderr)
    if args.paths:
        entries = split_entries(args.paths)
        if not entries:
            selection_errors.append(EMPTY_PATH_ERROR)
            modules = []
        args.paths = [runner_entry(entry, root, Path.cwd()) for entry in entries]
    if args.paths and not args.modules:
        selected, path_errors = select_modules(root, modules, args.paths, Path.cwd())
        selection_errors += path_errors
        # Nothing is selected when a path doesn't exist: the modules aren't skipped for having no tests
        if selected or not path_errors:
            skipped = [module for module in modules if module not in selected]
        modules = selected

    gradle_log: Optional[Path] = None
    gradle_exit_code = 0
    interrupted = False
    gradle_output = ""
    reports: Dict[str, dict] = {}
    if modules:
        gradle_log = output_directory / f"{run_id}.gradle.log"
        command = gradle_command(root, args, modules, run_id)
        print(f"Running {task_name(args.mode)} in {', '.join(modules)}", file=sys.stderr)
        print(f"Gradle log: {gradle_log}", file=sys.stderr)
        print(f"Summary, written when the run ends: {summary_file}", file=sys.stderr, flush=True)
        with gradle_log.open("w", encoding="utf-8") as log:
            log.write("$ " + " ".join(command) + "\n")
            log.flush()
            gradle_exit_code, interrupted = run_gradle(command, root, log)
        # Gradle may write in the console code page on Windows
        gradle_output = gradle_log.read_text(encoding="utf-8", errors="replace")
        reports = load_reports(gradle_output, run_id)

    merged = merge(modules, reports)
    has_filters = bool(args.paths or args.class_pattern)
    summary = summarize(args.mode.upper(), run_id, modules, reports, merged, has_filters, gradle_exit_code, gradle_log,
                        summary_file, skipped, selection_errors, gradle_output, interrupted)
    write_atomically(summary_file, json.dumps(summary, indent=2) + "\n")
    delete_old_runs(output_directory, kept_run=args.rerun)

    if args.json:
        print(json.dumps(summary, indent=2))
    else:
        print_summary(summary, merged)

    if interrupted:
        return EXIT_INTERRUPTED
    return {"PASSED": EXIT_PASSED, "FAILED": EXIT_FAILED}.get(summary["status"], EXIT_ERROR)


if __name__ == "__main__":
    # A Windows console may not encode every character of a test name or message
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(errors="backslashreplace")
    try:
        sys.exit(main())
    except KeyboardInterrupt:  # before Gradle started or after it finished: nothing to wait for
        print("Interrupted", file=sys.stderr)
        sys.exit(EXIT_INTERRUPTED)
    except Exception as error:  # an unexpected failure is a failed run, not test data problems
        import traceback
        traceback.print_exc()
        print(f"ERROR the script failed: {error}", file=sys.stderr)
        sys.exit(EXIT_ERROR)
