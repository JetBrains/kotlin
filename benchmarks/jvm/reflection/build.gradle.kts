import kotlinx.benchmark.gradle.benchmark

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    alias(libs.plugins.kotlinx.benchmark)
}

dependencies {
    testImplementation(kotlinStdlib())
    testImplementation(testFixtures(project(":compiler:tests-integration")))
    testImplementation(libs.kotlinx.benchmark.runtime)
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

benchmark {
    configurations {
        // The plugin pre-registers `main`, which would give a `testBenchmark` task that runs every
        // benchmark in the module under JMH settings that do not suit them.
        // Drop it and register each suite explicitly, so every benchmark belongs to exactly one
        // task and neither suite needs to exclude the other.
        remove(getByName("main"))

        // The reflection benchmarks carry their own JMH settings: they are single-shot and select the
        // reflection implementation through a `reflectImplementation` param
        register("reflection") {

            // `reflectionInclude` is `<class>.<method>`, or `<class>.*` for all methods of the class.
            // Examples: `KClassMembersHierarchyBenchmark.kotlinHierarchy`, `KClassMembersHierarchyBenchmark.*`
            providers.gradleProperty("reflectionInclude").orNull?.let { value ->
                val (className, methodName) = Regex("""(\w+)\.(\w+|\*)""").matchEntire(value)?.destructured
                        ?: error("Invalid reflectionInclude '$value': expected '<class>.<method>' or '<class>.*'")
                val methodPattern = if (methodName == "*") ".*" else methodName
                include("^org\\.jetbrains\\.kotlin\\.benchmarks\\.jvm\\.reflection\\.$className\\.$methodPattern$")
            }

            providers.gradleProperty("reflectionImplementation").orNull?.let {
                if (it != "new" || it != "k1") error("Unknown reflectionImplementation '$it': expected 'new' or 'k1'")
                param("reflectImplementation", it)
            }

            iterationTime = 1 // Required param
            iterationTimeUnit = "sec" // Required param

            warmups = 0
            iterations = 1
            advanced("jvmForks", 30)
        }
    }
    targets {
        register("test")
    }
}

// The reflection benchmarks measure kotlin-reflect built from this working tree. They get it the same
// way the FE compiler tests do: as jar paths, which `StandardLibrariesPathProviderForKotlinProject` loads
// into a dedicated class loader that also selects the K1 or the new implementation. Nothing of
// kotlin-reflect goes on the benchmark's own classpath.
tasks.withType<JavaExec>().configureEach {
    addJarPathProperty(TestCompilePaths.KOTLIN_FULL_STDLIB_PATH, ":kotlin-stdlib")
    addJarPathProperty(TestCompilePaths.KOTLIN_REFLECT_JAR_PATH, ":kotlin-reflect")
    // The benchmarks never touch these two, but `getOrCreateClassLoader` resolves all four jars
    // unconditionally and fails on a missing property. They are built for this module anyway.
    addJarPathProperty(TestCompilePaths.KOTLIN_SCRIPT_RUNTIME_PATH, ":kotlin-script-runtime")
    addJarPathProperty(TestCompilePaths.KOTLIN_TEST_JAR_PATH, ":kotlin-test")
}
