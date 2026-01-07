/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.keys.generator

import org.jetbrains.kotlin.config.keys.generator.model.KeysContainer
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.js.IncrementalNextRoundChecker
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.js.config.*
import java.io.File

@Suppress("unused")
object JsConfigurationKeysContainer : KeysContainer("org.jetbrains.kotlin.js.config", "JSConfigurationKeys") {
    val WASM_COMPILATION by key<Boolean>()

    val OUTPUT_NAME by key<String>("Name of output KLib file.")

    val LIBRARIES by key<List<String>>()

    val FRIEND_LIBRARIES by key<List<String>>()

    val SOURCE_MAP by key<Boolean>()

    val USE_DEBUGGER_CUSTOM_FORMATTERS by key<Boolean>()

    val ARTIFACT_CONFIGURATION by key<WebArtifactConfiguration>()

    val OUTPUT_DIR by key<File>()

    val SOURCE_MAP_PREFIX by key<String>("Prefix to add to paths in source map.")

    val SOURCE_MAP_SOURCE_ROOTS by key<List<String>>("Base directories used to calculate relative paths for source map.")

    val SOURCE_MAP_EMBED_SOURCES by key<SourceMapSourceEmbedding>("Embed source files into source map.")

    val SOURCEMAP_NAMES_POLICY by key<SourceMapNamesPolicy>("A policy to generate a mapping from generated identifiers to their corresponding original names.")

    val SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_FILES by key<Boolean>("Insert source mappings from libraries even if their sources are unavailable on the end-user machine.")

    val MODULE_KIND by key<ModuleKind>()

    val JS_INCREMENTAL_COMPILATION_ENABLED by key<Boolean>()

    val INCREMENTAL_DATA_PROVIDER by key<IncrementalDataProvider>(throwOnNull = false)

    val INCREMENTAL_RESULTS_CONSUMER by key<IncrementalResultsConsumer>(throwOnNull = false)

    val INCREMENTAL_NEXT_ROUND_CHECKER by key<IncrementalNextRoundChecker>(throwOnNull = false)

    val FRIEND_PATHS_DISABLED by key<Boolean>()

    val METADATA_ONLY by key<Boolean>()

    val DEVELOPER_MODE by key<Boolean>("Enable additional checkers.")

    val GENERATE_COMMENTS_WITH_FILE_PATH by key<Boolean>("Generate comments with file path at the start of each file block.")

    val GENERATE_POLYFILLS by key<Boolean>("Generate polyfills for newest properties, methods and classes from ES6+.")

    val DEFINE_PLATFORM_MAIN_FUNCTION_ARGUMENTS by key<String>("Provide platform-specific args as a parameter of the main function.")

    val GENERATE_DTS by key<Boolean>("Generate TypeScript definition file.")

    val COMPILE_SUSPEND_AS_JS_GENERATOR by key<Boolean>("Force suspend functions compilation in JS generator functions.")

    val COMPILE_LAMBDAS_AS_ES6_ARROW_FUNCTIONS by key<Boolean>("Lower Kotlin lambdas into arrow functions instead of anonymous functions.")

    val COMPILE_LONG_AS_BIGINT by key<Boolean>()

    val GENERATE_REGION_COMMENTS by key<Boolean>(
        "Generate special comments at the start and the end of each file block, it allows to fold them and navigate to them in IDEA."
    )

    val FILE_PATHS_PREFIX_MAP by key<Map<String, String>>(
        "This map is used to shorten/replace prefix of paths in comments with file paths, including region comments."
    )

    val PRINT_REACHABILITY_INFO by key<Boolean>("Print declarations' reachability info during performing DCE.")

    val DUMP_REACHABILITY_INFO_TO_FILE by key<String>(
        "Dump declarations' reachability info to file during performing DCE.", throwOnNull = false,
    )

    val FAKE_OVERRIDE_VALIDATOR by key<Boolean>()

    val PROPERTY_LAZY_INITIALIZATION by key<Boolean>()

    val GENERATE_INLINE_ANONYMOUS_FUNCTIONS by key<Boolean>("Translate lambdas into inline anonymous functions.")

    val GENERATE_STRICT_IMPLICIT_EXPORT by key<Boolean>("Enable strict implicitly exported entities types inside d.ts files.")

    val OPTIMIZE_GENERATED_JS by key<Boolean>()

    val USE_ES6_CLASSES by key<Boolean>()

    val INCLUDES by key<String>("List of KLibs for this linking phase.")
    val PRODUCE_KLIB_FILE by key<Boolean>("Need to produce KLib file or not.")
    val PRODUCE_KLIB_DIR by key<Boolean>("Need to produce unpacked KLib dir or not.")
    val PER_MODULE_OUTPUT_NAME by key<String>("Custom output name to the split .js files.", throwOnNull = false)

    val KEEP by key<List<String>>("List of fully qualified names not to be eliminated by DCE.")
    val DCE by key<Boolean>("Perform experimental dead code elimination.")
    val DCE_RUNTIME_DIAGNOSTIC by key<String>("Enable runtime diagnostics instead of removing declarations when performing DCE.")
    val SAFE_EXTERNAL_BOOLEAN by key<Boolean>("Wrap access to external 'Boolean' properties with an explicit conversion to 'Boolean'.")
    val SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC by key<String>("Enable runtime diagnostics when accessing external 'Boolean' properties.")
    val MINIMIZED_MEMBER_NAMES by key<Boolean>()
    val CALL_MAIN_MODE by key<String>("Specify whether the 'main' function should be called upon execution.")
    val IC_CACHE_DIRECTORY by key<String>(throwOnNull = false)
    val IC_CACHE_READ_ONLY by key<Boolean>()
    val PRESERVE_IC_ORDER by key<Boolean>()
}
