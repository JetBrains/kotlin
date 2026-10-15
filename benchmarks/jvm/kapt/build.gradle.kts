// Root build file for kapt-benchmark.
//
// This is a multi-module project. Each sub-project has its own build.gradle.kts.
// See settings.gradle.kts for the module list.
//
// To run a benchmark scenario, use gradle-profiler from the command line; see README.md.

allprojects {
    // The Kotlin artifacts come from a -SNAPSHOT in Maven Local, so a republished
    // local compiler build must be picked up without --refresh-dependencies.
    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(0, "seconds")
    }
}
