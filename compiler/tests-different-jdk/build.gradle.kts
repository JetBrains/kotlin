import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val testJvm6ServerRuntime by configurations.creating

dependencies {
    testCompile(projectTests(":compiler"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntime(project(":kotlin-reflect"))
    testRuntime(intellijDep())
    testRuntime(intellijDep())
    testJvm6ServerRuntime(projectTests(":compiler:tests-common-jvm6"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

fun Project.codegenTest(
    target: Int, jvm: Int,
    jdk: String = "JDK_${if (jvm <= 8) "1" else ""}$jvm",
    body: Test.() -> Unit
) {
    codegenTest(target, jvm.toString(), jdk, body = body)
}

fun Project.codegenTest(
    target: Int, jvm: String, jdk: String,
    targetInTestClass: String = "$target",
    body: Test.() -> Unit
): TaskProvider<Test> = projectTest("codegenTarget${targetInTestClass}Jvm${jvm}Test") {
    dependsOn(":dist")
    workingDir = rootDir

    filter.includeTestsMatching("org.jetbrains.kotlin.codegen.jdk.JvmTarget${targetInTestClass}OnJvm${jvm}")

    systemProperty("kotlin.test.default.jvm.target", "${if (target <= 8) "1." else ""}$target")
    body()
    doFirst {
        val jdkPath = project.findProperty(jdk) ?: error("$jdk is not optional to run this test")
        executable = "$jdkPath/bin/java"
        println("Running tests with $target target and $executable")
    }
    group = "verification"
}

codegenTest(target = 6, jvm = 6, jdk = "JDK_18") {
    dependsOn(testJvm6ServerRuntime)

    doFirst {
        systemProperty("kotlin.test.default.jvm.target", "1.6")
        systemProperty("kotlin.test.java.compilation.target", "1.6")

        val port = project.findProperty("kotlin.compiler.codegen.tests.port") ?: "5100"
        systemProperty("kotlin.test.box.in.separate.process.port", port)
        systemProperty("kotlin.test.box.in.separate.process.server.classpath", testJvm6ServerRuntime.asPath)
    }
}

codegenTest(target = 6, jvm = 9) {}

codegenTest(target = 8, jvm = 8) {}

codegenTest(target = 8, jvm = 9) {}

codegenTest(target = 9, jvm = 9) {}

val mostRecentJdk = JdkMajorVersion.values().last().name

codegenTest(target = 6, jvm = "Last", jdk = mostRecentJdk) {
    jvmArgs!!.add( "-XX:-FailOverToOldVerifier")
}

codegenTest(target = 8, jvm = "Last", jdk = mostRecentJdk) {
    jvmArgs!!.add( "-XX:-FailOverToOldVerifier")
}

codegenTest(
    mostRecentJdk.substringAfter('_').toInt(),
    targetInTestClass = "Last",
    jvm = "Last",
    jdk = mostRecentJdk
) {}

testsJar()
