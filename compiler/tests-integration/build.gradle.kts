plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
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
    testFixturesImplementation(project(":compiler:cli-jvm:javac-integration"))
    testImplementation(project(":compiler:cli-jvm:javac-integration"))

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
        useJUnitPlatform()

        systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
        systemProperty("kotlin.ant.classpath", antLauncherJar.asPath)
        systemProperty("kotlin.ant.launcher.class", "org.apache.tools.ant.Main")

        testInputsCheck {
            with(extraPermissions) {
                add("""permission java.util.PropertyPermission "kotlin.language.settings", "write";""")
                add("""permission java.util.PropertyPermission "kotlin.test.is.pre.release", "write";""")
                add("""permission java.util.PropertyPermission "java.awt.headless", "write";""")
                // jline/REPL tests need broad property access and command execution
                add("""permission java.util.PropertyPermission "*", "read,write";""")
                add("""permission java.lang.RuntimePermission "loadLibrary.*";""")
                add("""permission java.io.FilePermission "<<ALL FILES>>", "execute";""")
                add("""permission java.lang.RuntimePermission "getenv.*";""")
                // PathUtil auto-discovery tries to read dist/ relative to CWD
                add("""permission java.io.FilePermission "dist", "read";""")
                add("""permission java.io.FilePermission "dist/-", "read";""")
                // Integration tests execute launcher scripts from dist
                add("""permission java.io.FilePermission "${rootDir.absolutePath}/dist/-", "execute";""")
                // Compiler checks relative paths for source files
                add("""permission java.io.FilePermission "${projectDir.absolutePath}/-", "read";""")
            }
        }
    }

    testGenerator("org.jetbrains.kotlin.TestGeneratorForTestsIntegrationTestsKt")

    withJvmStdlibAndReflect()
    withTestJar()
    withAnnotations()
    withJsRuntime()
    withWasmRuntime()
    withStdlibCommon()
    withMockJdkRuntime()
    withMockJdkAnnotationsJar()
    withThirdPartyAnnotations()
    withThirdPartyJava8Annotations()
    withThirdPartyJsr305()
    withScriptingPlugin()
    withScriptRuntime()
    @OptIn(KotlinCompilerDistUsage::class)
    withDist()

    testData(project(":compiler").isolated, "testData")
}

testsJar()
