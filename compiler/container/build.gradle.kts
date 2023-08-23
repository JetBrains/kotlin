plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:util.runtime"))
    api(commonDependency("javax.inject"))
    compileOnly(kotlinStdlib())
    compileOnly(intellijCore())
    testApi(kotlinStdlib())
    testCompileOnly("org.jetbrains:annotations:13.0")
    testApi(project(":kotlin-test:kotlin-test-jvm"))
    testApi(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(libs.junit4)
    testCompileOnly(intellijCore())

    testRuntimeOnly(intellijCore())
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar {}

projectTest(parallel = true) {
    workingDir = rootDir
}
