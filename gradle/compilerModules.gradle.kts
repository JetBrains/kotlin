/**
 * Common modules, used by K1 frontend, K2 frontend, backends, AA and CLI
 */
val commonCompilerModules = arrayOf(
    ":compiler:psi:psi-api",
    ":compiler:psi:psi-impl",
    ":compiler:psi:psi-utils",
    ":compiler:psi:psi-frontend-utils",
    ":compiler:psi:parser",
    ":compiler:frontend.common-psi",
    ":compiler:frontend.common",
    ":compiler:util",
    ":compiler:config",
    ":compiler:config.jvm",
    ":compiler:compiler.version",
    ":compiler:resolution.common",
    ":compiler:resolution.common.jvm",
    ":compiler:backend.common.jvm",
    ":compiler:plugin-api",
    ":core:metadata",
    ":core:metadata.jvm",
    ":core:deserialization.common",
    ":core:deserialization.common.jvm",
    ":core:compiler.common",
    ":core:compiler.common.jvm",
    ":core:compiler.common.js",
    ":core:compiler.common.native",
    ":core:compiler.common.wasm",
    ":core:compiler.common.web",
    ":core:util.runtime",
    ":core:names",
    ":core:language.model",
    ":core:language.targets",
    ":core:language.targets.jvm",
    ":core:language.version-settings",
    ":compiler:frontend.common.jvm",
    ":kotlin-util-io",
    ":kotlin-util-klib",
    ":kotlin-util-klib-abi",
    ":compiler:build-tools:kotlin-build-statistics",
    ":js:js.config",
    ":js:js.frontend.common",
    ":js:js.ast", // used by js fir checkers and js backend
    ":wasm:wasm.config",
    ":native:base",
    ":native:native.config",
    ":native:kotlin-native-utils",
).also { extra["commonCompilerModules"] = it }

/**
 * Modules of K2 (FIR) frontend
 */
val firCompilerModules = arrayOf(
    ":compiler:fir:cones",
    ":compiler:fir:providers",
    ":compiler:fir:semantics",
    ":compiler:fir:resolve",
    ":compiler:fir:fir-serialization",
    ":compiler:fir:fir-deserialization",
    ":compiler:fir:plugin-utils",
    ":compiler:fir:tree",
    ":compiler:fir:fir-jvm",
    ":compiler:fir:fir-js",
    ":compiler:fir:fir-native",
    ":compiler:fir:raw-fir:raw-fir.common",
    ":compiler:fir:raw-fir:psi2fir",
    ":compiler:fir:raw-fir:light-tree2fir",
    ":compiler:fir:checkers",
    ":compiler:fir:checkers:checkers.jvm",
    ":compiler:fir:checkers:checkers.js",
    ":compiler:fir:checkers:checkers.native",
    ":compiler:fir:checkers:checkers.wasm",
    ":compiler:fir:checkers:checkers.web.common",
    ":compiler:fir:diagnostic-renderers",
    ":compiler:fir:entrypoint", // TODO should not be in core modules but FIR IDE uses DependencyListForCliModule from this module
    ":compiler:fir:fir2ir:jvm-backend",  // TODO should not be in core modules but FIR IDE uses Fir2IrSignatureComposer from this module
    ":compiler:fir:fir2ir", // TODO should not be in core modules but FIR IDE uses Fir2IrSignatureComposer from this module
).also { extra["firCompilerModules"] = it }

/**
 * Modules of K1 frontend
 */
val fe10CompilerModules = arrayOf(
    ":compiler",
    ":core:descriptors.runtime",
    ":core:descriptors",
    ":core:descriptors.jvm",
    ":compiler:light-classes",
    ":compiler:resolution",
    ":compiler:serialization",
    ":compiler:frontend",
    ":compiler:frontend.java",
    ":compiler:container",
    ":core:deserialization",
    ":compiler:frontend:cfg",
    ":compiler:ir.psi2ir",
    ":kotlin-util-klib-metadata",
    ":compiler:javac-wrapper",
    ":js:js.frontend",
    ":native:frontend.native",
    ":wasm:wasm.frontend",
    ":compiler:backend.common.jvm",
).also { extra["fe10CompilerModules"] = it }

/**
 * Modules of IR-based backends
 */
val irCompilerModules = arrayOf(
    ":compiler:ir.tree",
    ":compiler:ir.serialization.common",
    ":compiler:ir.backend.common",
    ":compiler:ir.actualization",
    ":compiler:ir.interpreter",
    ":compiler:ir.inline",
    ":compiler:ir.validation",
).also { extra["irCompilerModules"] = it }

