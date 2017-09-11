@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.*
import org.gradle.kotlin.dsl.*
import org.gradle.api.tasks.testing.Test

fun Project.projectTest(body: Test.() -> Unit = {}): Test = getOrCreateTask("test") {
    jvmArgs("-ea", "-XX:+HeapDumpOnOutOfMemoryError", "-Xmx1200m", "-XX:+UseCodeCacheFlushing", "-XX:ReservedCodeCacheSize=128m", "-Djna.nosys=true")
    maxHeapSize = "1200m"
    systemProperty("idea.is.unit.test", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
    environment("KOTLIN_HOME", rootProject.extra["distKotlinHomeDir"])
    systemProperty("jps.kotlin.home", rootProject.extra["distKotlinHomeDir"])
    ignoreFailures = System.getenv("kotlin_build_ignore_test_failures")?.let { it == "yes" } ?: false
    body()
}

inline fun<reified T: Task> Project.getOrCreateTask(taskName: String, body: T.() -> Unit): T =
        (tasks.findByName(taskName)?.let { it as T } ?: task<T>(taskName)).apply{ body() }
