plugins {
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    testImplementation(project(":compiler:tests-common-new", "testsJarConfig"))
    testRuntimeOnly(testFixtures(project(":compiler:tests-common-new")))
    testImplementation(project(":compiler:fir:fir2ir", "testsJarConfig"))
    testRuntimeOnly(testFixtures(project(":compiler:fir:fir2ir")))

    testImplementation(libs.junit4)
    testImplementation(kotlinStdlib())
    testImplementation(project(":libraries:tools:abi-comparator"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.platform.suite)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)

    testImplementation(intellijCore())
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

projectTests {
    testData(project(":compiler").isolated, "testData/codegen")
    testData(project(":compiler").isolated, "testData/klib")

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withScriptingPlugin()
    withAnnotations()
    withMockJdkRuntime()
    withTestJar()

    withMockJDKModifiedRuntime()
    withMockJdkAnnotationsJar()
    withThirdPartyAnnotations()
    withThirdPartyJsr305()

    fun codegenTestTask(
        target: Int,
        jdk: JdkMajorVersion,
        jvm: String = jdk.majorVersion.toString(),
        targetInTestClass: String = "$target",
        body: Test.() -> Unit = {},
    ) {
        testTask(
            taskName = "codegenTarget${targetInTestClass}Jvm${jvm}Test",
            jUnitMode = JUnitMode.JUnit5,
            maxMetaspaceSizeMb = 1024,
            skipInLocalBuild = false,
            defineJDKEnvVariables = listOf(jdk, JdkMajorVersion.JDK_11_0)
        ) {
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
    }

    //JDK 8
    // This is default one and is executed in default build configuration
    codegenTestTask(target = 8, jdk = JdkMajorVersion.JDK_1_8)

    //JDK 11
    codegenTestTask(target = 8, jdk = JdkMajorVersion.JDK_11_0)

    codegenTestTask(target = 11, jdk = JdkMajorVersion.JDK_11_0)

    //JDK 17
    codegenTestTask(target = 8, jdk = JdkMajorVersion.JDK_17_0)

    codegenTestTask(target = 17, jdk = JdkMajorVersion.JDK_17_0) {
        systemProperty("kotlin.test.box.d8.disable", true)
    }

    //..also add this two tasks to build after adding fresh jdks to build agents
    val mostRecentJdk = JdkMajorVersion.values().last()

    //LAST JDK from JdkMajorVersion available on machine
    codegenTestTask(target = 8, jvm = "Last", jdk = mostRecentJdk)

    codegenTestTask(
        target = mostRecentJdk.majorVersion,
        targetInTestClass = "Last",
        jvm = "Last",
        jdk = mostRecentJdk
    ) {
        systemProperty("kotlin.test.box.d8.disable", true)
    }
}
