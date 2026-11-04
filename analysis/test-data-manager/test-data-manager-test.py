#!/usr/bin/env python3
#
# Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
# Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
#

"""
Tests of `test-data-manager.py`, with the standard library only:

    python3 analysis/test-data-manager/test-data-manager-test.py

They run the wrapper in a temporary git repository whose `gradlew` is a stub writing the module reports the way
the runner does.
"""

from __future__ import annotations

import contextlib
import importlib.util
import io
import json
import os
import re
import subprocess
import sys
import tempfile
import textwrap
import unittest
from pathlib import Path
from typing import Dict, List, Tuple

# Loading the script must not leave `__pycache__` in the repository
sys.dont_write_bytecode = True
SCRIPT = Path(__file__).resolve().parent / "test-data-manager.py"
_spec = importlib.util.spec_from_file_location("test_data_manager", SCRIPT)
tdm = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(tdm)

# Writes and prints the report of each `<project>:<task>` from `stub-gradle.json`: {project: report fields}, a project
# without an entry writes none, `"raw"` replaces the report text; `"stray"` also prints a report of another run,
# `"exitCode"` overrides the exit code.
STUB_GRADLEW = textwrap.dedent('''\
    #!/usr/bin/env python3
    import json, sys
    from pathlib import Path

    root = Path(__file__).resolve().parent
    config = json.loads((root / "stub-gradle.json").read_text())
    (root / "build").mkdir(exist_ok=True)
    (root / "build" / "stub-gradle-args.txt").write_text("\\n".join(sys.argv[1:]))
    options = dict(arg[2:].split("=", 1) for arg in sys.argv[1:] if arg.startswith("-P"))
    run_id = options["org.jetbrains.kotlin.testDataManager.options.runId"]
    failed = False
    for task in (arg for arg in sys.argv[1:] if not arg.startswith("-")):
        project = task.rsplit(":", 1)[0]
        entry = config.get(project)
        if entry is None:
            continue
        report = {
            "runId": run_id, "project": project, "status": "PASSED", "runErrors": [],
            "tests": {"discovered": 1, "passed": 1, "failed": 0, "skipped": 0},
            "problems": [], "testErrors": [], "recoveredTests": [], "fileActions": [],
        }
        report.update(entry.get("report", {}))
        failed = failed or report["status"] != "PASSED"
        file = root / project[1:].replace(":", "/") / "build" / "test-data-manager" / (run_id + ".json")
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(entry.get("raw", json.dumps(report)))
        print("Test data manager report: " + str(file))
    if "stray" in config:
        stray = root / "build" / "other.json"
        stray.write_text(json.dumps(dict(config["stray"], runId="other-run")))
        print("Test data manager report: " + str(stray))
    sys.exit(config.get("exitCode", 1 if failed else 0))
''')

ALL_MODULES = [":mod-a", ":mod-a:nested", ":mod-b"]


def git(root: Path, *args: str) -> None:
    subprocess.run(["git", *args], cwd=root, check=True, capture_output=True)