/**
 * Modules of JVM backend
 */
val jvmCompilerModules = arrayOf(
    ":compiler:backend",
    ":compiler:backend.jvm",
    ":compiler:backend.jvm.lower",
    ":compiler:backend.jvm.codegen",
    ":compiler:backend.jvm.entrypoint",
    ":compiler:ir.serialization.jvm",
).also { extra["jvmCompilerModules"] = it }

/**
 * Modules of JS backend
 */
val jsCompilerModules = arrayOf(
    ":compiler:backend.js",
    ":js:js.sourcemap",
    ":js:js.serializer",
    ":js:js.parser",
    ":js:js.translator",
    ":js:typescript-export-model",
    ":js:typescript-printer",
    ":compiler:ir.serialization.js",
).also { extra["jsCompilerModules"] = it }

/**
 * Modules of Wasm backend
 */
val wasmCompilerModules = arrayOf(
    ":compiler:backend.wasm",
    ":wasm:wasm.ir",
).also { extra["wasmCompilerModules"] = it }

/**
 * Modules of Native backend
 */
val nativeCompilerModules = arrayOf(
    ":compiler:ir.backend.native",
    ":compiler:ir.serialization.native",
    ":compiler:ir.objcinterop",
    ":native:binary-options",
).also { extra["nativeCompilerModules"] = it }

/**
 * Modules of IR-based backends used in IDE
 */
val irCompilerModulesForIDE = arrayOf(
    ":compiler:ir.tree",
    ":compiler:ir.serialization.common",
    ":compiler:ir.serialization.jvm",
    ":compiler:ir.serialization.js", // used in IJ android plugin in `ComposeIrGenerationExtension`
    ":compiler:ir.objcinterop",
    ":compiler:ir.backend.common",
    ":compiler:ir.backend.native",
    ":compiler:ir.actualization",
    ":compiler:ir.interpreter",
    ":compiler:ir.inline",
    ":compiler:ir.validation",
).also { extra["irCompilerModulesForIDE"] = it }

val cliCompilerModules = arrayOf(
    ":compiler:arguments.common",
    ":compiler:cli-base",
    ":compiler:cli",
    ":compiler:cli-jvm",
    ":compiler:cli-js",
    ":compiler:cli-metadata",
    ":compiler:incremental-compilation-impl",
    ":kotlin-build-common",
).also { extra["cliCompilerModules"] = it }

val analysisApiModules = arrayOf(
    ":analysis:analysis-api",
    ":analysis:analysis-api-fe10",
    ":analysis:analysis-api-fir",
    ":analysis:analysis-api-impl-base",
    ":analysis:analysis-api-platform-interface",
    ":analysis:analysis-api-standalone:analysis-api-standalone-base",
    ":analysis:analysis-api-standalone:analysis-api-fir-standalone-base",
    ":analysis:analysis-api-standalone",
    ":analysis:analysis-test-framework",
    ":analysis:kt-references",
    ":analysis:light-classes-base",
    ":analysis:low-level-api-fir",
    ":analysis:stubs",
    ":analysis:symbol-light-classes",
    ":analysis:analysis-internal-utils",
    ":analysis:light-classes-base",
    ":analysis:decompiled",
    ":analysis:decompiled:decompiler-to-stubs",
    ":analysis:decompiled:decompiler-to-file-stubs",
    ":analysis:decompiled:decompiler-js",
    ":analysis:decompiled:decompiler-native",
    ":analysis:decompiled:decompiler-to-psi",
    ":analysis:decompiled:light-classes-for-decompiled",
    ":analysis:kt-references",
).also { extra["analysisApiModules"] = it }

extra["compilerModules"] =
    commonCompilerModules +
    fe10CompilerModules +
    firCompilerModules +
    irCompilerModules +
    jvmCompilerModules +
    jsCompilerModules +
    wasmCompilerModules +
    nativeCompilerModules +
    cliCompilerModules +
    ":analysis:light-classes-base"

/**
 * An array of projects used in the IntelliJ Kotlin Plugin.
 *
 * Experimental declarations from Kotlin stdlib cannot be used in those projects to avoid stdlib binary compatibility problems.
 * See KT-62510 for details.
 */
