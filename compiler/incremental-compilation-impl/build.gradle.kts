
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
    api(project(":compiler:ir.serialization.jvm"))
    api(project(":compiler:backend.jvm.entrypoint"))
    api(project(":kotlin-build-common"))
    api(project(":daemon-common"))
    implementation(commonDependency("com.google.code.gson:gson"))
    compileOnly(intellijCore())

    testApi(commonDependency("junit:junit"))
    testApi(project(":kotlin-test:kotlin-test-junit"))
    testApi(kotlinStdlib())
    testApi(projectTests(":kotlin-build-common"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(intellijCore())
    testApi(commonDependency("org.jetbrains.intellij.deps:log4j"))
    testApi(commonDependency("org.jetbrains.intellij.deps:jdom"))

    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":core:descriptors.runtime"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    workingDir = rootDir
    dependsOn(":kotlin-stdlib-js-ir:packFullRuntimeKLib")
}

projectTest("testJvmICWithJdk11", parallel = true) {
    workingDir = rootDir
    filter {
        includeTestsMatching("org.jetbrains.kotlin.incremental.IncrementalJvmCompilerRunnerTestGenerated*")
    }
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11))
}

testsJar()
