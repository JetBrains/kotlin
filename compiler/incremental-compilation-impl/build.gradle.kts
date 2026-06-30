plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("d8-configuration")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

dependencies {
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    runtimeOnly(project(":core:deserialization"))
    implementation(project(":kotlin-util-klib-metadata"))
    implementation(libs.intellij.asm)
    implementation(libs.guava)
    implementation(commonDependency("com.google.code.findbugs", "jsr305"))
    api(project(":compiler:util"))
    api(project(":compiler:cli"))
    api(project(":compiler:cli-jvm"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":compiler:backend.jvm"))
    api(project(":compiler:cli-js"))
    api(project(":compiler:cli-metadata"))
    api(project(":compiler:fir:entrypoint"))
    api(project(":compiler:fir:fir2ir:jvm-backend"))
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

    testRuntimeOnly(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    main { projectDefault() }
    test {
        projectDefault()
        generatedTestDir()
    }
    testFixtures { projectDefault() }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5, javaLauncher = JdkMajorVersion.JDK_1_8) {
        useJsIrBoxTests(buildDir = layout.buildDirectory)
    }

    testTask("testJvmICWithJdk11", jUnitMode = JUnitMode.JUnit5, javaLauncher = JdkMajorVersion.JDK_11_0, skipInLocalBuild = false) {
        useJsIrBoxTests(buildDir = layout.buildDirectory)
        filter {
            includeTestsMatching("org.jetbrains.kotlin.incremental.IncrementalK1JvmCompilerRunnerTestGenerated*")
            includeTestsMatching("org.jetbrains.kotlin.incremental.IncrementalK2JvmCompilerRunnerTestGenerated*")
        }
    }


    testGenerator("org.jetbrains.kotlin.incremental.TestGeneratorForICTestsKt")
    testData(project.isolated, "testData")
    testData(project(":jps:jps-plugin").isolated, "testData")
    withJsRuntime()
    withJvmStdlibAndReflect()
    withMockJdkAnnotationsJar()
}

testsJar()
