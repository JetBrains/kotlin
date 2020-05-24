import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompile(project(":kotlin-scripting-compiler-unshaded"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testCompile(projectTests(":generators:test-generator"))
    testRuntime(project(":kotlin-reflect"))
    testRuntimeOnly(toolsJar())
    testRuntime(intellijDep())
    Platform[192].orHigher {
        testRuntimeOnly(intellijPluginDep("java"))
    }
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    executable = "${rootProject.extra["JDK_18"]!!}/bin/java"
    dependsOn(":dist")
    workingDir = rootDir
    systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
    systemProperty("idea.home.path", intellijRootDir().canonicalPath)
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateJava8TestsKt")

testsJar()
