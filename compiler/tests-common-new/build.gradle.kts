plugins {
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check")
    id("share-foreign-java-nullability-annotations")
    id("java-test-fixtures")
}

dependencies {
    testFixturesApi(project(":compiler:fir:entrypoint"))
    testFixturesApi(project(":compiler:fir:fir-serialization"))
    testFixturesApi(project(":compiler:fir:fir2ir:jvm-backend"))
    testFixturesApi(project(":compiler:cli"))
    testFixturesApi(project(":compiler:ir.backend.native"))
    testFixturesImplementation(project(":compiler:cli-jvm:javac-integration"))
    testFixturesImplementation(project(":compiler:ir.tree"))
    testFixturesImplementation(project(":compiler:ir.serialization.native"))
    testFixturesImplementation(project(":compiler:backend.jvm.entrypoint"))
    testFixturesImplementation(project(":compiler:backend.jvm.lower"))
    testFixturesImplementation(project(":kotlin-util-klib-abi"))
    testFixturesImplementation(intellijCore())
    testFixturesImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    testFixturesImplementation(testFixtures(project(":generators:test-generator")))

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testFixturesApi(libs.junit.platform.launcher)
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils.common")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-compiler-utils")))
    testFixturesApi(project(":libraries:tools:abi-comparator"))
    testFixturesApi(project(":compiler:tests-mutes:mutes-junit5"))

    /*
     * Actually those dependencies are needed only at runtime, but they
     *   declared as Api dependencies to propagate them to all modules
     *   which depend on current one
     */
    testFixturesApi(libs.intellij.fastutil)
    testFixturesApi(commonDependency("one.util:streamex"))
    testFixturesApi(commonDependency("org.jetbrains.intellij.deps.jna:jna"))
    testFixturesApi(jpsModel()) { isTransitive = false }
    testFixturesApi(jpsModelImpl()) { isTransitive = false }
    testFixturesApi(libs.junit4)

    testFixturesCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())

    thirdPartyAnnotationsClasspath(commonDependency("jakarta.annotation", "jakarta.annotation-api"))
    thirdPartyAnnotationsClasspath(commonDependency("io.vertx", "vertx-codegen"))
}

optInToExperimentalCompilerApi()
optInToUnsafeDuringIrConstructionAPI()
optInToK1Deprecation()

tasks.processTestFixturesResources.configure {
    from(project(":compiler").layout.projectDirectory.dir("testData")) {
        include("/diagnostics/helpers/**")
        include("/codegen/helpers/**")
        include("/ir/interpreter/helpers/**")
    }
    into("stdlib") {
        from(project(":kotlin-stdlib").layout.projectDirectory.dir("src/kotlin")) {
            into("src/kotlin")
            include("ranges/Progressions.kt")
            include("ranges/ProgressionIterators.kt")
            include("internal/progressionUtil.kt")
            include("text/regex/MatchResult.kt")
            include("collections/Sequence.kt")
            include("annotations/WasExperimental.kt")
            include("annotations/ExperimentalStdlibApi.kt")
            include("annotations/OptIn.kt")
            include("internal/Annotations.kt")
            include("experimental/inferenceMarker.kt")
        }
        from(project(":kotlin-stdlib").layout.projectDirectory.dir("unsigned/src/kotlin")) {
            into("unsigned/src/kotlin")
        }
        from(project(":kotlin-stdlib").layout.projectDirectory.dir("jvm/src/kotlin")) {
            into("jvm/src/kotlin")
            include("util/UnsignedJVM.kt")
            include("collections/TypeAliases.kt")
            include("reflect/**")
        }
        from(project(":kotlin-stdlib").layout.projectDirectory.dir("jvm/runtime/kotlin")) {
            into("jvm/runtime/kotlin")
            include("TypeAliases.kt")
            include("text/TypeAliases.kt")
            include("jvm/annotations/JvmPlatformAnnotations.kt")
        }
    }
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

projectTests {
    testData(project(":compiler").isolated, "testData/diagnostics")
    testData(project(":compiler").isolated, "testData/codegen")
    testData(project(":compiler").isolated, "testData/debug")
    testData(project(":compiler").isolated, "testData/ir")
    testData(project(":compiler").isolated, "testData/klib")

    withJvmStdlibAndReflect()
    withStdlibCommon()
    withScriptRuntime()
    withTestJar()
    withAnnotations()
    withScriptingPlugin()
    withJsRuntime()

    withMockJdkRuntime()
    withMockJDKModifiedRuntime()
    withMockJdkAnnotationsJar()
    withThirdPartyAnnotations()
    withThirdPartyJava8Annotations()
    withThirdPartyJava9Annotations()
    withThirdPartyJsr305()

    testTask(
        jUnitMode = JUnitMode.JUnit5,
        defineJDKEnvVariables = listOf(
            JdkMajorVersion.JDK_1_8,
            JdkMajorVersion.JDK_11_0, // e.g. org.jetbrains.kotlin.test.runners.ForeignAnnotationsCompiledJavaTestGenerated.Java11Tests
            JdkMajorVersion.JDK_17_0,
            JdkMajorVersion.JDK_21_0, // e.g. org.jetbrains.kotlin.test.runners.codegen.FirLightTreeBlackBoxModernJdkCodegenTestGenerated.TestsWithJava21
        )
    )

    testGenerator("org.jetbrains.kotlin.test.TestGeneratorForTestCommonNewKt", generateTestsInBuildDirectory = true)
}

testsJarToBeUsedAlongWithFixtures()
