plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("d8-configuration")
}

val compilerModules: Array<String> by rootProject.extra
val otherCompilerModules = compilerModules.filter { it != path }

dependencies {
    testImplementation(intellijCore()) // Should come before compiler, because of "progarded" stuff needed for tests

    testApi(project(":kotlin-script-runtime"))

    testApi(kotlinStdlib())

    testApi(kotlinTest())
    testCompileOnly(kotlinTest("junit"))
    testImplementation(libs.junit4)
    testApi(testFixtures(project(":compiler:tests-common")))
    testApi(testFixtures(project(":compiler:tests-common-new")))
    testApi(testFixtures(project(":compiler:fir:raw-fir:psi2fir")))
    testApi(testFixtures(project(":compiler:fir:raw-fir:light-tree2fir")))
    testApi(testFixtures(project(":compiler:fir:analysis-tests:legacy-fir-tests")))
    testApi(testFixtures(project(":generators:test-generator")))
    testApi(project(":compiler:ir.tree")) // used for deepCopyWithSymbols call that is removed by proguard from the compiler TODO: make it more straightforward
    testApi(project(":kotlin-scripting-compiler"))

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
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTest(
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

if (kotlinBuildProperties.isTeamcityBuild) {
    projectTest("fastJarFSLongTests", jUnitMode = JUnitMode.JUnit4) {
        include("**/FastJarFSLongTest*")
    }
} else {
    // avoiding IntelliJ test configuration selection menu (see comments in compiler/fir/fir2ir/build.gradle.kts for details)
    tasks.register("fastJarFSLongTests")
}

val generateTestData by generator("org.jetbrains.kotlin.generators.tests.GenerateCompilerTestDataKt")

testsJar()
