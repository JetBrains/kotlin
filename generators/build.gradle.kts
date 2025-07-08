plugins {
    kotlin("jvm")
    id("jps-compatible")
}

sourceSets {
    "main" { java.srcDirs("main") }
    "test" { projectDefault() }
}

fun extraSourceSet(name: String, extendMain: Boolean = true, jpsKind: String? = null): Pair<SourceSet, Configuration> {
    val sourceSet = sourceSets.create(name) {
        java.srcDir(name)
    }
    val api = configurations[sourceSet.apiConfigurationName]
    if (extendMain) {
        dependencies { api(mainSourceSet.output) }
        configurations[sourceSet.runtimeOnlyConfigurationName]
            .extendsFrom(configurations.runtimeClasspath.get())
    }
    if (jpsKind != null) {
        // For Pill
        sourceSet.extra["jpsKind"] = jpsKind
    }
    return sourceSet to api
}

val (builtinsSourceSet, builtinsApi) = extraSourceSet("builtins", extendMain = false)
val (evaluateSourceSet, evaluateApi) = extraSourceSet("evaluate")
val (interpreterSourceSet, interpreterApi) = extraSourceSet("interpreter")
val (protobufSourceSet, protobufApi) = extraSourceSet("protobuf")
val (protobufCompareSourceSet, protobufCompareApi) = extraSourceSet("protobufCompare", jpsKind = SourceSet.TEST_SOURCE_SET_NAME)
val (wasmSourceSet, wasmApi) = extraSourceSet("wasm")
val (nativeInteropRuntimeSourceSet, nativeInteropRuntimeApi) = extraSourceSet("nativeInteropRuntime")

dependencies {
    api(kotlinStdlib("jdk8"))
    api(project(":core:util.runtime"))
    api(intellijPlatformUtil()) {
        exclude(module = "annotations")
    }

    builtinsApi("org.jetbrains.kotlin:kotlin-stdlib:$bootstrapKotlinVersion") { isTransitive = false }
    evaluateApi(project(":core:deserialization"))
    wasmApi(project(":wasm:wasm.ir"))
    wasmApi(kotlinStdlib())
    interpreterApi(project(":compiler:ir.tree"))
    interpreterApi(commonDependency("org.jetbrains.kotlin:kotlin-reflect"))
    protobufApi(kotlinStdlib())
    protobufCompareApi(projectTests(":kotlin-build-common"))
    nativeInteropRuntimeApi(kotlinStdlib())

    testApi(builtinsSourceSet.output)
    testApi(evaluateSourceSet.output)
    testApi(interpreterSourceSet.output)
    testApi(protobufSourceSet.output)
    testApi(protobufCompareSourceSet.output)

    testApi(project(":compiler:cli"))
    testApi(testFixtures(project(":compiler:incremental-compilation-impl")))
    testApi(testFixtures(project(":plugins:jvm-abi-gen")))
    testApi(testFixtures(project(":plugins:parcelize:parcelize-compiler")))
    testApi(testFixtures(project(":kotlin-annotation-processing-cli")))
    testApi(testFixtures(project(":kotlin-annotation-processing")))
    testApi(testFixtures(project(":kotlin-allopen-compiler-plugin")))
    testApi(testFixtures(project(":kotlin-noarg-compiler-plugin")))
    testApi(testFixtures(project(":kotlin-lombok-compiler-plugin")))
    testApi(testFixtures(project(":kotlin-power-assert-compiler-plugin")))
    testApi(testFixtures(project(":kotlin-sam-with-receiver-compiler-plugin")))
    testApi(testFixtures(project(":kotlin-assignment-compiler-plugin")))
    testApi(testFixtures(project(":kotlinx-serialization-compiler-plugin")))
    testApi(projectTests(":kotlin-atomicfu-compiler-plugin"))
    testApi(testFixtures(project(":kotlin-dataframe-compiler-plugin")))
    testImplementation(testFixtures(project(":analysis:analysis-api-impl-base")))
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testApi(testFixtures(project(":plugins:plugin-sandbox")))
    testApi(testFixtures(project(":plugins:plugin-sandbox:plugin-sandbox-ic-test")))
    testApi(testFixtures(project(":plugins:plugins-interactions-testing")))
    testApi(testFixtures(project(":generators:test-generator")))
    testApi(testFixtures(project(":generators:analysis-api-generator")))
    testApi(testFixtures(project(":plugins:scripting:scripting-tests")))
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testImplementation(project(":compiler:arguments"))
    testImplementation(project(":compiler:cli:cli-arguments-generator"))
    testImplementation(testFixtures(project(":compiler:test-infrastructure-utils")))
    testImplementation(testFixtures(project(":compiler:test-infrastructure")))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))
    testImplementation(testFixtures(project(":js:js.tests")))
    testImplementation(project(":kotlin-gradle-compiler-types"))
    testImplementation(project(":jps:jps-common"))
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}


projectTest(parallel = true) {
    workingDir = rootDir
}

val generateCompilerArgumentsCopy by generator("org.jetbrains.kotlin.generators.arguments.GenerateCompilerArgumentsCopyKt")

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateTestsKt") {
    dependsOn(":generators:analysis-api-generator:generateFrontendApiTests")
}

val generateProtoBuf by generator("org.jetbrains.kotlin.generators.protobuf.GenerateProtoBufKt", protobufSourceSet)
val generateProtoBufCompare by generator("org.jetbrains.kotlin.generators.protobuf.GenerateProtoBufCompare", protobufCompareSourceSet)

val generateGradleCompilerTypes by generator("org.jetbrains.kotlin.generators.arguments.GenerateGradleCompilerTypesKt") {
    description = "Generate Kotlin compiler arguments types Gradle representation"
}
val generateGradleOptions by generator("org.jetbrains.kotlin.generators.arguments.GenerateGradleOptionsKt") {
    dependsOn(generateGradleCompilerTypes)
    description = "Generate Gradle plugin compiler options"
}
val generateKeywordStrings by generator("org.jetbrains.kotlin.generators.frontend.GenerateKeywordStrings")

val generateBuiltins by generator("org.jetbrains.kotlin.generators.builtins.generateBuiltIns.GenerateBuiltInsKt", builtinsSourceSet)
val generateOperationsMap by generator("org.jetbrains.kotlin.generators.evaluate.GenerateOperationsMapKt", evaluateSourceSet)
val generateInterpreterMap by generator("org.jetbrains.kotlin.generators.interpreter.GenerateInterpreterMapKt", interpreterSourceSet)
val generateWasmIntrinsics by generator("org.jetbrains.kotlin.generators.wasm.WasmIntrinsicGeneratorKt", wasmSourceSet)

val generateNativeInteropRuntime by generator(
    "org.jetbrains.kotlin.generators.native.interopRuntime.NativeInteropRuntimeGeneratorKt",
    nativeInteropRuntimeSourceSet
)

testsJar()