val projectsUsedInIntelliJKotlinPlugin =
    fe10CompilerModules +
            commonCompilerModules +
            firCompilerModules +
            irCompilerModulesForIDE +
            analysisApiModules +
            cliCompilerModules +
            jvmCompilerModules + // used by K1 plugin
            arrayOf(
                ":js:js.serializer",
                ":native:binary-options",

                ":kotlin-allopen-compiler-plugin.cli",
                ":kotlin-allopen-compiler-plugin.common",
                ":kotlin-allopen-compiler-plugin.k1",
                ":kotlin-allopen-compiler-plugin.k2",

                ":kotlin-assignment-compiler-plugin.cli",
                ":kotlin-assignment-compiler-plugin.common",
                ":kotlin-assignment-compiler-plugin.k1",
                ":kotlin-assignment-compiler-plugin.k2",

                ":plugins:parcelize:parcelize-compiler:parcelize.backend",
                ":plugins:parcelize:parcelize-compiler:parcelize.cli",
                ":plugins:parcelize:parcelize-compiler:parcelize.common",
                ":plugins:parcelize:parcelize-compiler:parcelize.k1",
                ":plugins:parcelize:parcelize-compiler:parcelize.k2",
                ":plugins:parcelize:parcelize-runtime",

                ":plugins:compose-compiler-plugin:compiler-hosted",

                ":kotlin-sam-with-receiver-compiler-plugin.cli",
                ":kotlin-sam-with-receiver-compiler-plugin.common",
                ":kotlin-sam-with-receiver-compiler-plugin.k1",
                ":kotlin-sam-with-receiver-compiler-plugin.k2",

                ":kotlinx-serialization-compiler-plugin.cli",
                ":kotlinx-serialization-compiler-plugin.common",
                ":kotlinx-serialization-compiler-plugin.k1",
                ":kotlinx-serialization-compiler-plugin.k2",
                ":kotlinx-serialization-compiler-plugin.backend",

                ":plugins:js-plain-objects:compiler-plugin:js-plain-objects.cli",
                ":plugins:js-plain-objects:compiler-plugin:js-plain-objects.common",
                ":plugins:js-plain-objects:compiler-plugin:js-plain-objects.k2",
                ":plugins:js-plain-objects:compiler-plugin:js-plain-objects.backend",

                ":kotlin-lombok-compiler-plugin.cli",
                ":kotlin-lombok-compiler-plugin.common",
                ":kotlin-lombok-compiler-plugin.k1",
                ":kotlin-lombok-compiler-plugin.k2",

                ":kotlin-noarg-compiler-plugin.cli",
                ":kotlin-noarg-compiler-plugin.common",
                ":kotlin-noarg-compiler-plugin.k1",
                ":kotlin-noarg-compiler-plugin.k2",
                ":kotlin-noarg-compiler-plugin.backend",

                ":kotlin-sam-with-receiver-compiler-plugin.cli",
                ":kotlin-sam-with-receiver-compiler-plugin.common",
                ":kotlin-sam-with-receiver-compiler-plugin.k1",
                ":kotlin-sam-with-receiver-compiler-plugin.k2",

                ":kotlin-dataframe-compiler-plugin.cli",
                ":kotlin-dataframe-compiler-plugin.common",
                ":kotlin-dataframe-compiler-plugin.k2",
                ":kotlin-dataframe-compiler-plugin.backend",

                ":kotlin-compiler-runner-unshaded",
                ":kotlin-preloader",
                ":daemon-common",
                ":kotlin-daemon-client",

                ":kotlin-scripting-jvm",

                ":kotlin-scripting-compiler",
                ":kotlin-gradle-statistics",
                ":jps:jps-common",
                ":compiler:build-tools:kotlin-build-tools-api",
            ) +
            arrayOf(
                ":compiler:ir.serialization.native",
                ":libraries:tools:analysis-api-based-klib-reader",
                ":native:base",
                ":native:objcexport-header-generator",
                ":native:objcexport-header-generator-analysis-api",
                ":native:objcexport-header-generator-k1",
                ":native:analysis-api-based-export-common",
            ) +
            arrayOf(
                ":native:swift:sir",
                ":native:swift:sir-light-classes",
                ":native:swift:sir-printer",
                ":native:swift:sir-providers",
                ":native:swift:swift-export-ide",
            ) +
            arrayOf(
                ":analysis:analysis-tools:deprecated-k1-frontend-internals-for-ide-generated",
            )

/**
 * In all specified modules `-XXexplicit-return-types` flag will be added to warn about
 *   not specified return types for public declarations
 */