class WrapperTest(unittest.TestCase):
    """A temporary git repository with the managed modules [ALL_MODULES] and a stub `gradlew`."""

    def setUp(self) -> None:
        self._temp = tempfile.TemporaryDirectory()
        self.root = Path(self._temp.name).resolve()
        for module in ALL_MODULES:
            self.write(f"{module[1:].replace(':', '/')}/build.gradle.kts", 'plugins { id("test-data-manager") }\n')
        self.write("unmanaged/build.gradle.kts", 'plugins { kotlin("jvm") }\n')
        self.write("gradlew", STUB_GRADLEW).chmod(0o755)
        git(self.root, "init", "-q")
        git(self.root, "add", "-A")
        git(self.root, "-c", "user.name=Test", "-c", "user.email=test@example.com", "-c", "commit.gpgsign=false",
            "commit", "-q", "-m", "init")

    def tearDown(self) -> None:
        self._temp.cleanup()

    def write(self, path: str, content: str) -> Path:
        file = self.root / path
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(content)
        return file

    def run_wrapper(self, stub: Dict[str, object], *args: str) -> Tuple[int, dict]:
        self.write("stub-gradle.json", json.dumps(stub))
        with contextlib.redirect_stdout(io.StringIO()) as stdout:
            exit_code = tdm.main([*args, "--json"], root=self.root)
        return exit_code, json.loads(stdout.getvalue())

    def run_text(self, stub: Dict[str, object], *args: str) -> Tuple[int, str]:
        """The text output, with the run id and the file paths replaced."""
        self.write("stub-gradle.json", json.dumps(stub))
        with contextlib.redirect_stdout(io.StringIO()) as stdout:
            exit_code = tdm.main(list(args), root=self.root)
        output = re.sub(r"(Gradle log|Summary): .*", r"\1: <path>", stdout.getvalue())
        return exit_code, re.sub(r"runId \S+", "runId <id>", output)

    def gradle_args(self) -> List[str]:
        return (self.root / "build" / "stub-gradle-args.txt").read_text().split("\n")

    def test_runs_every_managed_module_with_the_options(self) -> None:
        exit_code, summary = self.run_wrapper(
            {module: {} for module in ALL_MODULES},
            "update", "--path", "mod-a/testData", "--path", "other/", "--incremental", "--verbose",
        )
        self.assertEqual((0, "PASSED"), (exit_code, summary["status"]))
        self.assertEqual({"discovered": 3, "passed": 3, "failed": 0, "skipped": 0}, summary["tests"])
        prefix = "-Porg.jetbrains.kotlin.testDataManager.options."
        self.assertEqual(
            ["-q", "--continue", ":mod-a:updateTestData", ":mod-a:nested:updateTestData", ":mod-b:updateTestData",
             f"{prefix}runId={summary['runId']}", f"{prefix}testDataPath=mod-a/testData,other/",
             f"{prefix}incremental=true", f"{prefix}output=verbose"],
            self.gradle_args(),
        )

    def test_requested_modules_only(self) -> None:
        exit_code, summary = self.run_wrapper({":mod-b": {}}, "check", "--module", ":mod-b", "--gradle-arg=--offline")
        self.assertEqual((0, [":mod-b"]), (exit_code, [module["project"] for module in summary["modules"]]))
        self.assertEqual(["-q", "--continue", ":mod-b:checkTestData"], self.gradle_args()[:3])
        self.assertEqual("--offline", self.gradle_args()[-1])

    def test_problems_errors_and_recovered_tests_are_merged(self) -> None:
        stub = {
            ":mod-a": {"report": {
                "status": "FAILED",
                "problems": [{"kind": "MISMATCH", "file": "mod-a/testData/file.txt", "test": "FooTestGenerated.testFile"}],
                "testErrors": [{"test": "FooTestGenerated.testBroken", "exception": "java.lang.IllegalStateException",
                                "message": "Broken\nsecond line"}],
                "recoveredTests": [{"test": "FooTestGenerated.testFlaky", "passedInPass": 2, "failures": ["java.lang.Error"]}],
                "tests": {"discovered": 3, "passed": 1, "failed": 2, "skipped": 0},
            }},
            ":mod-a:nested": {"report": {"fileActions": [{"kind": "CREATED", "file": "mod-a/nested/testData/x.txt"}]}},
            ":mod-b": {"report": {"problems": [{"kind": "MISSING_FILE", "file": "mod-b/testData/y.txt", "test": "BarTestGenerated.testY"}]}},
        }
        exit_code, summary = self.run_wrapper(stub, "check")
        self.assertEqual((1, "FAILED"), (exit_code, summary["status"]))
        self.assertEqual({"discovered": 5, "passed": 3, "failed": 2, "skipped": 0}, summary["tests"])
        self.assertEqual(
            {"problems": {"MISMATCH": 1, "MISSING_FILE": 1}, "testErrors": 1, "recoveredTests": 1, "fileActions": {"CREATED": 1}},
            summary["counts"],
        )
        self.assertEqual(textwrap.dedent('''\
            Test data manager CHECK: FAILED, runId <id>
            5 tests: 3 passed, 2 failed, 0 skipped
            MISMATCH mod-a/testData/file.txt (FooTestGenerated.testFile)
            MISSING_FILE mod-b/testData/y.txt (BarTestGenerated.testY)
            TEST ERROR FooTestGenerated.testBroken: java.lang.IllegalStateException: Broken
            RECOVERED FooTestGenerated.testFlaky (passed in pass 2): java.lang.Error
            CREATED mod-a/nested/testData/x.txt
            Gradle log: <path>
            Summary: <path>
        '''), self.run_text(stub, "check")[1])

    def test_text_lists_are_capped(self) -> None:
        problems = [{"kind": "MISMATCH", "file": f"mod-a/testData/{index:02}.txt", "test": "FooTestGenerated.test"}
                    for index in range(55)]
        exit_code, output = self.run_text({":mod-a": {"report": {"status": "FAILED", "problems": problems}}}, "check",
                                          "--module", ":mod-a")
        lines = output.splitlines()
        self.assertEqual(1, exit_code)
        self.assertEqual(
            ["MISMATCH mod-a/testData/49.txt (FooTestGenerated.test)", "... and 5 more, see the module reports"],
            lines[-4:-2],
        )
        self.assertEqual(50, sum(1 for line in lines if line.startswith("MISMATCH")))

        # A mass update: the files changed across the modules
        def updated_files(module: str) -> list:
            directory = tdm.module_directory(self.root, module).relative_to(self.root).as_posix()
            return [{"kind": "UPDATED", "file": f"{directory}/testData/{index:02}.txt"} for index in range(40)]

        actions = {module: {"report": {"fileActions": updated_files(module)}} for module in ALL_MODULES}
        exit_code, output = self.run_text(actions, "update")
        lines = output.splitlines()
        self.assertEqual(0, exit_code)
        self.assertEqual(50, sum(1 for line in lines if line.startswith("UPDATED")))
        self.assertEqual(
            ["UPDATED mod-a/nested/testData/09.txt", "... and 70 more, see the module reports"],
            lines[-4:-2],
        )

    def test_summary_file_is_the_json_output_and_its_path_is_the_last_line(self) -> None:
        exit_code, summary = self.run_wrapper({":mod-b": {}}, "check", "--module", ":mod-b")
        summary_file = Path(summary["summary"])
        self.assertEqual(self.root / "build" / "test-data-manager" / f"{summary['runId']}.summary.json", summary_file)
        self.assertEqual(summary, json.loads(summary_file.read_text()))
        self.assertEqual([{"project": ":mod-b", "status": "PASSED", "tests": {"discovered": 1, "passed": 1, "failed": 0, "skipped": 0},
                           "report": str(self.root / "mod-b" / "build" / "test-data-manager" / f"{summary['runId']}.json")}],
                         summary["modules"])
        self.assertTrue(self.run_text({":mod-b": {}}, "check", "--module", ":mod-b")[1].endswith("Summary: <path>\n"))

    def test_keeps_the_most_recent_runs(self) -> None:
        directory = self.root / "build" / "test-data-manager"
        for index in range(6):
            for suffix in (".summary.json", ".gradle.log"):
                file = self.write(f"build/test-data-manager/old-{index}{suffix}", "{}")
                os.utime(file, (index, index))
        _, summary = self.run_wrapper({":mod-b": {}}, "check", "--module", ":mod-b")
        self.assertEqual(
            sorted([f"old-{index}{suffix}" for index in range(2, 6) for suffix in (".gradle.log", ".summary.json")] +
                   [f"{summary['runId']}.gradle.log", f"{summary['runId']}.summary.json"]),
            sorted(file.name for file in directory.iterdir() if file.name != "stub-gradle-args.txt"),
        )

    def test_module_run_errors_fail_the_run(self) -> None:
        invalid = {"report": {"status": "ERROR", "runErrors": ["testDataPath 'nope' does not exist"]}}
        exit_code, summary = self.run_wrapper({module: invalid for module in ALL_MODULES}, "check", "--path", "nope")
        self.assertEqual(2, exit_code)
        self.assertEqual([f"{module}: testDataPath 'nope' does not exist" for module in ALL_MODULES], summary["runErrors"])

    def test_missing_unreadable_and_stray_reports(self) -> None:
        exit_code, summary = self.run_wrapper({
            ":mod-a": {},
            ":mod-a:nested": {"raw": '{"runId": "cut'},
            "stray": {"project": ":mod-b", "status": "PASSED"},
        }, "check")
        self.assertEqual((2, "ERROR"), (exit_code, summary["status"]))
        self.assertEqual(["PASSED", "MISSING", "MISSING"], [module["status"] for module in summary["modules"]])
        self.assertEqual(
            [f"{module}: no report for this run; the task didn't run or its JVM crashed, see the Gradle log"
             for module in [":mod-a:nested", ":mod-b"]],
            summary["runErrors"],
        )

    def test_filters_matching_nothing_are_an_error(self) -> None:
        empty = {"report": {"tests": {"discovered": 0, "passed": 0, "failed": 0, "skipped": 0}}}
        stub = {module: empty for module in ALL_MODULES}
        exit_code, summary = self.run_wrapper(stub, "check", "--class-pattern", ".*Nope.*")
        self.assertEqual((2, ["No managed test in any module matches the filters"]), (exit_code, summary["runErrors"]))
        exit_code, summary = self.run_wrapper(stub, "check")
        self.assertEqual((0, []), (exit_code, summary["runErrors"]))

    def test_gradle_failure_without_a_failing_report_is_an_error(self) -> None:
        exit_code, summary = self.run_wrapper({**{module: {} for module in ALL_MODULES}, "exitCode": 1}, "check")
        self.assertEqual(2, exit_code)
        self.assertEqual(["Gradle failed (exit code 1) without a failing report, see the Gradle log"], summary["runErrors"])

    def test_gradle_output_goes_to_the_log(self) -> None:
        _, summary = self.run_wrapper({":mod-b": {}}, "check", "--module", ":mod-b")
        log = Path(summary["gradleLog"]).read_text()
        self.assertTrue(log.startswith(f"$ {self.root / 'gradlew'} -q --continue :mod-b:checkTestData"))
        self.assertIn("Test data manager report: ", log)



if __name__ == "__main__":
    unittest.main()
