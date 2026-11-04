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

Exit codes: 0 when everything passed, 1 when there are test data problems or test errors, 2 when the run itself failed.

See README.md next to this script.
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import uuid
from pathlib import Path
from typing import Dict, List, Optional

OPTION_PREFIX = "org.jetbrains.kotlin.testDataManager.options"
REPORT_LINE = re.compile(r"^Test data manager report: (.+\.json)$", re.MULTILINE)

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
              has_filters: bool, gradle_exit_code: int, gradle_log: Path, summary_file: Path) -> dict:
    """The index of the run: its status, the totals and counts, and the module reports holding the details."""
    run_errors = []
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
        ],
        "gradleLog": str(gradle_log),
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
                        help="project path of a module to run (repeatable); default: all modules with managed test data")
    parser.add_argument("--verbose", action="store_true", help="verbose output of the modules, in the Gradle log")
    parser.add_argument("--json", action="store_true", help="print the summary file instead of the text output")
    parser.add_argument("--gradle-arg", action="append", default=[], dest="gradle_args", help="extra Gradle argument")
    args = parser.parse_args(argv)

    root = root or Path(__file__).resolve().parents[2]
    # UUIDv7 (Python 3.14+) only sorts better; any unique id names the reports of one run
    run_id = str(getattr(uuid, "uuid7", uuid.uuid4)())
    modules = args.modules or managed_modules(root)
    output_directory = root / "build" / "test-data-manager"
    gradle_log = output_directory / f"{run_id}.gradle.log"
    summary_file = output_directory / f"{run_id}.summary.json"
    output_directory.mkdir(parents=True, exist_ok=True)

    command = gradle_command(root, args, modules, run_id)
    with gradle_log.open("w") as log:
        log.write("$ " + " ".join(command) + "\n")
        log.flush()
        gradle_exit_code = subprocess.run(command, cwd=root, stdout=log, stderr=subprocess.STDOUT).returncode

    reports = load_reports(gradle_log.read_text(), run_id)
    merged = merge(modules, reports)
    has_filters = bool(args.paths or args.class_pattern)
    summary = summarize(args.mode.upper(), run_id, modules, reports, merged, has_filters, gradle_exit_code, gradle_log,
                        summary_file)
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
