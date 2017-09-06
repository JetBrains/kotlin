@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.*
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.kotlin.dsl.*
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.extra

fun Project.projectTest(body: Test.() -> Unit = {}): Test = (tasks.findByName("test") as Test).apply {
    doFirst {
        val patterns = filter.includePatterns + ((filter as? DefaultTestFilter)?.commandLineIncludePatterns ?: emptySet())
        if (patterns.isEmpty() || patterns.any { '*' in it }) return@doFirst
        patterns.forEach { pattern ->
            val maybeMethodName = pattern.substringAfterLast('.')
            val className = if (maybeMethodName.isNotEmpty() && maybeMethodName[0].isLowerCase())
                pattern.substringBeforeLast('.')
            else
                pattern

            val matchPattern = className.replace('.', '/') + ".class"

            include {
                if (it.isDirectory) {
                    matchPattern.startsWith(it.path)
                } else {
                    it.path.endsWith(matchPattern)
                }
            }
        }
    }
    jvmArgs("-ea", "-XX:+HeapDumpOnOutOfMemoryError", "-Xmx1200m", "-XX:+UseCodeCacheFlushing", "-XX:ReservedCodeCacheSize=128m", "-Djna.nosys=true")
    maxHeapSize = "1200m"
    systemProperty("idea.is.unit.test", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
    environment("KOTLIN_HOME", rootProject.extra["distKotlinHomeDir"])
    systemProperty("jps.kotlin.home", rootProject.extra["distKotlinHomeDir"])
    ignoreFailures = System.getenv("kotlin_build_ignore_test_failures")?.let { it == "yes" } ?: false
    body()
}

inline fun <reified T : Task> Project.getOrCreateTask(taskName: String, body: T.() -> Unit): T =
        (tasks.findByName(taskName)?.let { it as T } ?: task<T>(taskName)).apply { body() }
