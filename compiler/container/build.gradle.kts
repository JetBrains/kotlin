plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("test-inputs-check")
}

dependencies {
    api(project(":core:util.runtime"))
    api(commonDependency("javax.inject"))
    compileOnly(kotlinStdlib())
    compileOnly(intellijCore())
    testImplementation(kotlinStdlib())
    testCompileOnly("org.jetbrains:annotations:13.0")
    testImplementation(kotlinTest("junit5"))
    testCompileOnly(intellijCore())

    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(intellijCore())
    testRuntimeOnly(libs.intellij.fastutil)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

optInToK1Deprecation()

testsJar {}

projectTests {
    testTask()
}
