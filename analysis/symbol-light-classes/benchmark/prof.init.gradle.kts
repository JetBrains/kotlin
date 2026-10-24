// -Pprof.jfr.dir=<dir>    : write <dir>/<project>-<task>.jfr with deep stacks
// -Pprof.jfr.settings=<jfc> : JFR settings (default: profile)
// -Pprof.jfr.extra=<opts> : extra comma-separated -XX:StartFlightRecording options (e.g. event settings)
// -Pprof.jdk=17           : override the test launcher JDK
// -Pprof.out.dir=<dir>    : passed to the test JVM as the `prof.out.dir` system property
// -Pprof.sysprop.<k>=<v>  : not supported; use prof.jvmargs
// -Pprof.jvmargs=<a|b|c>  : extra JVM args separated by '|'
// -Pprof.prebuild=true    : run no tests, only build everything the test tasks need
allprojects {
    val jfrDir = providers.gradleProperty("prof.jfr.dir").orNull
    val jfrExtra = providers.gradleProperty("prof.jfr.extra").orNull
    val jfrSettings = providers.gradleProperty("prof.jfr.settings").orNull ?: "profile"
    val jdk = providers.gradleProperty("prof.jdk").orNull
    val outDir = providers.gradleProperty("prof.out.dir").orNull
    val extraJvmArgs = providers.gradleProperty("prof.jvmargs").orNull
    val prebuild = providers.gradleProperty("prof.prebuild").orNull != null
    tasks.withType<Test>().configureEach {
        // The dependencies of a task are built even if the task itself is skipped
        if (prebuild) onlyIf("prof.prebuild") { false }
        if (jdk != null) {
            val toolchains = project.extensions.getByType<JavaToolchainService>()
            javaLauncher.set(toolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(jdk.toInt())) })
        }
        if (outDir != null) systemProperty("prof.out.dir", outDir)
        if (extraJvmArgs != null) jvmArgs(extraJvmArgs.replace("{project}", project.name).split('|'))
        if (jfrDir != null) {
            val file = "$jfrDir/${project.name}-$name.jfr"
            val extra = if (jfrExtra != null) ",$jfrExtra" else ""
            jvmArgs(
                "-XX:FlightRecorderOptions:stackdepth=2048",
                "-XX:StartFlightRecording:settings=$jfrSettings,filename=$file,dumponexit=true,maxsize=20g$extra",
            )
        }
    }
}
