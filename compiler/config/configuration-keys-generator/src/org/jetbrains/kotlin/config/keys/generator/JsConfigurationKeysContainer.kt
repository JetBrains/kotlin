/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.keys.generator

import org.jetbrains.kotlin.config.keys.generator.model.KeysContainer
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.js.IncrementalNextRoundChecker
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.js.config.EcmaVersion
import org.jetbrains.kotlin.js.config.SourceMapNamesPolicy
import org.jetbrains.kotlin.js.config.SourceMapSourceEmbedding
import org.jetbrains.kotlin.konan.file.ZipFileSystemAccessor
import org.jetbrains.kotlin.serialization.js.ModuleKind
import java.io.File

@Suppress("unused")
object JsConfigurationKeysContainer : KeysContainer("org.jetbrains.kotlin.js.config", "JSConfigurationKeys") {
    val TRANSITIVE_LIBRARIES by key<List<String>>("library files for transitive dependencies")

    val LIBRARIES by key<List<String>>("library file paths")

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

    val INCREMENTAL_DATA_PROVIDER by key<IncrementalDataProvider>("incremental data provider")

    val INCREMENTAL_RESULTS_CONSUMER by key<IncrementalResultsConsumer>("incremental results consumer")

    val INCREMENTAL_NEXT_ROUND_CHECKER by key<IncrementalNextRoundChecker>("incremental compilation next round checker")

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

    val DUMP_REACHABILITY_INFO_TO_FILE by key<String>("dump declarations' reachability info to file during performing DCE")

    val FAKE_OVERRIDE_VALIDATOR by key<Boolean>("IR fake override validator")

    val PROPERTY_LAZY_INITIALIZATION by key<Boolean>("perform lazy initialization for properties")

    val GENERATE_INLINE_ANONYMOUS_FUNCTIONS by key<Boolean>("translate lambdas into in-line anonymous functions")

    val GENERATE_STRICT_IMPLICIT_EXPORT by key<Boolean>("enable strict implicitly exported entities types inside d.ts files")

    val ZIP_FILE_SYSTEM_ACCESSOR by key<ZipFileSystemAccessor>("zip file system accessor, used for klib reading")

    val OPTIMIZE_GENERATED_JS by key<Boolean>("perform additional optimizations on the generated JS code")

    val USE_ES6_CLASSES by key<Boolean>("perform ES6 class usage")
}
