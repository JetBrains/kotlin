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
    testApi(commonDependency("junit:junit"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))

    testApi(commonDependency("junit:junit"))

    testApi(jpsModel())
    testApi(jpsBuildTest())

    testRuntimeOnly(intellijCore())
    testRuntimeOnly(commonDependency("net.java.dev.jna:jna"))

    testApi("org.junit.platform:junit-platform-launcher:${commonDependencyVersion("org.junit.platform", "")}")
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest {
    dependsOn(":dist")
    doFirst {
        environment("kotlin.tests.android.timeout", "45")
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
