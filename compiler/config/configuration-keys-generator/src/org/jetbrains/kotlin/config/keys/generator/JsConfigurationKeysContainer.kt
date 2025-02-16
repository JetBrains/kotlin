/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.keys.generator

import org.jetbrains.kotlin.config.keys.generator.model.KeysContainer
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.js.IncrementalNextRoundChecker
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.backend.js.JsGenerationGranularity
import org.jetbrains.kotlin.backend.js.TsCompilationStrategy
import org.jetbrains.kotlin.js.config.EcmaVersion
import org.jetbrains.kotlin.js.config.SourceMapNamesPolicy
import org.jetbrains.kotlin.js.config.SourceMapSourceEmbedding
import org.jetbrains.kotlin.konan.file.ZipFileSystemAccessor
import org.jetbrains.kotlin.serialization.js.ModuleKind
import java.io.File

@Suppress("unused")
object JsConfigurationKeysContainer : KeysContainer("org.jetbrains.kotlin.js.config", "JSConfigurationKeys") {
    val WASM_COMPILATION by key<Boolean>("compile to WASM")

    val OUTPUT_NAME by key<String>("Name of output KLib file")

    val TRANSITIVE_LIBRARIES by key<List<String>>("library files for transitive dependencies")

    val LIBRARIES by key<List<String>>("library file paths")

    val FRIEND_LIBRARIES by key<List<String>>("friend library file paths")

    val SOURCE_MAP by key<Boolean>("generate source map")

    val USE_DEBUGGER_CUSTOM_FORMATTERS by key<Boolean>("add import of debugger custom formatters")

    val OUTPUT_DIR by key<File>("output directory")

    val SOURCE_MAP_PREFIX by key<String>("prefix to add to paths in source map")

    val SOURCE_MAP_SOURCE_ROOTS by key<List<String>>("base directories used to calculate relative paths for source map")

    val SOURCE_MAP_EMBED_SOURCES by key<SourceMapSourceEmbedding>("embed source files into source map")

    val SOURCEMAP_NAMES_POLICY by key<SourceMapNamesPolicy>("a policy to generate a mapping from generated identifiers to their corresponding original names")

    val SOURCE_MAP_INCLUDE_MAPPINGS_FROM_UNAVAILABLE_FILES by key<Boolean>("insert source mappings from libraries even if their sources are unavailable on the end-user machine")

    val META_INFO by key<Boolean>("generate .meta.js and .kjsm files")

    val TARGET by key<EcmaVersion>("ECMA version target")

    val MODULE_KIND by key<ModuleKind>("module kind")

    val JS_INCREMENTAL_COMPILATION_ENABLED by key<Boolean>("incremental compilation enabled")

    val INCREMENTAL_DATA_PROVIDER by key<IncrementalDataProvider>("incremental data provider", throwOnNull = false)

    val INCREMENTAL_RESULTS_CONSUMER by key<IncrementalResultsConsumer>("incremental results consumer", throwOnNull = false)

    val INCREMENTAL_NEXT_ROUND_CHECKER by key<IncrementalNextRoundChecker>("incremental compilation next round checker", throwOnNull = false)

    val FRIEND_PATHS_DISABLED by key<Boolean>("disable support for friend paths")

    val FRIEND_PATHS by key<List<String>>("friend module paths")

    val METADATA_ONLY by key<Boolean>("generate .meta.js and .kjsm files only")

    val DEVELOPER_MODE by key<Boolean>("enables additional checkers")

    val GENERATE_COMMENTS_WITH_FILE_PATH by key<Boolean>("generate comments with file path at the start of each file block")

    val GENERATE_POLYFILLS by key<Boolean>("generate polyfills for newest properties, methods and classes from ES6+")

    val DEFINE_PLATFORM_MAIN_FUNCTION_ARGUMENTS by key<String>("provide platform specific args as a parameter of the main function")

    val GENERATE_DTS by key<Boolean>("generate TypeScript definition file")

    val COMPILE_SUSPEND_AS_JS_GENERATOR by key<Boolean>("force suspend functions compilation int JS generator functions")

    val COMPILE_LAMBDAS_AS_ES6_ARROW_FUNCTIONS by key<Boolean>("lower Kotlin lambdas into arrow functions instead of anonymous functions")

    val GENERATE_REGION_COMMENTS by key<Boolean>(
        "generate special comments at the start and the end of each file block, it allows to fold them and navigate to them in the IDEA"
    )

    val FILE_PATHS_PREFIX_MAP by key<Map<String, String>>(
        "this map used to shorten/replace prefix of paths in comments with file paths, including region comments"
    )

    val PRINT_REACHABILITY_INFO by key<Boolean>("print declarations' reachability info during performing DCE")

    val DUMP_REACHABILITY_INFO_TO_FILE by key<String>("dump declarations' reachability info to file during performing DCE", throwOnNull = false)

    val FAKE_OVERRIDE_VALIDATOR by key<Boolean>("IR fake override validator")

    val PROPERTY_LAZY_INITIALIZATION by key<Boolean>("perform lazy initialization for properties")

    val GENERATE_INLINE_ANONYMOUS_FUNCTIONS by key<Boolean>("translate lambdas into in-line anonymous functions")

    val GENERATE_STRICT_IMPLICIT_EXPORT by key<Boolean>("enable strict implicitly exported entities types inside d.ts files")

    val ZIP_FILE_SYSTEM_ACCESSOR by key<ZipFileSystemAccessor>("zip file system accessor, used for klib reading")

    val OPTIMIZE_GENERATED_JS by key<Boolean>("perform additional optimizations on the generated JS code")

    val USE_ES6_CLASSES by key<Boolean>("perform ES6 class usage")

    val INCLUDES by key<String>("List of KLibs for this linking phase")
    val PRODUCE_KLIB_FILE by key<Boolean>("Need to produce KLib file or not")
    val PRODUCE_KLIB_DIR by key<Boolean>("Need to produce unpacked KLib dir or not")
    val PER_MODULE_OUTPUT_NAME by key<String>("Custom output name to the split .js files", throwOnNull = false)

    val KEEP by key<List<String>>("list of fully qualified names not to be eliminated by DCE")
    val DCE by key<Boolean>("Perform experimental dead code elimination")
    val DCE_RUNTIME_DIAGNOSTIC by key<String>("Enable runtime diagnostics instead of removing declarations when performing DCE")
    val SAFE_EXTERNAL_BOOLEAN by key<Boolean>("Wrap access to external 'Boolean' properties with an explicit conversion to 'Boolean'")
    val SAFE_EXTERNAL_BOOLEAN_DIAGNOSTIC by key<String>("Enable runtime diagnostics when accessing external 'Boolean' properties")
    val MINIMIZED_MEMBER_NAMES by key<Boolean>("Minimize the names of members")
    val GRANULARITY by key<JsGenerationGranularity>("Granularity of JS files generation")
    val TS_COMPILATION_STRATEGY by key<TsCompilationStrategy>("TS compilation strategy")
    val CALL_MAIN_MODE by key<String>("Specify whether the 'main' function should be called upon execution.")
    val IC_CACHE_DIRECTORY by key<String>("Directory for the IC cache", throwOnNull = false)
    val IC_CACHE_READ_ONLY by key<Boolean>("IC caches are read-only")
    val PRESERVE_IC_ORDER by key<Boolean>("Preserve IC order")
}
