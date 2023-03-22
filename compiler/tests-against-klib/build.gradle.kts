import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import java.io.File

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(kotlinStdlib())
    testApi(projectTests(":generators:test-generator"))
    testApi(projectTests(":compiler:tests-common"))

    testCompileOnly(intellijCore())
    testRuntimeOnly(intellijCore())
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar {}

projectTest(parallel = true) {
    dependsOn(":kotlin-stdlib:jsJar")
    workingDir = rootDir
    systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateCompilerTestsAgainstKlibKt")
