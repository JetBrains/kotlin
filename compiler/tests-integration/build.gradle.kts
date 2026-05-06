plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    //id("test-inputs-check")
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
}

testsJar()