val modulesWithRequiredExplicitTypes: Array<String> by extra {
    firCompilerModules + arrayOf(
        ":compiler:fir:analysis-tests",
        ":compiler:fir:analysis-tests:legacy-fir-tests"
    )
}

extra["projectsUsedInIntelliJKotlinPlugin"] = projectsUsedInIntelliJKotlinPlugin

// They are embedded just because we don't publish those dependencies as separate Maven artifacts (yet)
extra["kotlinJpsPluginEmbeddedDependencies"] = listOf(
    ":compiler:cli-base",
    ":kotlin-build-tools-enum-compat",
    ":kotlin-compiler-runner-unshaded",
    ":daemon-common",
    ":core:names",
    ":core:language.model",
    ":core:language.targets",
    ":core:language.targets.jvm",
    ":core:language.version-settings",
    ":core:compiler.common",
    ":core:compiler.common.jvm",
    ":core:compiler.common.js",
    ":core:compiler.common.native",
    ":core:compiler.common.wasm",
    ":core:compiler.common.web",
    ":core:descriptors",
    ":core:descriptors.jvm",
    ":compiler:backend.common.jvm",
    ":js:js.serializer",
    ":core:deserialization",
    ":core:deserialization.common",
    ":core:deserialization.common.jvm",
    ":compiler:frontend.common.jvm",
    ":compiler:frontend.java",
    ":core:metadata",
    ":core:metadata.jvm",
    ":kotlin-preloader",
    ":compiler:util",
    ":compiler:config",
    ":compiler:config.jvm",
    ":js:js.config",
    ":wasm:wasm.config",
    ":native:native.config",
    ":core:util.runtime",
    ":compiler:compiler.version",
    ":compiler:build-tools:kotlin-build-statistics",
    ":kotlin-build-common",
    ":compiler:arguments.common",
)

extra["kotlinJpsPluginMavenDependencies"] = listOf(
    ":kotlin-daemon-client",
    ":kotlin-util-io",
    ":kotlin-util-klib",
    ":kotlin-util-klib-metadata",
    ":native:kotlin-native-utils",
    ":compiler:build-tools:kotlin-build-tools-api",
)

extra["compilerArtifactsForIde"] = listOfNotNull(
    ":prepare:ide-plugin-dependencies:allopen-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:compose-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:js-plain-objects-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:scripting-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:incremental-compilation-impl-tests-for-ide",
    ":prepare:ide-plugin-dependencies:js-ir-runtime-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-build-common-tests-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-cli-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-gradle-statistics-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-jps-common-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-jps-plugin-classpath",
    ":prepare:ide-plugin-dependencies:kotlin-jps-plugin-tests-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-jps-plugin-testdata-for-ide",
    ":prepare:ide-plugin-dependencies:kotlinx-serialization-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:noarg-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:sam-with-receiver-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:assignment-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:parcelize-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:lombok-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-dataframe-compiler-plugin-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-objcexport-header-generator-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-swift-export-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-tests-for-ide",
    ":prepare:ide-plugin-dependencies:low-level-api-fir-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-impl-base-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-k2-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-fe10-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-platform-interface-for-ide",
    ":prepare:ide-plugin-dependencies:symbol-light-classes-for-ide",
    ":prepare:ide-plugin-dependencies:analysis-api-standalone-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-ir-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-common-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-fe10-for-ide",
    ":prepare:ide-plugin-dependencies:kotlin-compiler-fir-for-ide",
    ":prepare:kotlin-jps-plugin",
    ":kotlin-script-runtime",
    ":kotlin-scripting-common",
    ":kotlin-scripting-dependencies",
    ":kotlin-scripting-jvm",
    ":kotlin-scripting-compiler",
    ":kotlin-scripting-compiler-impl",
    ":plugins:parcelize:parcelize-runtime",
    ":plugins:jvm-abi-gen",
    ":kotlin-stdlib-common",
    ":kotlin-stdlib",
    ":kotlin-test",
    ":kotlin-daemon",
    ":kotlin-compiler",
    ":kotlin-annotations-jvm",
    ":kotlin-stdlib-jdk7",
    ":kotlin-stdlib-jdk8",
    ":kotlin-reflect",
    ":kotlin-main-kts",
    ":kotlin-dom-api-compat",
    ":compiler:build-tools:kotlin-build-tools-api",
    ":compiler:build-tools:kotlin-build-tools-impl",
    ":compiler:build-tools:kotlin-build-tools-cri-impl",
)
