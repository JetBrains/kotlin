import com.osacky.doctor.DoctorExtension

plugins {
    id("com.osacky.doctor")
}

configure<DoctorExtension> {
    /**
     * Throw an exception when multiple Gradle Daemons are running.
     *
     * Windows is not supported yet, see https://github.com/runningcode/gradle-doctor/issues/84
     */
    disallowMultipleDaemons.set(false)
    /**
     * Show a message if the download speed is less than this many megabytes / sec.
     */
    downloadSpeedWarningThreshold.set(.5f)
    /**
     * The level at which to warn when a build spends more than this percent garbage collecting.
     */
    GCWarningThreshold.set(0.10f)
    /**
     * The level at which to fail when a build spends more than this percent garbage collecting.
     */
    GCFailThreshold.set(0.9f)
    /**
     * By default, Gradle caches test results. This can be dangerous if tests rely on timestamps, dates, or other files
     * which are not declared as inputs.
     */
    enableTestCaching.set(true)
    /**
     * By default, Gradle treats empty directories as inputs to compilation tasks. This can cause cache misses.
     */
    failOnEmptyDirectories.set(true)
    /**
     * Negative Avoidance Savings Threshold
     * By default the Gradle Doctor will print out a warning when a task is slower to pull from the cache than to
     * re-execute. There is some variance in the amount of time a task can take when several tasks are running
     * concurrently. In order to account for this there is a threshold you can set. When the difference is above the
     * threshold, a warning is displayed.
     */
    negativeAvoidanceThreshold.set(500)
    /**
     * Warn when not using parallel GC. Parallel GC is faster for build type tasks and is no longer the default in Java 9+.
     */
    warnWhenNotUsingParallelGC.set(true)
    /**
     * Throws an error when the `Delete` or `clean` task has dependencies.
     * If a clean task depends on other tasks, clean can be reordered and made to run after the tasks that would produce
     * output. This can lead to build failures or just strangeness with seemingly straightforward builds
     * (e.g., gradle clean build).
     * http://github.com/gradle/gradle/issues/2488
     */
    disallowCleanTaskDependencies.set(true)
    /**
     * Warn if using the Kotlin Compiler Daemon Fallback. The fallback is incredibly slow and should be avoided.
     * https://youtrack.jetbrains.com/issue/KT-48843
     */
    warnIfKotlinCompileDaemonFallback.set(true)

    /** Configuration properties relating to JAVA_HOME */
    javaHome {
        /**
         * Ensure that we are using JAVA_HOME to build with this Gradle.
         */
        ensureJavaHomeMatches.set(true)
        /**
         * Ensure we have JAVA_HOME set.
         */
        ensureJavaHomeIsSet.set(true)
        /**
         * Fail on any `JAVA_HOME` issues.
         */
        failOnError.set(true)
        /**
         * Extra message text, if any, to show with the Gradle Doctor message. This is useful if you have a wiki page or
         * other instructions that you want to link for developers on your team if they encounter an issue.
         */
        extraMessage.set("Here's an extra message to show.")
    }
}