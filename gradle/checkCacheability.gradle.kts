import org.gradle.api.internal.GeneratedSubclasses
import org.gradle.api.internal.TaskInternal

val checkCacheability = findProperty("kotlin.build.cache.check.enabled") as String? == "true"
val isTeamcityBuild = project.hasProperty("teamcity") || System.getenv("TEAMCITY_VERSION") != null

if (checkCacheability && buildCacheEnabled()) {
    @Suppress("DEPRECATION")
    gradle.taskGraph.afterTask {
        if (isCacheable()) {
            if (isTeamcityBuild)
                testStarted(path)

            if (!state.skipped)
                reportCacheMiss()

            if (isTeamcityBuild)
                testFinished(path)
        }
    }
}

fun Task.reportCacheMiss() {
    if (isTeamcityBuild)
        testFailed(path, "Build cache MISS", "$path task outputs expected to be taken from Gradle build cache")
    else
        println("BUILD CACHE MISS - $path")
}

fun Project.buildCacheEnabled() = gradle.startParameter.isBuildCacheEnabled

fun Task.isCacheable(): Boolean {
    this as TaskInternal
    return cachingEnabled() && !cachingDisabled()
}

fun TaskInternal.cachingEnabled(): Boolean {
    return if (outputs.cacheIfSpecs.isEmpty())
        GeneratedSubclasses.unpackType(this).isAnnotationPresent(CacheableTask::class.java)
    else
        outputs.cacheIfSpecs.all { it.invoke(this) }
}

fun TaskInternal.cachingDisabled(): Boolean = outputs.doNotCacheIfSpecs.any { it.invoke(this) }

fun escape(s: String): String {
    return s.replace("[\\|'\\[\\]]".toRegex(), "\\|$0").replace("\n".toRegex(), "|n").replace("\r".toRegex(), "|r")
}

fun testStarted(testName: String) {
    println("##teamcity[testStarted name='%s']".format(escape(testName)))
}

fun testFinished(testName: String) {
    println("##teamcity[testFinished name='%s']".format(escape(testName)))
}

fun testFailed(name: String, message: String, details: String) {
    println("##teamcity[testFailed name='%s' message='%s' details='%s']".format(escape(name), escape(message), escape(details)))
}