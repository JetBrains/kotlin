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

fun Project.codegenTest(target: Int, jvm: Int,
                        jdk: String = "JDK_${if (jvm <= 8) "1" else ""}$jvm",
                        body: Test.() -> Unit): Test = projectTest("codegenTarget${target}Jvm${jvm}Test") {
    dependsOn(":dist")
    workingDir = rootDir

    filter.includeTestsMatching("org.jetbrains.kotlin.codegen.jdk.JvmTarget${target}OnJvm${jvm}")

    body()
    doFirst {
        val jdkPath = project.findProperty(jdk) ?: error("$jdk is not optional to run this test")
        executable = "$jdkPath/bin/java"
        println("Running test with $executable")
    }
    group = "verification"
}

codegenTest(target = 6, jvm = 6, jdk = "JDK_18") {
    dependsOn(testJvm6ServerRuntime)

    val port = project.findProperty("kotlin.compiler.codegen.tests.port")?.toString() ?: "5100"
    var jdkProcess: Process? = null

    doFirst {
        logger.info("Configuring JDK 6 server...")
        val jdkPath = project.findProperty("JDK_16") ?: error("JDK_16 is not optional to run this test")
        val executable = "$jdkPath/bin/java"
        val main = "org.jetbrains.kotlin.test.clientserver.TestProcessServer"
        val classpath = testJvm6ServerRuntime.asPath

        logger.debug("Server classpath: $classpath")

        val builder = ProcessBuilder(executable, "-cp", classpath, main, port)
        builder.directory(rootDir)

        builder.inheritIO()
        builder.redirectErrorStream(true)

        logger.info("Starting JDK 6 server $executable")
        jdkProcess = builder.start()

    }
    systemProperty("kotlin.test.default.jvm.target", "1.6")
    systemProperty("kotlin.test.java.compilation.target", "1.6")
    systemProperty("kotlin.test.box.in.separate.process.port", port)

    doLast {
        logger.info("Stopping JDK 6 server...")
        jdkProcess?.destroy()
    }
}

codegenTest(target = 6, jvm = 9) {
    systemProperty("kotlin.test.default.jvm.target", "1.6")
}

codegenTest(target = 8, jvm = 8) {
    systemProperty("kotlin.test.default.jvm.target", "1.8")
}

codegenTest(target = 8, jvm = 9) {
    systemProperty("kotlin.test.default.jvm.target", "1.8")
}

codegenTest(target = 9, jvm = 9) {
    systemProperty("kotlin.test.default.jvm.target", "1.8")
    systemProperty("kotlin.test.substitute.bytecode.1.8.to.1.9", "true")
}

codegenTest(target = 10, jvm = 10) {
    systemProperty("kotlin.test.default.jvm.target", "1.8")
    systemProperty("kotlin.test.substitute.bytecode.1.8.to.10", "true")
}

codegenTest(target = 8, jvm = 11) {
    systemProperty("kotlin.test.default.jvm.target", "1.8")
    jvmArgs!!.add( "-XX:-FailOverToOldVerifier")
}


testsJar()
