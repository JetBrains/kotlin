plugins {
    kotlin("jvm")
    id("d8-configuration")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
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
    testRuntimeOnly(libs.junit.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
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
        jUnitMode = JUnitMode.JUnit5,
        javaLauncher = JdkMajorVersion.JDK_1_8,
        defineJDKEnvVariables = listOf(JdkMajorVersion.JDK_1_8, JdkMajorVersion.JDK_11_0, JdkMajorVersion.JDK_17_0)
    ) {
        filter {
            excludeTestsMatching("org.jetbrains.kotlin.jvm.compiler.io.FastJarFSLongTest*")
        }

        addClasspathProperty(testSourceSet.output.classesDirs, "kotlin.test.script.classpath")
    }

    testTask("fastJarFSLongTests", jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = true) {
        include("**/FastJarFSLongTest*")
    }

    testGenerator("org.jetbrains.kotlin.generators.tests.TestGeneratorForCompilerTestsKt")

    testData(isolated, "testData/checkLocalVariablesTable")
    testData(isolated, "testData/codegen")
    testData(isolated, "testData/compileJavaAgainstKotlin")
    testData(isolated, "testData/kotlinClassFinder")
    testData(isolated, "testData/moduleProtoBuf")
    testData(isolated, "testData/modules.xml")
    testData(isolated, "testData/serialization")
    testData(isolated, "testData/versionRequirement")
    testData(isolated, "testData/writeFlags")
    testData(isolated, "testData/writeSignature")
    withJvmStdlibAndReflect()
    withScriptRuntime()
    withTestJar()
    withStdlibCommon()
    withMockJdkRuntime()
    withMockJdkAnnotationsJar()
}

val generateTestData by generator("org.jetbrains.kotlin.generators.tests.GenerateCompilerTestDataKt", testSourceSet)

testsJar()
