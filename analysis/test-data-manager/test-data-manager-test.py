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
import time
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
# `"output"` is printed after the reports, `"exitCode"` overrides the exit code.
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
    if "output" in config:
        print(config["output"])
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
        # Windows runs `gradlew.bat`
        self.write("gradlew.bat", f'@"{sys.executable}" "%~dp0gradlew" %*\n')
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

    def generated_tests(self, module_path: str, *directories: str, into: str = "tests-gen") -> Path:
        """A generated test class per test data directory in [into] of the module, each with a test of `file.kt`."""
        classes = "".join(textwrap.indent(textwrap.dedent(f'''\
            @Nested
            @TestMetadata("{directory}")
            @TestDataPath("$PROJECT_ROOT")
            public static class Directory{index} {{
              @Test
              @TestMetadata("file.kt")
              public void testFile() {{
              }}
            }}
        '''), "  ") for index, directory in enumerate(directories))
        return self.write(
            f"{module_path}/{into}/org/example/FooTestGenerated.java",
            f'@SuppressWarnings("all")\npublic class FooTestGenerated {{\n{classes}}}\n',
        )

    def select(self, *paths: str, cwd: str = "") -> Tuple[List[str], List[str]]:
        return tdm.select_modules(self.root, ALL_MODULES, list(paths), self.root / cwd)

    def run_wrapper(self, stub: Dict[str, object], *args: str) -> Tuple[int, dict]:
        """Runs the wrapper with [args]; returns its exit code and the JSON summary, keeps its stderr in [stderr]."""
        self.write("stub-gradle.json", json.dumps(stub))
        with contextlib.redirect_stdout(io.StringIO()) as stdout, contextlib.redirect_stderr(io.StringIO()) as stderr:
            exit_code = tdm.main([*args, "--json"], root=self.root)
        self.stderr = stderr.getvalue()
        return exit_code, json.loads(stdout.getvalue())

    def run_text(self, stub: Dict[str, object], *args: str) -> Tuple[int, str]:
        """The text output, with the run id and the file paths replaced."""
        self.write("stub-gradle.json", json.dumps(stub))
        with contextlib.redirect_stdout(io.StringIO()) as stdout, contextlib.redirect_stderr(io.StringIO()):
            exit_code = tdm.main(list(args), root=self.root)
        output = re.sub(r"(Gradle log|Summary): .*", r"\1: <path>", stdout.getvalue())
        return exit_code, re.sub(r"runId \S+", "runId <id>", output)

    def gradle_args(self) -> List[str]:
        return (self.root / "build" / "stub-gradle-args.txt").read_text().split("\n")

    def test_runs_every_managed_module_with_the_options(self) -> None:
        self.write("mod-a/testData/file.kt", "")
        self.write("other/file.kt", "")
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

    def test_requested_modules_are_project_paths(self) -> None:
        stub = {":mod-a:nested": {}, ":mod-b": {}}
        exit_code, summary = self.run_wrapper(
            stub, "check", "--module", "mod-b", "--module", "mod-a/nested/", "--module", ":mod-b:",
        )
        self.assertEqual((0, "PASSED"), (exit_code, summary["status"]))
        self.assertEqual([":mod-b", ":mod-a:nested"], [module["project"] for module in summary["modules"]])
        self.assertEqual(["-q", "--continue", ":mod-b:checkTestData", ":mod-a:nested:checkTestData"],
                         self.gradle_args()[:4])

        with contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit) as exit:
            tdm.main(["check", "--module", ":"], root=self.root)
        self.assertEqual(2, exit.exception.code)

    def test_run_id_names_the_reports_and_the_log(self) -> None:
        exit_code, summary = self.run_wrapper({":mod-b": {}}, "check", "--module", ":mod-b", "--run-id", "my-run.1")
        self.assertEqual((0, "my-run.1"), (exit_code, summary["runId"]))
        self.assertEqual(self.root / "build" / "test-data-manager" / "my-run.1.gradle.log", Path(summary["gradleLog"]))
        self.assertEqual(self.root / "build" / "test-data-manager" / "my-run.1.summary.json", Path(summary["summary"]))
        self.assertEqual(
            str(self.root / "mod-b" / "build" / "test-data-manager" / "my-run.1.json"),
            summary["modules"][0]["report"],
        )

        for invalid in ["..", "../x", "a b", ""]:
            with contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit) as exit:
                tdm.main(["check", "--run-id", invalid], root=self.root)
            self.assertEqual(2, exit.exception.code, invalid)

    def test_the_log_is_announced_before_gradle_runs(self) -> None:
        _, summary = self.run_wrapper({":mod-a": {}, ":mod-b": {}}, "check", "--module", ":mod-a", "--module", ":mod-b")
        self.assertEqual(f"Running checkTestData in :mod-a, :mod-b; Gradle log: {summary['gradleLog']}\n", self.stderr)

    def test_gradle_failures_and_compiler_errors_are_reported(self) -> None:
        output = textwrap.dedent('''\
            e: file:///mod-b/src/Foo.kt:1:1 Unresolved reference 'x'.
            mod-b/src/Bar.java:3: error: cannot find symbol

            FAILURE: Build completed with 2 failures.

            1: Task failed with an exception.
            -----------
            * What went wrong:
            Execution failed for task ':mod-a:checkTestData' (registered by plugin 'test-data-manager').
            > Process 'command 'java'' finished with non-zero exit value 1

            * Try:
            > Run with --stacktrace option to get the stack trace.
            ==============================================================================

            2: Task failed with an exception.
            -----------
            * What went wrong:
            Execution failed for task ':mod-b:compileTestKotlin'.
            > A failure occurred while executing GradleKotlinCompilerWorkAction
               > Compilation error. See log for more details

            * Try:
            > Run with --stacktrace option to get the stack trace.
        ''')
        problem = {"kind": "MISMATCH", "file": "mod-a/testData/file.txt", "test": "FooTestGenerated.testFile"}
        stub = {":mod-a": {"report": {"status": "FAILED", "problems": [problem]}}, "output": output, "exitCode": 1}
        exit_code, summary = self.run_wrapper(stub, "check", "--module", ":mod-a", "--module", ":mod-b")
        self.assertEqual((2, "ERROR"), (exit_code, summary["status"]))
        self.assertEqual(
            ["Execution failed for task ':mod-b:compileTestKotlin'.\n"
             "> A failure occurred while executing GradleKotlinCompilerWorkAction\n"
             "   > Compilation error. See log for more details"],
            summary["gradleFailures"],
        )
        self.assertEqual(
            ["e: file:///mod-b/src/Foo.kt:1:1 Unresolved reference 'x'.", "mod-b/src/Bar.java:3: error: cannot find symbol"],
            summary["compilerErrors"],
        )

        exit_code, text = self.run_text(stub, "check", "--module", ":mod-a", "--module", ":mod-b")
        self.assertIn(textwrap.dedent('''\
            ERROR :mod-b: no report for this run; the task didn't run or its JVM crashed, see the Gradle log
            GRADLE FAILURE Execution failed for task ':mod-b:compileTestKotlin'.
              > A failure occurred while executing GradleKotlinCompilerWorkAction
                 > Compilation error. See log for more details
            e: file:///mod-b/src/Foo.kt:1:1 Unresolved reference 'x'.
            mod-b/src/Bar.java:3: error: cannot find symbol
            MISMATCH mod-a/testData/file.txt (FooTestGenerated.testFile)
        '''), text)

        # Only the failure of a task whose report failed: nothing to add
        only_report_failure = output.split("\n\n", 1)[1].split("2: ")[0]
        stub = {":mod-a": {"report": {"status": "FAILED", "problems": [problem]}}, "output": only_report_failure}
        exit_code, summary = self.run_wrapper(stub, "check", "--module", ":mod-a")
        self.assertEqual((1, [], []), (exit_code, summary["gradleFailures"], summary["compilerErrors"]))
        # Without the plugin suffix, as Gradle prints the task of a build script
        plain = only_report_failure.replace(" (registered by plugin 'test-data-manager').", ".")
        self.assertEqual([], tdm.gradle_failures(plain, {":mod-a"}, "checkTestData"))
        self.assertEqual(1, len(tdm.gradle_failures(plain, {":mod-a:nested"}, "checkTestData")))

    def test_long_gradle_failures_are_cut(self) -> None:
        failure = "\n".join(["* What went wrong:", "Could not resolve all files."] + [f"> line {n}" for n in range(30)])
        errors = "\n".join(f"e: file:///Foo.kt:{n}:1 Error" for n in range(25))
        failures = tdm.gradle_failures(f"{failure}\n\n* Try:\n", set(), "checkTestData")
        self.assertEqual(21, len(failures[0].split("\n")))
        self.assertEqual("... 11 more lines", failures[0].split("\n")[-1])
        self.assertEqual(["e: file:///Foo.kt:19:1 Error", "... 5 more compiler errors"], tdm.compiler_errors(errors)[19:])

    def test_windows_command_line(self) -> None:
        self.assertEqual("gradlew.bat", tdm.gradle_wrapper(self.root, windows=True).name)
        self.assertEqual("gradlew", tdm.gradle_wrapper(self.root, windows=False).name)
        self.assertEqual(
            r'C:\repo\gradlew.bat -q -Pp.testDataPath=a\b,c/ "-Pp.testClassPattern=.*(Foo|Bar).*" '
            r'"-Pp.testDataPath=C:\my dir\\" "say \"hi\"" "a\\\"b" "x&y" "^" ""',
            tdm.windows_command_line([
                r"C:\repo\gradlew.bat", "-q", r"-Pp.testDataPath=a\b,c/", "-Pp.testClassPattern=.*(Foo|Bar).*",
                "-Pp.testDataPath=C:\\my dir\\", 'say "hi"', 'a\\"b', "x&y", "^", "",
            ]),
        )

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

        # Run errors, one per path that doesn't exist; the summary keeps them all
        paths = ",".join(f"nope/{index:02}" for index in range(60))
        _, summary = self.run_wrapper({}, "check", "--path", paths)
        self.assertEqual(60, len(summary["runErrors"]))
        _, output = self.run_text({}, "check", "--path", paths)
        lines = output.splitlines()
        self.assertEqual(50, sum(1 for line in lines if line.startswith("ERROR")))
        self.assertEqual(
            [f"ERROR testDataPath 'nope/49' does not exist; paths are relative to the repository root {self.root}",
             "... and 10 more, see the summary"],
            lines[-3:-1],
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
        invalid = {"report": {"status": "ERROR", "runErrors": ["Variant chain conflicts"]}}
        exit_code, summary = self.run_wrapper({module: invalid for module in ALL_MODULES}, "check")
        self.assertEqual(2, exit_code)
        self.assertEqual([f"{module}: Variant chain conflicts" for module in ALL_MODULES], summary["runErrors"])

    def test_paths_select_the_modules_with_tests_under_them(self) -> None:
        self.generated_tests("mod-a", "mod-a/testData", "mod-a/testData/nested")
        self.generated_tests("mod-a/nested", "shared/testData/name.ext")
        self.generated_tests("mod-b", "shared/testData/b")
        for file in ["mod-a/testData/nested/file.kt", "shared/testData/name.ext/file.kt", "shared/testData/b/file.kt"]:
            self.write(file, "")

        self.assertEqual(([":mod-a:nested", ":mod-b"], []), self.select("shared/testData"))
        self.assertEqual(([":mod-b"], []), self.select("shared/testData/b/file.kt"))
        self.assertEqual(([":mod-b"], []), self.select("shared/testData/b/file"))
        self.assertEqual(([":mod-a:nested"], []), self.select("shared/testData/name"))
        self.assertEqual(([":mod-a"], []), self.select("mod-a/testData/nested/"))
        self.assertEqual(([":mod-a", ":mod-b"], []), self.select("mod-a/testData/nested,shared/testData/b"))
        self.assertEqual((ALL_MODULES, []), self.select(str(self.root)))

        exit_code, summary = self.run_wrapper({":mod-b": {}}, "check", "--path", "shared/testData/b")
        self.assertEqual((0, "PASSED"), (exit_code, summary["status"]))
        self.assertEqual(
            [(":mod-b", "PASSED"), (":mod-a", "SKIPPED"), (":mod-a:nested", "SKIPPED")],
            [(module["project"], module["status"]) for module in summary["modules"]],
        )
        self.assertEqual(["-q", "--continue", ":mod-b:checkTestData"], self.gradle_args()[:3])

        _, text = self.run_text({":mod-b": {}}, "check", "--path", "shared/testData/b")
        self.assertIn("\nSkipped 2 modules without tests under the paths, see the summary\n", text)

        # Explicit modules run as requested
        exit_code, summary = self.run_wrapper({":mod-a": {}}, "check", "--path", "shared/testData/b", "--module", ":mod-a")
        self.assertEqual((0, [":mod-a"]), (exit_code, [module["project"] for module in summary["modules"]]))

    def test_paths_no_module_has_tests_of_fail_the_run(self) -> None:
        self.generated_tests("mod-a", "mod-a/testData")
        self.generated_tests("mod-a/nested", "mod-a/testData")
        self.generated_tests("mod-b", "mod-b/testData")
        for file in ["mod-a/testData/file.kt", "mod-b/testData/file.kt", "shared/testData/file.kt", "file.kt"]:
            self.write(file, "")

        # `file.kt` is the file of a test method, not a test data directory
        self.assertEqual(([], ["testDataPath 'file.kt' matches no managed test in any module"]), self.select("file.kt"))

        exit_code, summary = self.run_wrapper({":mod-b": {}}, "check", "--path", "mod-b/testData", "--path", "shared/")
        self.assertEqual((2, ["testDataPath 'shared/' matches no managed test in any module"]),
                         (exit_code, summary["runErrors"]))
        self.assertEqual(["-q", "--continue", ":mod-b:checkTestData"], self.gradle_args()[:3])

        (self.root / "build" / "stub-gradle-args.txt").unlink()
        exit_code, summary = self.run_wrapper({}, "check", "--path", "shared/testData")
        self.assertEqual((2, ["testDataPath 'shared/testData' matches no managed test in any module"], None),
                         (exit_code, summary["runErrors"], summary["gradleLog"]))
        self.assertEqual([], summary["modules"])
        self.assertFalse((self.root / "build" / "stub-gradle-args.txt").exists())

    def test_paths_that_do_not_exist_fail_before_gradle(self) -> None:
        self.write("mod-a/testData/file.kt", "")
        exit_code, summary = self.run_wrapper({}, "check", "--path", "nope", "--path", "mod-a/testData/file")
        self.assertEqual(
            (2, [f"testDataPath 'nope' does not exist; paths are relative to the repository root {self.root}"], None),
            (exit_code, summary["runErrors"], summary["gradleLog"]),
        )
        self.assertEqual([], summary["modules"])
        self.assertNotIn("Skipped", self.run_text({}, "check", "--path", "nope")[1])
        self.assertFalse((self.root / "build" / "stub-gradle-args.txt").exists())

        self.assertEqual(
            ([], [f"testDataPath 'testData' does not exist; paths are relative to the repository root {self.root}. "
                  "Did you mean 'mod-a/testData'?"]),
            self.select("testData", cwd="mod-a"),
        )
        # Relative to a module, as the runner suggests for its own module
        self.write("mod-b/testData/other.kt", "")
        self.assertEqual(
            ([], [f"testDataPath 'testData/other.kt' does not exist; paths are relative to the repository root {self.root}. "
                  "Did you mean 'mod-b/testData/other.kt'?"]),
            self.select("testData/other.kt"),
        )
        self.assertEqual(
            ([], [f"testDataPath 'testData/' does not exist; paths are relative to the repository root {self.root}. "
                  "Did you mean 'mod-a/testData' or 'mod-b/testData'?"]),
            self.select("testData/"),
        )
        self.assertEqual(([], [f"testDataPath '../x' is outside the repository root {self.root}"]), self.select("../x"))

    def test_modules_without_generated_tests_may_have_any_test_data(self) -> None:
        self.generated_tests("mod-a", "mod-a/testData")
        self.write("mod-b/testData/file.kt", "")
        self.assertEqual(([":mod-a:nested", ":mod-b"], []), self.select("mod-b/testData"))

    def test_tests_generated_into_build_are_used_while_newer_than_the_sources(self) -> None:
        self.generated_tests("mod-a", "mod-a/testData")
        self.generated_tests("mod-a/nested", "mod-a/testData")
        self.write("mod-a/testData/file.kt", "")
        self.write("mod-b/build.gradle.kts", 'testGenerator("Generator", generateTestsInBuildDirectory = true)\n')
        self.write("mod-b/testData/file.kt", "")
        generator = self.write("mod-b/testFixtures/Generator.kt", "")
        git(self.root, "add", "-A")

        # Not generated yet
        self.assertEqual(([":mod-a", ":mod-a:nested", ":mod-b"], []), self.select("mod-a/testData"))

        generated = self.generated_tests("mod-b", "mod-b/testData", into="build/tests-gen")
        later = time.time() + 100
        os.utime(generated, (later, later))
        self.assertEqual(([":mod-a", ":mod-a:nested"], []), self.select("mod-a/testData"))
        self.assertEqual(([":mod-b"], []), self.select("mod-b/testData"))

        # Test data doesn't change what the generator writes for known directories
        os.utime(self.root / "mod-b/testData/file.kt", (later + 1, later + 1))
        self.assertEqual(([":mod-a", ":mod-a:nested"], []), self.select("mod-a/testData"))

        # The generator may have changed
        os.utime(generator, (later + 1, later + 1))
        self.assertEqual(([":mod-a", ":mod-a:nested", ":mod-b"], []), self.select("mod-a/testData"))

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
        self.assertTrue(log.startswith(f"$ {tdm.gradle_wrapper(self.root)} -q --continue :mod-b:checkTestData"))
        self.assertIn("Test data manager report: ", log)



if __name__ == "__main__":
    unittest.main()
