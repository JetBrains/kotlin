import TaskUtils.useAndroidEmulator

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(project(":core:descriptors"))
    testApi(project(":core:descriptors.jvm"))
    testApi(project(":compiler:util"))
    testApi(project(":compiler:cli"))
    testApi(project(":compiler:frontend"))
    testApi(project(":compiler:backend"))
    testApi(project(":compiler:incremental-compilation-impl"))
    testApi(project(":compiler:frontend.java"))

    testApi(kotlinStdlib())
    testApi(projectTests(":compiler:tests-common"))
    testImplementation(libs.junit4)
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))

    testApi(jpsModel())
    testApi(jpsBuildTest())

    testRuntimeOnly(intellijCore())
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.jna:jna"))

    testImplementation(libs.junit.platform.launcher)
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest {
    dependsOn(":dist")
    val jdkHome = project.getToolchainJdkHomeFor(JdkMajorVersion.JDK_1_8)
    doFirst {
        environment("kotlin.tests.android.timeout", "45")
        environment("JAVA_HOME", jdkHome.get())
    }

    if (project.hasProperty("teamcity") || project.hasProperty("kotlin.test.android.teamcity")) {
        systemProperty("kotlin.test.android.teamcity", true)
    }

    project.findProperty("kotlin.test.android.path.filter")?.let {
        systemProperty("kotlin.test.android.path.filter", it.toString())
    }

    workingDir = rootDir
    useAndroidEmulator(this)
}

val generateAndroidTests by generator("org.jetbrains.kotlin.android.tests.CodegenTestsOnAndroidGenerator") {
    workingDir = rootDir
    dependsOn(rootProject.tasks.named("dist"))
}
