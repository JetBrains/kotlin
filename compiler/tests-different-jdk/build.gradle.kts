plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(projectTests(":compiler"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))

    testApiJUnit5(vintageEngine = true, runner = true, suiteApi = true)

    testImplementation(intellijCore())
    testRuntimeOnly(project(":kotlin-reflect"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

fun Project.codegenTest(
    target: Int,
    jdk: JdkMajorVersion,
    jvm: String = jdk.majorVersion.toString(),
    targetInTestClass: String = "$target",
    body: Test.() -> Unit = {}
): TaskProvider<Test> = projectTest(
    taskName = "codegenTarget${targetInTestClass}Jvm${jvm}Test",
    jUnitMode = JUnitMode.JUnit5
) {
    dependsOn(":dist")
    workingDir = rootDir

    val testName = "JvmTarget${targetInTestClass}OnJvm${jvm}"
    filter.includeTestsMatching("org.jetbrains.kotlin.codegen.jdk.$testName")

    javaLauncher.set(project.getToolchainLauncherFor(jdk))

    systemProperty("kotlin.test.default.jvm.target", "${if (target <= 8) "1." else ""}$target")
    body()
    doFirst {
        logger.warn("Running tests with $target target and ${javaLauncher.get().metadata.installationPath.asFile}")
    }
    group = "verification"
}

//JDK 8
// This is default one and is executed in default build configuration
codegenTest(target = 8, jdk = JdkMajorVersion.JDK_1_8)

//JDK 11
codegenTest(target = 8, jdk = JdkMajorVersion.JDK_11)

codegenTest(target = 11, jdk = JdkMajorVersion.JDK_11)

//JDK 17
codegenTest(target = 8, jdk = JdkMajorVersion.JDK_17)

codegenTest(target = 17, jdk = JdkMajorVersion.JDK_17) {
    systemProperty("kotlin.test.box.d8.disable", true)
}

//..also add this two tasks to build after adding fresh jdks to build agents
val mostRecentJdk = JdkMajorVersion.values().last()

//LAST JDK from JdkMajorVersion available on machine
codegenTest(target = 8, jvm = "Last", jdk = mostRecentJdk)

codegenTest(
    target = mostRecentJdk.majorVersion,
    targetInTestClass = "Last",
    jvm = "Last",
    jdk = mostRecentJdk
) {
    systemProperty("kotlin.test.box.d8.disable", true)
}

testsJar()
