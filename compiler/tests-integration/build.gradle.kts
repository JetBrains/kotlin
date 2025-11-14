plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
}

val compilerModules: Array<String> by rootProject.extra
val otherCompilerModules = compilerModules.filter { it != path }

val antLauncherJar by configurations.creating

dependencies {
    testImplementation(intellijCore())

    testFixturesApi(project(":kotlin-script-runtime"))

    testFixturesApi(kotlinStdlib())

    testFixturesApi(kotlinTest())
    testCompileOnly(kotlinTest("junit"))

    testFixturesApi(libs.junit4)
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testFixturesApi(libs.junit.platform.launcher)

    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)

    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":compiler:fir:raw-fir:psi2fir")))
    testFixturesApi(testFixtures(project(":compiler:fir:raw-fir:light-tree2fir")))
    testFixturesApi(testFixtures(project(":compiler:fir:analysis-tests:legacy-fir-tests")))
    testFixturesApi(testFixtures(project(":generators:test-generator")))
    testFixturesApi(project(":compiler:ir.tree")) // used for deepCopyWithSymbols call that is removed by proguard from the compiler TODO: make it more straightforward
    testFixturesApi(project(":kotlin-scripting-compiler"))

    otherCompilerModules.forEach {
        testCompileOnly(project(it))
    }

    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testFixturesCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())

    antLauncherJar(commonDependency("org.apache.ant", "ant"))
    antLauncherJar(toolsJar())
}

optInToExperimentalCompilerApi()
optInToK1Deprecation()

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

projectTests {
    testTask(
        parallel = true,
        defineJDKEnvVariables = listOf(
            JdkMajorVersion.JDK_1_8,
            JdkMajorVersion.JDK_11_0,
            JdkMajorVersion.JDK_17_0,
            JdkMajorVersion.JDK_21_0
        ),
        jUnitMode = JUnitMode.JUnit4
    ) {
        dependsOn(":dist")
        dependsOn(":kotlin-stdlib:compileKotlinWasmJs")

        workingDir = rootDir

        useJUnitPlatform()

        systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
        val antLauncherJarPathProvider = project.provider {
            antLauncherJar.asPath
        }
        doFirst {
            systemProperty("kotlin.ant.classpath", antLauncherJarPathProvider.get())
            systemProperty("kotlin.ant.launcher.class", "org.apache.tools.ant.Main")
        }
    }

    testGenerator("org.jetbrains.kotlin.TestGeneratorForTestsIntegrationTestsKt")

    withJvmStdlibAndReflect()
}

testsJar()
