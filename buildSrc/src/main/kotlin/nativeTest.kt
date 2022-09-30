import org.gradle.api.GradleException
import org.gradle.api.tasks.testing.Test
import org.gradle.api.Project
import org.gradle.kotlin.dsl.project
import org.gradle.api.artifacts.Configuration

private enum class TestProperty(shortName: String) {
    // Use a separate Gradle property to pass Kotlin/Native home to tests: "kotlin.internal.native.test.nativeHome".
    // Don't use "kotlin.native.home" and similar properties for this purpose, as these properties may have undesired
    // effect on other Gradle tasks (ex: :kotlin-native:dist) that might be executed along with test task.
    KOTLIN_NATIVE_HOME("nativeHome"),
    COMPILER_CLASSPATH("compilerClasspath"),
    TEST_TARGET("target"),
    TEST_MODE("mode"),
    FORCE_STANDALONE("forceStandalone"),
    COMPILE_ONLY("compileOnly"),
    OPTIMIZATION_MODE("optimizationMode"),
    MEMORY_MODEL("memoryModel"),
    USE_THREAD_STATE_CHECKER("useThreadStateChecker"),
    GC_TYPE("gcType"),
    GC_SCHEDULER("gcScheduler"),
    CACHE_MODE("cacheMode"),
    EXECUTION_TIMEOUT("executionTimeout"),
    SANITIZER("sanitizer");

    private val propertyName = "kotlin.internal.native.test.$shortName"

    fun setUpFromGradleProperty(task: Test, defaultValue: () -> String? = { null }) {
        val propertyValue = readGradleProperty(task) ?: defaultValue()
        if (propertyValue != null) task.systemProperty(propertyName, propertyValue)
    }

    fun readGradleProperty(task: Test): String? = task.project.findProperty(propertyName)?.toString()
}

fun Project.nativeTest(taskName: String, tag: String?, vararg customDependencies: Configuration) = projectTest(
    taskName,
    jUnitMode = JUnitMode.JUnit5,
    maxHeapSizeMb = 3072 // Extra heap space for Kotlin/Native compiler.
) {
    group = "verification"

    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        workingDir = rootDir
        outputs.upToDateWhen {
            // Don't treat any test task as up-to-date, no matter what.
            // Note: this project should contain only test tasks, including ones that build binaries, and ones that run binaries.
            false
        }

        // Effectively remove the limit for the amount of stack trace elements in Throwable.
        jvmArgs("-XX:MaxJavaStackTraceDepth=1000000")

        // Double the stack size. This is needed to compile some marginal tests with extra-deep IR tree, which requires a lot of stack frames
        // for visiting it. Example: codegen/box/strings/concatDynamicWithConstants.kt
        // Such tests are successfully compiled in old test infra with the default 1 MB stack just by accident. New test infra requires ~55
        // additional stack frames more compared to the old one because of another launcher, etc. and it turns out this is not enough.
        jvmArgs("-Xss2m")

        val availableCpuCores: Int = Runtime.getRuntime().availableProcessors()
        if (!kotlinBuildProperties.isTeamcityBuild
            && minOf(kotlinBuildProperties.junit5NumberOfThreadsForParallelExecution ?: 16, availableCpuCores) > 4
        ) {
            logger.info("$path JIT C2 compiler has been disabled")
            jvmArgs("-XX:TieredStopAtLevel=1") // Disable C2 if there are more than 4 CPUs at the host machine.
        }

        TestProperty.KOTLIN_NATIVE_HOME.setUpFromGradleProperty(this) {
            val testTarget = TestProperty.TEST_TARGET.readGradleProperty(this)
            dependsOn(if (testTarget != null) ":kotlin-native:${testTarget}CrossDist" else ":kotlin-native:dist")
            project(":kotlin-native").projectDir.resolve("dist").absolutePath
        }

        TestProperty.COMPILER_CLASSPATH.setUpFromGradleProperty(this) {
            buildList {
                val customNativeHome = TestProperty.KOTLIN_NATIVE_HOME.readGradleProperty(this@projectTest)
                if (customNativeHome != null) {
                    this += file(customNativeHome).resolve("konan/lib/kotlin-native-compiler-embeddable.jar")
                } else {
                    val kotlinNativeCompilerEmbeddable = configurations.detachedConfiguration(dependencies.project(":kotlin-native-compiler-embeddable"))
                    dependsOn(kotlinNativeCompilerEmbeddable)
                    this.addAll(kotlinNativeCompilerEmbeddable.files)
                }
                customDependencies.forEach { dependency ->
                    dependsOn(dependency)
                    this.addAll(dependency.files)
                }
            }.map { it.absoluteFile }.joinToString(";")
        }

        // Pass Gradle properties as JVM properties so test process can read them.
        TestProperty.TEST_TARGET.setUpFromGradleProperty(this)
        TestProperty.TEST_MODE.setUpFromGradleProperty(this)
        TestProperty.FORCE_STANDALONE.setUpFromGradleProperty(this)
        TestProperty.COMPILE_ONLY.setUpFromGradleProperty(this)
        TestProperty.OPTIMIZATION_MODE.setUpFromGradleProperty(this)
        TestProperty.MEMORY_MODEL.setUpFromGradleProperty(this)
        TestProperty.USE_THREAD_STATE_CHECKER.setUpFromGradleProperty(this)
        TestProperty.GC_TYPE.setUpFromGradleProperty(this)
        TestProperty.GC_SCHEDULER.setUpFromGradleProperty(this)
        TestProperty.CACHE_MODE.setUpFromGradleProperty(this)
        TestProperty.EXECUTION_TIMEOUT.setUpFromGradleProperty(this)
        TestProperty.SANITIZER.setUpFromGradleProperty(this)

        // Pass the current Gradle task name so test can use it in logging.
        environment("GRADLE_TASK_NAME", path)

        tag?.let {
            useJUnitPlatform {
                includeTags(it)
            }
        }

        doFirst {
            logger.info(
                buildString {
                    appendLine("$path parallel test execution parameters:")
                    append("  Available CPU cores = $availableCpuCores")
                    systemProperties.filterKeys { it.startsWith("junit.jupiter") }.toSortedMap().forEach { (key, value) ->
                        append("\n  $key = $value")
                    }
                }
            )
        }
    } else
        doFirst {
            throw GradleException(
                """
                    Can't run task $path. The Kotlin/Native part of the project is currently disabled.
                    Make sure that "kotlin.native.enabled" is set to "true" in local.properties file, or is passed
                    as a Gradle command-line parameter via "-Pkotlin.native.enabled=true".
                """.trimIndent()
            )
        }
}
