"""
Tests for summarize_benchmark.py.

Run with: python3 -m unittest scripts/test_summarize_benchmark.py
"""

import io
import os
import sys
import tempfile
import unittest

# Make summarize_benchmark importable when running tests from the project root.
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import summarize_benchmark as sb


class TestParseBenchmarkCsv(unittest.TestCase):
    """gradle-profiler's benchmark.csv has a specific shape:
    Row 0: scenario,<title-1>,<title-2>,...
    Row 1: version,<gradle-version>,<gradle-version>,...
    Row 2: tasks,<tasks>,<tasks>,...
    Row 3: value,total execution time,total execution time,...
    Row 4+: warm-up build #1,<ms>,<ms>,...
            warm-up build #2,<ms>,<ms>,...
            measured build #1,<ms>,<ms>,...
            measured build #2,<ms>,<ms>,...
            ...
    """

    def test_parse_with_two_scenarios_returns_dict_of_lists(self):
        csv_content = (
            "scenario,scenario-a,scenario-b\n"
            "version,Gradle 8.14.4,Gradle 8.14.4\n"
            "tasks,:task-a,:task-b\n"
            "value,total execution time,total execution time\n"
            "warm-up build #1,1000,2000\n"
            "warm-up build #2,1100,2100\n"
            "measured build #1,1050,2050\n"
            "measured build #2,1060,2060\n"
            "measured build #3,1055,2055\n"
        )
        result = sb.parse_benchmark_csv(io.StringIO(csv_content))
        self.assertEqual(set(result.keys()), {"scenario-a", "scenario-b"})
        # Only measured builds (not warm-ups) are returned.
        self.assertEqual(result["scenario-a"], [1050.0, 1060.0, 1055.0])
        self.assertEqual(result["scenario-b"], [2050.0, 2060.0, 2055.0])


class TestComputeStats(unittest.TestCase):

    def test_compute_stats_known_values(self):
        # 10 values of 990 + 10 values of 1010: mean=1000, stddev=sqrt(2000/19)≈10.260, stderr≈2.295
        # 95% CI half-width ≈ 1.96 × 2.295 ≈ 4.498
        values = [990.0] * 10 + [1010.0] * 10
        stats = sb.compute_stats(values)
        self.assertEqual(stats["n"], 20)
        self.assertAlmostEqual(stats["mean"], 1000.0, places=2)
        self.assertAlmostEqual(stats["stddev"], 10.260, places=2)
        self.assertAlmostEqual(stats["stderr"], 2.295, places=2)
        self.assertAlmostEqual(stats["ci95_low"], 1000.0 - 4.498, places=1)
        self.assertAlmostEqual(stats["ci95_high"], 1000.0 + 4.498, places=1)

    def test_compute_stats_single_value_handles_zero_stddev(self):
        values = [1000.0]
        stats = sb.compute_stats(values)
        self.assertEqual(stats["n"], 1)
        self.assertEqual(stats["mean"], 1000.0)
        # Stddev / stderr undefined for n=1; the function should return 0 (or nan, depending on policy).
        # We pick 0 so the table renders cleanly.
        self.assertEqual(stats["stddev"], 0.0)
        self.assertEqual(stats["stderr"], 0.0)


class TestRenderTable(unittest.TestCase):

    def test_render_table_with_two_scenarios(self):
        data = {
            "scenario-a": [1050.0, 1060.0, 1055.0],
            "scenario-b": [2050.0, 2060.0, 2055.0],
        }
        output = sb.render_table(data)
        self.assertIn("scenario-a", output)
        self.assertIn("scenario-b", output)
        self.assertIn("Scenario", output)
        self.assertIn("Mean", output)
        self.assertIn("95% CI", output)


class TestCombineCsvFiles(unittest.TestCase):
    """When gradle-profiler runs with --output-dir per scenario, each benchmark.csv
    contains one scenario. combine_csv_files merges those into one dict so a single
    table can summarize the whole suite.
    """

    def _write_csv(self, scenario_name: str, measured_values: list) -> str:
        rows = (
            f"scenario,{scenario_name}\n"
            f"version,Gradle 8.14.4\n"
            f"tasks,:some:task\n"
            f"value,total execution time\n"
        )
        for i, v in enumerate(measured_values, 1):
            rows += f"measured build #{i},{v}\n"
        fd, path = tempfile.mkstemp(suffix=".csv")
        with os.fdopen(fd, "w") as f:
            f.write(rows)
        return path

    def test_combines_two_single_scenario_csvs(self):
        path_a = self._write_csv("scenario-a", [1050.0, 1060.0, 1055.0])
        path_b = self._write_csv("scenario-b", [2050.0, 2060.0, 2055.0])
        try:
            result = sb.combine_csv_files([path_a, path_b])
            self.assertEqual(set(result.keys()), {"scenario-a", "scenario-b"})
            self.assertEqual(result["scenario-a"], [1050.0, 1060.0, 1055.0])
            self.assertEqual(result["scenario-b"], [2050.0, 2060.0, 2055.0])
        finally:
            os.unlink(path_a)
            os.unlink(path_b)

    def test_preserves_input_order(self):
        path_a = self._write_csv("alpha", [1.0])
        path_z = self._write_csv("zulu", [2.0])
        try:
            result = sb.combine_csv_files([path_z, path_a])
            self.assertEqual(list(result.keys()), ["zulu", "alpha"])
        finally:
            os.unlink(path_a)
            os.unlink(path_z)

    def test_duplicate_scenario_name_concatenates_values(self):
        path_1 = self._write_csv("scenario-a", [100.0, 110.0])
        path_2 = self._write_csv("scenario-a", [200.0, 210.0])
        try:
            result = sb.combine_csv_files([path_1, path_2])
            self.assertEqual(result["scenario-a"], [100.0, 110.0, 200.0, 210.0])
        finally:
            os.unlink(path_1)
            os.unlink(path_2)


if __name__ == "__main__":
    unittest.main()
