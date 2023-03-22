plugins {
    kotlin("jvm")
    id("jps-compatible")
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
    compileOnly(intellijCore())

    testApi(commonDependency("junit:junit"))
    testApi(project(":kotlin-test:kotlin-test-junit"))
    testApi(kotlinStdlib())
    testApi(projectTests(":kotlin-build-common"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(intellijCore())
    testApi(commonDependency("org.jetbrains.intellij.deps:log4j"))
    testApi(commonDependency("org.jetbrains.intellij.deps:jdom"))

    testImplementation(commonDependency("com.google.code.gson:gson"))
    testRuntimeOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testRuntimeOnly(project(":core:descriptors.runtime"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    workingDir = rootDir
    useJsIrBoxTests(version = version, buildDir = "$buildDir/")
}

projectTest("testJvmICWithJdk11", parallel = true) {
    workingDir = rootDir
    useJsIrBoxTests(version = version, buildDir = "$buildDir/")
    filter {
        includeTestsMatching("org.jetbrains.kotlin.incremental.IncrementalK1JvmCompilerRunnerTestGenerated*")
        includeTestsMatching("org.jetbrains.kotlin.incremental.IncrementalK2JvmCompilerRunnerTestGenerated*")
    }
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
}

testsJar()
