plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":core:util.runtime"))
    compile(commonDep("javax.inject"))
    compileOnly(kotlinStdlib())
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testCompile(kotlinStdlib())
    testCompileOnly("org.jetbrains:annotations:13.0")
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntime(intellijDep()) { includeJars("trove4j", "util") }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar {}

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
}
