@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.*
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.kotlin.dsl.*
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.extra

fun Project.projectTest(taskName: String = "test", body: Test.() -> Unit = {}): Test = getOrCreateTask(taskName) {
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
    jvmArgs("-ea", "-XX:+HeapDumpOnOutOfMemoryError", "-Xmx1100m", "-XX:+UseCodeCacheFlushing", "-XX:ReservedCodeCacheSize=128m", "-Djna.nosys=true")
    maxHeapSize = "1100m"
    systemProperty("idea.is.unit.test", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
    environment("PROJECT_CLASSES_DIRS", the<JavaPluginConvention>().sourceSets.getByName("test").output.classesDirs.asPath)
    environment("PROJECT_BUILD_DIR", buildDir)
    systemProperty("jps.kotlin.home", rootProject.extra["distKotlinHomeDir"])
    ignoreFailures = System.getenv("kotlin_build_ignore_test_failures")?.let { it == "yes" } ?: false
    body()
}

inline fun <reified T : Task> Project.getOrCreateTask(taskName: String, body: T.() -> Unit): T =
        (tasks.findByName(taskName)?.let { it as T } ?: task<T>(taskName)).apply { body() }
