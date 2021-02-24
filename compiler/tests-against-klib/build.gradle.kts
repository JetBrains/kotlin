import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import java.io.File

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(kotlinStdlib())
    testCompile(projectTests(":compiler:visualizer"))
    testCompile(projectTests(":generators:test-generator"))
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar {}

projectTest(parallel = true) {
    dependsOn(":dist")
    dependsOn(":kotlin-stdlib-js-ir:compileKotlinJs")
    workingDir = rootDir
    systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateCompilerTestsAgainstKlibKt")
