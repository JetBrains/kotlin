# Goal

The main goal of Kotlin IDE plugin performance tests is to collect statistics over
routines (open project, highlighting, inspection, autocompletion etc) for
further analysis like anomalies, degradations and reference for optimizations.

## Resources

You need an extra copy of kotlin project located at `${KOTLIN_PROJECT}/../perfTestProject`

## Run 

Run all Kotlin IDE plugin performance tests with 

`$ gradle idea-plugin-performance-tests`

## Run with profiler

`YOURKIT_PROFILER_HOME=/Applications/YourKit-Java-Profiler-2019.8.app ./gradlew -Pkotlin.test.instrumentation.disable=true  :idea:performanceTests:performanceTest --tests "<test-filter>"`

## Performance test

Output is provided to console in TeamCity format like

```
##teamcity[testSuiteStarted name='Field']
##teamcity[testStarted name='highlight: Field warm-up #0' captureStandardOutput='true']
##teamcity[buildStatisticValue key='highlight: Field warm-up #0' value='142']
##teamcity[testFinished name='highlight: Field warm-up #0' duration='142']
....
##teamcity[testStarted name='highlight: Field #0' captureStandardOutput='true']
##teamcity[buildStatisticValue key='highlight: Field #0 counter "Call resolve": time' value='8']
##teamcity[buildStatisticValue key='highlight: Field #0' value='67']
##teamcity[testFinished name='highlight: Field #0' duration='67']
....
##teamcity[testStarted name='highlight: Field #19' captureStandardOutput='true']
##teamcity[buildStatisticValue key='highlight: Field #19' value='56']
##teamcity[testFinished name='highlight: Field #19' duration='56']

##teamcity[testStarted name='highlight: Field : mean' captureStandardOutput='true']
##teamcity[buildStatisticValue key='highlight: Field' value='57']
##teamcity[testFinished name='highlight: Field : mean' duration='57']

##teamcity[testStarted name='highlight: Field : stdDev' captureStandardOutput='true']
##teamcity[buildStatisticValue key='highlight: Field stdDev' value='0']
##teamcity[testFinished name='highlight: Field : stdDev' duration='0']

##teamcity[testStarted name='highlight: Field : geomMean' captureStandardOutput='true']
##teamcity[buildStatisticValue key='highlight: Field geomMean' value='57']
##teamcity[testFinished name='highlight: Field : geomMean' duration='57']

##teamcity[testStarted name='highlight: Field stability' captureStandardOutput='true']
##teamcity[buildStatisticValue key='highlight: Field stability' value='2']
##teamcity[testFinished name='highlight: Field stability']

##teamcity[testSuiteFinished name='Field']
```

as well in CVS files at `build/stats*.csv`, e.g. `build/stats-highlight.csv`:

 Name          | ValueMS                              | StdDev
--- | --- | ---
  highlight: NonNullAssertion | 56.1 | 1.86
  highlight: PropertiesWithPropertyDeclarations | 308.1 | 11.05
  highlight: NamedArguments | 67.15 | 1.23
  highlight: Annotations | 84.85 | 1.18
  highlight: VariablesAsFunctions | 125.5 | 2.11
  highlight: Enums | 84.4 | 1.31
  highlight: Field | 67.8 | 1.74
  

## PerfTest DSL

TBD