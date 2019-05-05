plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:ir.psi2ir"))
    compile(project(":compiler:ir.serialization.common"))
    compile(project(":js:js.frontend"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    testCompile(projectTests(":compiler:tests-common"))
    testCompileOnly(project(":compiler:frontend"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":compiler:util"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val unimplementedNativeBuiltIns =
    (file("$rootDir/core/builtins/native/kotlin/").list().toSet() - file("$rootDir/libraries/stdlib/js-ir/builtins/").list())
        .map { "core/builtins/native/kotlin/$it" }

// Required to compile native builtins with the rest of runtime
val builtInsHeader = """@file:Suppress(
    "NON_ABSTRACT_FUNCTION_WITH_NO_BODY",
    "MUST_BE_INITIALIZED_OR_BE_ABSTRACT",
    "EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE",
    "PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED",
    "WRONG_MODIFIER_TARGET"
)
"""

val fullRuntimeSources by task<Sync> {

    val sources = listOf(
        "core/builtins/src/kotlin/",
        "libraries/stdlib/common/src/",
        "libraries/stdlib/src/kotlin/",
        "libraries/stdlib/unsigned/",
        "libraries/stdlib/js/src/",
        "libraries/stdlib/js/runtime/",
        "libraries/stdlib/js-ir/builtins/",
        "libraries/stdlib/js-ir/src/",
        "libraries/stdlib/js-ir/runtime/",

        // TODO get rid - move to test module
        "js/js.translator/testData/_commonFiles/"
    ) + unimplementedNativeBuiltIns

    val excluded = listOf(
        // stdlib/js/src/generated is used exclusively for current `js-v1` backend.
        "libraries/stdlib/js/src/generated/**",

        // JS-specific optimized version of emptyArray() already defined
        "core/builtins/src/kotlin/ArrayIntrinsics.kt"
    )

    sources.forEach { path ->
        from("$rootDir/$path") {
            into(path.dropLastWhile { it != '/' })
            excluded.filter { it.startsWith(path) }.forEach {
                exclude(it.substring(path.length))
            }
        }
    }

    into("$buildDir/fullRuntime/src")

    doLast {
        unimplementedNativeBuiltIns.forEach { path ->
            val file = File("$buildDir/fullRuntime/src/$path")
            val sourceCode = builtInsHeader + file.readText()
            file.writeText(sourceCode)
        }
    }
}

val reducedRuntimeSources by task<Sync> {
    dependsOn(fullRuntimeSources)

    from(fullRuntimeSources.outputs.files.singleFile) {
        exclude(
            listOf(
                "libraries/stdlib/unsigned/**",
                "libraries/stdlib/common/src/generated/_Arrays.kt",
                "libraries/stdlib/common/src/generated/_Collections.kt",
                "libraries/stdlib/common/src/generated/_Comparisons.kt",
                "libraries/stdlib/common/src/generated/_Maps.kt",
                "libraries/stdlib/common/src/generated/_Sequences.kt",
                "libraries/stdlib/common/src/generated/_Sets.kt",
                "libraries/stdlib/common/src/generated/_Strings.kt",
                "libraries/stdlib/common/src/generated/_UArrays.kt",
                "libraries/stdlib/common/src/generated/_URanges.kt",
                "libraries/stdlib/common/src/generated/_UCollections.kt",
                "libraries/stdlib/common/src/generated/_UComparisons.kt",
                "libraries/stdlib/common/src/generated/_USequences.kt",
                "libraries/stdlib/common/src/kotlin/SequencesH.kt",
                "libraries/stdlib/common/src/kotlin/TextH.kt",
                "libraries/stdlib/common/src/kotlin/UMath.kt",
                "libraries/stdlib/common/src/kotlin/collections/**",
                "libraries/stdlib/common/src/kotlin/ioH.kt",
                "libraries/stdlib/js-ir/runtime/collectionsHacks.kt",
                "libraries/stdlib/js-ir/src/generated/**",
                "libraries/stdlib/js-ir/src/kotlin/text/**",
                "libraries/stdlib/js/src/jquery/**",
                "libraries/stdlib/js/src/org.w3c/**",
                "libraries/stdlib/js/src/kotlin/char.kt",
                "libraries/stdlib/js/src/kotlin/collections.kt",
                "libraries/stdlib/js/src/kotlin/collections/**",
                "libraries/stdlib/js/src/kotlin/console.kt",
                "libraries/stdlib/js/src/kotlin/coreDeprecated.kt",
                "libraries/stdlib/js/src/kotlin/date.kt",
                "libraries/stdlib/js/src/kotlin/debug.kt",
                "libraries/stdlib/js/src/kotlin/grouping.kt",
                "libraries/stdlib/js/src/kotlin/json.kt",
                "libraries/stdlib/js/src/kotlin/promise.kt",
                "libraries/stdlib/js/src/kotlin/regexp.kt",
                "libraries/stdlib/js/src/kotlin/sequence.kt",
                "libraries/stdlib/js/src/kotlin/text/**",
                "libraries/stdlib/src/kotlin/collections/**",
                "libraries/stdlib/src/kotlin/experimental/bitwiseOperations.kt",
                "libraries/stdlib/src/kotlin/properties/Delegates.kt",
                "libraries/stdlib/src/kotlin/random/URandom.kt",
                "libraries/stdlib/src/kotlin/text/**",
                "libraries/stdlib/src/kotlin/util/KotlinVersion.kt",
                "libraries/stdlib/src/kotlin/util/Tuples.kt",
                "libraries/stdlib/js/src/kotlin/dom/**",
                "libraries/stdlib/js/src/kotlin/browser/**"
            )
        )
    }

    from("$rootDir/libraries/stdlib/js-ir/smallRuntime") {
        into("libraries/stdlib/js-ir/runtime/")
    }

    into("$buildDir/reducedRuntime/src")
}


fun JavaExec.buildKLib(sources: List<String>, dependencies: List<String>, outPath: String, commonSources: List<String>) {
    inputs.files(sources)
    outputs.dir(file(outPath).parent)

    classpath = sourceSets.test.get().runtimeClasspath
    main = "org.jetbrains.kotlin.ir.backend.js.GenerateIrRuntimeKt"
    workingDir = rootDir
    args = sources.toList() + listOf("-o", outPath) + dependencies.flatMap { listOf("-d", it) } + commonSources.flatMap { listOf("-c", it) }

    passClasspathInJar()
}

val generateFullRuntimeKLib by task<NoDebugJavaExec> {
    dependsOn(fullRuntimeSources)

    buildKLib(sources = listOf(fullRuntimeSources.outputs.files.singleFile.path),
              dependencies = emptyList(),
              outPath = "$buildDir/fullRuntime/klib/JS_IR_RUNTIME.klm",
              commonSources = listOf("common", "src", "unsigned").map { "$buildDir/fullRuntime/src/libraries/stdlib/$it" }
    )
}

val generateReducedRuntimeKLib by task<NoDebugJavaExec> {
    dependsOn(reducedRuntimeSources)

    buildKLib(sources = listOf(reducedRuntimeSources.outputs.files.singleFile.path),
              dependencies = emptyList(),
              outPath = "$buildDir/reducedRuntime/klib/JS_IR_RUNTIME.klm",
              commonSources = listOf("common", "src", "unsigned").map { "$buildDir/reducedRuntime/src/libraries/stdlib/$it" }
    )
}

val kotlinTestCommonSources = listOf(
    "$rootDir/libraries/kotlin.test/annotations-common/src/main",
    "$rootDir/libraries/kotlin.test/common/src/main"
)
val generateKotlinTestKLib by task<NoDebugJavaExec> {
    dependsOn(generateFullRuntimeKLib)

    buildKLib(
        sources = listOf("$rootDir/libraries/kotlin.test/js/src/main") + kotlinTestCommonSources,
        dependencies = listOf("${generateFullRuntimeKLib.outputs.files.singleFile.path}/JS_IR_RUNTIME.klm"),
        outPath = "$buildDir/kotlin.test/klib/kotlin.test.klm",
        commonSources = kotlinTestCommonSources
    )
}

testsJar {}
