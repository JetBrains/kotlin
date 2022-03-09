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
    api(intellijPlatformUtil()) {
        exclude(module = "annotations")
    }

    builtinsApi("org.jetbrains.kotlin:kotlin-stdlib:$bootstrapKotlinVersion") { isTransitive = false }
    evaluateApi(project(":core:deserialization"))
    wasmApi(project(":wasm:wasm.ir"))
    wasmApi(kotlinStdlib())
    interpreterApi(project(":compiler:ir.tree"))
    interpreterApi(project(":compiler:ir.tree.impl"))
    interpreterApi(project(":compiler:ir.psi2ir"))
    protobufApi(kotlinStdlib())
    protobufCompareApi(projectTests(":kotlin-build-common"))
    nativeInteropRuntimeApi(kotlinStdlib())

    testApi(builtinsSourceSet.output)
    testApi(evaluateSourceSet.output)
    testApi(interpreterSourceSet.output)
    testApi(protobufSourceSet.output)
    testApi(protobufCompareSourceSet.output)

    testApi(projectTests(":compiler:cli"))
    testApi(projectTests(":compiler:incremental-compilation-impl"))
    testApi(projectTests(":plugins:jvm-abi-gen"))
    testApi(projectTests(":plugins:android-extensions-compiler"))
    testApi(projectTests(":plugins:parcelize:parcelize-compiler"))
    testApi(projectTests(":kotlin-annotation-processing"))
    testApi(projectTests(":kotlin-annotation-processing-cli"))
    testApi(projectTests(":kotlin-allopen-compiler-plugin"))
    testApi(projectTests(":kotlin-noarg-compiler-plugin"))
    testApi(projectTests(":plugins:lombok:lombok-compiler-plugin"))
    testApi(projectTests(":kotlin-sam-with-receiver-compiler-plugin"))
    testApi(projectTests(":kotlinx-serialization-compiler-plugin"))
    testApi(projectTests(":kotlinx-atomicfu-compiler-plugin"))
    testApi(projectTests(":plugins:fir-plugin-prototype"))
    testApi(projectTests(":plugins:fir-plugin-prototype:fir-plugin-ic-test"))
    testApi(projectTests(":generators:test-generator"))
    testCompileOnly(project(":kotlin-reflect-api"))
    testImplementation(project(":kotlin-reflect"))
    testImplementation(projectTests(":compiler:test-infrastructure-utils"))
    testImplementation(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":js:js.tests"))
    testApiJUnit5()

    if (Ide.IJ()) {
        testCompileOnly(jpsBuildTest())
        testApi(jpsBuildTest())
    }
}


projectTest(parallel = true) {
    workingDir = rootDir
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateTestsKt") {
    dependsOn(":generators:analysis-api-generator:generateFrontendApiTests")
}

val generateProtoBuf by generator("org.jetbrains.kotlin.generators.protobuf.GenerateProtoBufKt", protobufSourceSet)
val generateProtoBufCompare by generator("org.jetbrains.kotlin.generators.protobuf.GenerateProtoBufCompare", protobufCompareSourceSet)

val generateGradleOptions by generator("org.jetbrains.kotlin.generators.arguments.GenerateGradleOptionsKt")
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
