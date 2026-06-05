import org.jetbrains.kotlin.testFederation.SmokeTestConfig
import org.jetbrains.kotlin.testFederation.isSmokeTestMode
import org.jetbrains.kotlin.testFederation.smokeTestConfig

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    //id("test-inputs-check")
}

val otherCompilerModules = CompilerModules.compilerModules.filter { it != path }

val antLauncherJar = configurations.create("antLauncherJar")

dependencies {

    testFixturesApi(project(":kotlin-script-runtime"))

    testFixturesApi(kotlinStdlib())

    testFixturesApi(kotlinTest())
    testCompileOnly(kotlinTest("junit"))

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testFixturesApi(libs.junit.platform.launcher)

    testRuntimeOnly(libs.junit.jupiter.engine)

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
    testImplementation(project(":kotlinx-metadata-klib"))
    testFixturesCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())

    antLauncherJar(commonDependency("org.apache.ant", "ant"))
    antLauncherJar(toolsJar())
}

/**
 * The `:abi-comparator` dependency is a fat-jar which contains some metadata-related classes. Together with `tests-common-new` it appeared
 * on runtime classpath of `:compiler:tests-integration` and conflicted with the same classes from `protobufLite`,
 * required for `kotlinx-metadata-klib` dependency.
 *
 * But it's not possible to just remove the `:abi-comparator` dependency from the `:tests-common-new` because of KT-86586.
 * So it's removed from the test runtime classpath here manually.
 */
configurations {
    testRuntimeClasspath {
        exclude(module = "abi-comparator")
    }
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
        defineJDKEnvVariables = listOf(
            JdkMajorVersion.JDK_1_8,
            JdkMajorVersion.JDK_11_0,
            JdkMajorVersion.JDK_17_0,
            JdkMajorVersion.JDK_21_0
        ),
        javaLauncher = JdkMajorVersion.JDK_1_8
    ) {
        dependsOn(":dist")
        dependsOn(":kotlin-stdlib:compileKotlinWasmJs")

        workingDir = rootDir

        useJUnitPlatform()

        jvmArgumentProviders.add(
            project.objects.newInstance(SystemPropertyClasspathProvider::class.java).apply {
                property.set("kotlin.test.script.classpath")
                classpath.from(testSourceSet.output.classesDirs)
            }
        )
        /*testInputsCheck {
            extraPermissions.addAll(
                "permission java.io.FilePermission \"\$JDK_1_8, \$JDK_1_8\", \"read\";",
                "permission java.io.FilePermission \"abacaba\", \"read\";",
                "permission java.io.FilePermission \"/non-existing-path\", \"read\";",
                "permission java.io.FilePermission \"not/existing/path\", \"read\";",
                "permission java.io.FilePermission \"non-existing-path.jar\", \"read\";",
                "permission java.io.FilePermission \"path/to/nonexistent.kts\", \"read\";",
                "permission java.util.PropertyPermission \"kotlin.language.settings\", \"write\";",
            )
        }*/
        addClasspathProperty(antLauncherJar, "kotlin.ant.classpath")
        systemProperty("kotlin.ant.launcher.class", "org.apache.tools.ant.Main")

        /*
        This test is still using junit3 style tests, neither 'Category' nor 'Tag' mechanics are supported.
        We declare smoke tests here, junit3 compliant.
        */
        smokeTestConfig = SmokeTestConfig.RunAllTests
        if (isSmokeTestMode.get()) {
            filter {
                includeTestsMatching("*SmokeTest")
                includeTestsMatching("*CliTestGenerated$*")
            }
        }
    }

    testGenerator("org.jetbrains.kotlin.TestGeneratorForTestsIntegrationTestsKt")

    testData(isolated, "testData")
    testData(project(":compiler").isolated, "testData/cli")
    testData(project(":compiler").isolated, "testData/codegen")
    testData(project(":compiler").isolated, "testData/compileKotlinAgainstCustomBinaries")
    testData(project(":compiler").isolated, "testData/ir/klibLayout/multiFiles.kt")
    testData(project(":compiler").isolated, "testData/kotlinClassFinder/nestedClass.kt")
    testData(project(":compiler").isolated, "testData/loadJavaPackageAnnotations")

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withTestJar()
    withThirdPartyAnnotations()
    @OptIn(KotlinCompilerDistUsage::class)
    withDist()
    withThirdPartyJsr305()
    withThirdPartyJava8Annotations()
    withWasmRuntime()
    withMockJdkRuntime()
    withMockJDKModifiedRuntime()
    withMockJdkAnnotationsJar()
    withStdlibCommon()
    withJsRuntime()
    withLombokCompilerPluginJar()
    withAllOpenCompilerPluginJar()
    withNoArgCompilerPluginJar()
    withMainKtsJar()
    withScriptingPlugin()
}

testsJar()
