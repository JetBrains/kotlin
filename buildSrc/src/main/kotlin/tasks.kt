@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.task
import org.gradle.kotlin.dsl.the
import java.lang.Character.isLowerCase
import java.lang.Character.isUpperCase

fun Project.projectTest(taskName: String = "test", body: Test.() -> Unit = {}): Test = getOrCreateTask(taskName) {
    doFirst {
        val patterns = filter.includePatterns + ((filter as? DefaultTestFilter)?.commandLineIncludePatterns ?: emptySet())
        if (patterns.isEmpty() || patterns.any { '*' in it }) return@doFirst
        patterns.forEach { pattern ->
            val maybeMethodName = pattern.substringAfterLast('.')
            val maybeClassFqName = if (maybeMethodName.isFirstChar(::isLowerCase))
                pattern.substringBeforeLast('.')
            else
                pattern

            if (!maybeClassFqName.substringAfterLast('.').isFirstChar(::isUpperCase)) {
                return@forEach
            }

            val classFileNameWithoutExtension = maybeClassFqName.replace('.', '/')
            val classFileName = classFileNameWithoutExtension + ".class"

            include {
                val path = it.path
                if (it.isDirectory) {
                    classFileNameWithoutExtension.startsWith(path)
                } else {
                    path == classFileName || (path.endsWith(".class") && path.startsWith(classFileNameWithoutExtension + "$"))
                }
            }
        }
    }

    doFirst {
        val agent = tasks.findByPath(":test-instrumenter:jar")!!.outputs.files.singleFile

        val args = project.findProperty("kotlin.test.instrumentation.args")?.let { "=$it" }.orEmpty()

        jvmArgs("-javaagent:$agent$args")
    }

    dependsOn(":test-instrumenter:jar")

    jvmArgs("-ea", "-XX:+HeapDumpOnOutOfMemoryError", "-Xmx1100m", "-XX:+UseCodeCacheFlushing", "-XX:ReservedCodeCacheSize=128m", "-Djna.nosys=true")
    maxHeapSize = "1100m"
    systemProperty("idea.is.unit.test", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
    environment("PROJECT_CLASSES_DIRS", the<JavaPluginConvention>().sourceSets.getByName("test").output.classesDirs.asPath)
    environment("PROJECT_BUILD_DIR", buildDir)
    systemProperty("jps.kotlin.home", rootProject.extra["distKotlinHomeDir"])
    body()
}

private inline fun String.isFirstChar(f: (Char) -> Boolean) = isNotEmpty() && f(first())

inline fun <reified T : Task> Project.getOrCreateTask(taskName: String, body: T.() -> Unit): T =
        (tasks.findByName(taskName)?.let { it as T } ?: task<T>(taskName)).apply { body() }
