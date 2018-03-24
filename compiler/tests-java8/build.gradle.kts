import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(intellijCoreDep()) { includeJars("intellij-core") }
    testCompile(projectTests(":generators:test-generator"))
    testRuntime(projectDist(":kotlin-reflect"))
    testRuntime(intellijDep())
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jdkHome = rootProject.extra["JDK_18"]!!.toString()
    kotlinOptions.jvmTarget = "1.8"
}

projectTest {
    executable = "${rootProject.extra["JDK_18"]!!}/bin/java"
    dependsOn(":dist")
    workingDir = rootDir
    systemProperty("kotlin.test.script.classpath", the<JavaPluginConvention>().sourceSets.getByName("test").output.classesDirs.joinToString(File.pathSeparator))
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateJava8TestsKt")

testsJar()
