# Test coverage for the compiler frontend (FIR) area

This module aggregates [JaCoCo](https://www.jacoco.org/jacoco/) test coverage for the FIR frontend
(`org.jetbrains.kotlin.fir.*` classes) from the test tasks agreed on in
[KQA-3083](https://youtrack.jetbrains.com/issue/KQA-3083):

- `:compiler:test`
- `:compiler:tests-integration:test`
- `:compiler:fir:analysis-tests:test`
- `:compiler:fir:fir2ir:test`
- `:compiler:fir:raw-fir:psi2fir:test`
- `:compiler:fir:raw-fir:light-tree2fir:test`

Coverage collection is **disabled by default** and is a strict no-op for regular builds.
It is enabled with the Gradle property `kotlin.build.coverage.enabled` (also settable in
`local.properties`). The property must be passed to *every* coverage-related invocation —
including the report task — otherwise the test JVMs are not instrumented and the report is skipped.

The setup lives in two places only, so the coverage engine can be swapped without touching
the test modules themselves:

- `repo/gradle-build-conventions/test-coverage-convention` — applies JaCoCo to the producer
  projects when the property is set;
- this module — aggregates execution data into a report via Gradle's
  [`jacoco-report-aggregation`](https://docs.gradle.org/current/userguide/jacoco_report_aggregation_plugin.html)
  plugin (Project-Isolation-compatible: data flows through outgoing configurations, no project
  reaches into another project's build directory).

## How to generate the report

The report task triggers the required test tasks through Gradle's dependency graph — running the
report alone is enough:

```bash
./gradlew :compiler:test-coverage:firCoverageReport -Pkotlin.build.coverage.enabled=true
```

Outputs:

- HTML: `compiler/test-coverage/build/reports/jacoco/firCoverageReport/html/index.html`
- XML: `compiler/test-coverage/build/reports/jacoco/firCoverageReport/firCoverageReport.xml`

To collect coverage from a subset of the suites, exclude the unwanted test tasks with `-x`;
the report is built from whatever execution data exists:

```bash
./gradlew :compiler:test-coverage:firCoverageReport -Pkotlin.build.coverage.enabled=true \
  -x :compiler:test -x :compiler:tests-integration:test \
  -x :compiler:fir:analysis-tests:test -x :compiler:fir:fir2ir:test
# keeps only the raw-fir suites
```

Raw execution data lands in each producing project's `build/jacoco/test.exec`. Re-running a test
task with a different `--tests` filter overwrites its `.exec` file, so collect one scope per
invocation.

> Note: with `kotlin.build.coverage.enabled=true` the test tasks are configured with
> `ignoreFailures = true`, so failing tests don't abort the build and the report is still
> generated from the collected data. The failures are still printed in the test output.

## How to open the report

Open the HTML report in a browser, or in IntelliJ IDEA — double-click
`html/index.html`: IDEA shows coverage for loaded modules in the Coverage tool window and renders
per-file coverage in the Project view. (IDEA refuses to open reports located outside the project
directory.)

## Known limitations

- Only `org.jetbrains.kotlin.*` classes are instrumented (see `test-coverage-convention`).
  This is deliberate: instrumenting everything breaks reflection-sensitive tests — debugger
  stepping/local-variable tests fail because `com.sun.tools.jdi` reflects over JDWP constant
  classes and trips on JaCoCo's synthetic members, and some codegen tests reflect over the
  bytecode they generate and load. Widen the filter here and in the report module together
  if the report scope ever grows beyond `org.jetbrains.kotlin`.
- Only code running inside the test JVMs is instrumented; tests that spawn external processes
  (some `:compiler:tests-integration` scenarios) don't contribute the coverage of those processes.
- JaCoCo reports branch data on Kotlin bytecode constructs (inline functions, `when` mappings,
  suspend state machines) that can look noisier than Kotlin-aware engines — keep this in mind
  when reading branch counters.
