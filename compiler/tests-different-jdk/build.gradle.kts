import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val testJvm6ServerRuntime by configurations.creating

dependencies {
    testCompile(projectTests(":compiler"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testCompile(projectTests(":compiler:tests-common-new"))

    testApiJUnit5(vintageEngine = true, runner = true, suiteApi = true)

    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntime(project(":kotlin-reflect"))
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
): TaskProvider<Test> = projectTest("codegenTarget${targetInTestClass}Jvm${jvm}Test", jUnit5Enabled = true) {
    dependsOn(":dist")
    workingDir = rootDir

    val testName = "JvmTarget${targetInTestClass}OnJvm${jvm}"
    filter.includeTestsMatching("org.jetbrains.kotlin.codegen.jdk.$testName")

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

//JDK 8
codegenTest(target = 6, jvm = 8) {}

// This is default one and is executed in default build configuration
codegenTest(target = 8, jvm = 8) {}

//JDK 11
codegenTest(target = 6, jvm = 11) {}

codegenTest(target = 8, jvm = 11) {}

codegenTest(target = 11, jvm = 11) {}

//JDK 15
codegenTest(target = 6, jvm = 15) {}

codegenTest(target = 8, jvm = 15) {}

codegenTest(target = 15, jvm = 15) {
    systemProperty("kotlin.test.box.d8.disable", true)
}

//..also add this two tasks to build after adding fresh jdks to build agents
val mostRecentJdk = JdkMajorVersion.values().last().name

//LAST JDK from JdkMajorVersion available on machine
codegenTest(target = 6, jvm = "Last", jdk = mostRecentJdk) {}

codegenTest(target = 8, jvm = "Last", jdk = mostRecentJdk) {}

codegenTest(
    mostRecentJdk.substringAfter('_').toInt(),
    targetInTestClass = "Last",
    jvm = "Last",
    jdk = mostRecentJdk
) {
    systemProperty("kotlin.test.box.d8.disable", true)
}

testsJar()
