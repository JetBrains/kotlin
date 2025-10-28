plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("d8-configuration")
    id("java-test-fixtures")
    id("project-tests-convention")
}

dependencies {
    api(project(":core:descriptors"))
    api(project(":core:descriptors.jvm"))
    api(project(":core:deserialization"))
    api(project(":compiler:util"))
    api(project(":compiler:frontend"))
    api(project(":compiler:frontend.java"))
    api(project(":compiler:cli"))
    api(project(":compiler:cli-js"))
    api(project(":compiler:fir:entrypoint"))
    api(project(":compiler:fir:fir2ir:jvm-backend"))
    api(project(":compiler:ir.serialization.jvm"))
    api(project(":compiler:backend.jvm.entrypoint"))
    api(project(":kotlin-build-common"))
    api(project(":daemon-common"))
    api(project(":compiler:build-tools:kotlin-build-statistics"))
    api(project(":compiler:build-tools:kotlin-build-tools-api"))
    implementation(project(":compiler:build-tools:kotlin-build-tools-cri-impl"))
    compileOnly(intellijCore())

    testFixturesApi(libs.junit4)
    testFixturesApi(kotlinTest("junit"))
    testFixturesApi(kotlinStdlib())
    testFixturesApi(testFixtures(project(":kotlin-build-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(intellijCore())
    testFixturesApi(commonDependency("org.jetbrains.intellij.deps:log4j"))
    testFixturesApi(intellijJDom())

    testFixturesImplementation(commonDependency("com.google.code.gson:gson"))
    testImplementation(commonDependency("com.google.code.gson:gson"))
    testRuntimeOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testRuntimeOnly(project(":core:descriptors.runtime"))
}

optInToK1Deprecation()

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}
projectTests {
    testTask(parallel = true, jUnitMode = JUnitMode.JUnit4) {
        dependsOn(":dist")
        workingDir = rootDir
        useJsIrBoxTests(version = version, buildDir = layout.buildDirectory)
    }

    testTask("testJvmICWithJdk11", parallel = true, jUnitMode = JUnitMode.JUnit4, skipInLocalBuild = false) {
        dependsOn(":dist")
        workingDir = rootDir
        useJsIrBoxTests(version = version, buildDir = layout.buildDirectory)
        filter {
            includeTestsMatching("org.jetbrains.kotlin.incremental.IncrementalK1JvmCompilerRunnerTestGenerated*")
            includeTestsMatching("org.jetbrains.kotlin.incremental.IncrementalK2JvmCompilerRunnerTestGenerated*")
        }
        javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    }

    testGenerator("org.jetbrains.kotlin.incremental.TestGeneratorForICTestsKt")
}

testsJar()
