plugins {
    kotlin("jvm")
    id("d8-configuration")
    id("java-test-fixtures")
    id("project-tests-convention")
}

val compilerModules: Array<String> by rootProject.extra
val otherCompilerModules = compilerModules.filter { it != path }

dependencies {
    testImplementation(intellijCore()) // Should come before compiler, because of "progarded" stuff needed for tests

    testImplementation(project(":kotlin-script-runtime"))

    testImplementation(kotlinStdlib())

    testImplementation(kotlinTest())
    testCompileOnly(kotlinTest("junit"))
    testImplementation(libs.junit4)
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
    testCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())
}

optInToK1Deprecation()
optInToExperimentalCompilerApi()

sourceSets {
    "main" {}
    "testFixtures" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTests {
    testTask(
        jUnitMode = JUnitMode.JUnit4,
        parallel = true,
        defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_1_8, JdkMajorVersion.JDK_11_0, JdkMajorVersion.JDK_17_0)
    ) {
        dependsOn(":dist")
        useJsIrBoxTests(version = version, buildDir = layout.buildDirectory)

        filter {
            excludeTestsMatching("org.jetbrains.kotlin.jvm.compiler.io.FastJarFSLongTest*")
        }

        workingDir = rootDir
        systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
    }

    testTask("fastJarFSLongTests", jUnitMode = JUnitMode.JUnit4, skipInLocalBuild = true) {
        include("**/FastJarFSLongTest*")
    }

    testGenerator("org.jetbrains.kotlin.generators.tests.TestGeneratorForCompilerTestsKt")

    withJvmStdlibAndReflect()
}

val generateTestData by generator("org.jetbrains.kotlin.generators.tests.GenerateCompilerTestDataKt", testSourceSet)

testsJar()
