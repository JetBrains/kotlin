plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("test-inputs-check")
    id("compiler-tests-convention")
}

dependencies {
    api(project(":core:util.runtime"))
    api(commonDependency("javax.inject"))
    compileOnly(kotlinStdlib())
    compileOnly(intellijCore())
    testApi(kotlinStdlib())
    testCompileOnly("org.jetbrains:annotations:13.0")
    testApi(kotlinTest("junit5"))
    testCompileOnly(intellijCore())

    testImplementation("org.junit.jupiter:junit-jupiter:${libs.versions.junit5.get()}")
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(intellijCore())
    testRuntimeOnly(libs.intellij.fastutil)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar {}

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5)
}
