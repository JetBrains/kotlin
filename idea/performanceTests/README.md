# Goal

The main goal of these performance tests is to collect statistics over
routines (open project, highlighting, inspection, autocompletion etc) for
further analysis like anomalies, degradations and reference for optimizations.

## Resources
- Clone another copy of kotlin project to `${KOTLIN_PROJECT}/../perfTestProject`
- `${KOTLIN_PROJECT}/idea/testData/perfTest/helloKotlin` as _hello world_ project in Kotlin

## Performance test types

### AbstractKotlinProjectsPerformanceTest

Subclasses of `AbstractKotlinProjectsPerformanceTest` provide statistics
(execution time in **ms**) over some **particular** (semi-synthetic) actions.

The output is provided to console in TeamCity format like

```
##teamcity[testSuiteStarted name='Kotlin']
##teamcity[testStarted name='Project opening perfTestProject' captureStandardOutput='true']
##teamcity[testMetadata key='Project opening perfTestProject' type='number' value='63']
##teamcity[testFinished name='Project opening perfTestProject' duration='63']
##teamcity[testSuiteFinished name='Kotlin']
```

as well in `build/stats.csv`

 File          | ProcessID                              | Time
--- | --- | ---
  HelloMain.kt | Highlighting file warm-up HelloMain.kt | 114
  KtFile.kt    | Highlighting file KtFile.kt            | 1255

### WholeProjectPerformanceTest

**TBD**: drop `@Ignore` from some test cases

Subclasses of `WholeProjectPerformanceTest` collect performance metrics to
figure out some significant anomalies (e.g. inspection takes ages or
even **fails**) over huge number of files (like `kotlin` project itself)
on various **real** use cases.
