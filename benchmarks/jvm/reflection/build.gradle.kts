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

        // The reflection benchmarks are single-shot and select the reflection implementation through a `implementation` param
        named("main") {

            // Examples: `KClassMembersHierarchyBenchmark\.kotlinHierarchy`, `KClassMembersHierarchyBenchmark`, `java`
            providers.gradleProperty("include").orNull?.let { include(it) }

            providers.gradleProperty("implementation").orNull?.let {
                if (it != "new" && it != "k1") error("Unknown implementation '$it': expected 'new' or 'k1'")
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
    addJarPathProperty(TestCompilePaths.KOTLIN_FULL_STDLIB_PATH, project.dependencies.project(":kotlin-stdlib"))
    addJarPathProperty(TestCompilePaths.KOTLIN_REFLECT_JAR_PATH, project.dependencies.project(":kotlin-reflect"))
    // The benchmarks never touch these two, but `getOrCreateClassLoader` resolves all four jars
    // unconditionally and fails on a missing property. They are built for this module anyway.
    addJarPathProperty(TestCompilePaths.KOTLIN_SCRIPT_RUNTIME_PATH, project.dependencies.project(":kotlin-script-runtime"))
    addJarPathProperty(TestCompilePaths.KOTLIN_TEST_JAR_PATH, project.dependencies.project(":kotlin-test"))
}
