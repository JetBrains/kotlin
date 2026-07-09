plugins {
    id("common-configuration")
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

description = "Prototype: merge/extract klibs into a single zstd-compressed artifact (KT-87204)"

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    api("org.jetbrains.kotlin:kotlin-stdlib:$coreDepsVersion")

    implementation(project(":kotlin-util-klib"))
    implementation(libs.zstd.jni)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

configureKotlinCompileTasksGradleCompatibility()

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5)
}

// Measurement-only: forward `-Dcoroutines.klibs.dir=<dir>` to the test JVM so
// KlibMergerCoroutinesMeasurementTest can point at a directory of real .klib files.
// The measurement is skipped (JUnit assumption) when the property is absent.
val coroutinesKlibsDir = providers.systemProperty("coroutines.klibs.dir")
tasks.withType<Test>().configureEach {
    doFirst {
        coroutinesKlibsDir.orNull?.let { systemProperty("coroutines.klibs.dir", it) }
    }
}
