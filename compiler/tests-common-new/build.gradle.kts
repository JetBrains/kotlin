plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("compiler-tests-convention")
}

dependencies {
    testApi(project(":compiler:fir:entrypoint"))
    testApi(project(":compiler:fir:fir-serialization"))
    testApi(project(":compiler:fir:fir2ir:jvm-backend"))
    testApi(project(":compiler:cli"))
    testImplementation(project(":compiler:ir.tree"))
    testImplementation(project(":compiler:backend.jvm.entrypoint"))
    testImplementation(project(":compiler:backend.jvm.lower"))
    testImplementation(project(":kotlin-util-klib-abi"))
    testImplementation(intellijCore())
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    testRuntimeOnly(project(":core:descriptors.runtime"))

    testImplementation(projectTests(":generators:test-generator"))

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testApi(libs.junit.platform.launcher)
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(project(":libraries:tools:abi-comparator"))
    testApi(project(":compiler:tests-mutes:mutes-junit5"))

    /*
     * Actually those dependencies are needed only at runtime, but they
     *   declared as Api dependencies to propagate them to all modules
     *   which depend on current one
     */
    testApi(libs.intellij.fastutil)
    testApi(commonDependency("one.util:streamex"))
    testApi(commonDependency("org.jetbrains.intellij.deps.jna:jna"))
    testApi(jpsModel()) { isTransitive = false }
    testApi(jpsModelImpl()) { isTransitive = false }
    testApi(libs.junit4)

    testApi(toolsJarApi())
    testRuntimeOnly(toolsJar())
}

optInToExperimentalCompilerApi()
optInToUnsafeDuringIrConstructionAPI()

val zipJsr305TestAnnotations = tasks.register<Zip>("zipJsr305TestAnnotations") {
    archiveFileName.set("jsr305_test_annotations.jar")
    from { project(":compiler").layout.projectDirectory.dir("testData/diagnostics/helpers/jsr305_test_annotations") }
}

tasks.processTestResources.configure {
    from(zipJsr305TestAnnotations)
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
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

compilerTests {
    testData(project(":compiler").isolated, "testData/diagnostics")
    testData(project(":compiler").isolated, "testData/codegen")
    testData(project(":compiler").isolated, "testData/debug")
    testData(project(":compiler").isolated, "testData/ir")
    testData(project(":compiler").isolated, "testData/klib")
    withStdlibCommon()
    withScriptRuntime()
    withTestJar()
    withAnnotations()
    withScriptingPlugin()
    withStdlibJsRuntime()
    withTestJsRuntime()

    withMockJdkRuntime()
    withMockJDKModifiedRuntime()
    withMockJdkAnnotationsJar()
    withThirdPartyAnnotations()
    withThirdPartyJava8Annotations()
    withThirdPartyJava9Annotations()
    withThirdPartyJsr305()
}

projectTest(
    jUnitMode = JUnitMode.JUnit5,
    defineJDKEnvVariables = listOf(
        JdkMajorVersion.JDK_1_8,
        JdkMajorVersion.JDK_11_0, // e.g. org.jetbrains.kotlin.test.runners.ForeignAnnotationsCompiledJavaTestGenerated.Java11Tests
        JdkMajorVersion.JDK_17_0,
        JdkMajorVersion.JDK_21_0, // e.g. org.jetbrains.kotlin.test.runners.codegen.FirLightTreeBlackBoxModernJdkCodegenTestGenerated.TestsWithJava21
    )
) {
    useJUnitPlatform()
}

testsJar()
