/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.options.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.types.ProfileCompilerCommand
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import kotlin.math.max
import kotlin.reflect.KClass

private const val MAX_SUPPORTED_VERSIONS_BACK = 3

internal const val IMPL_ARGUMENTS_PACKAGE = "org.jetbrains.kotlin.buildtools.internal.arguments"
internal const val API_PACKAGE = "org.jetbrains.kotlin.buildtools.api"
internal const val API_ARGUMENTS_PACKAGE = "$API_PACKAGE.arguments"
internal const val API_ENUMS_PACKAGE = "$API_ARGUMENTS_PACKAGE.enums"
internal const val API_TYPES_PACKAGE = "$API_ARGUMENTS_PACKAGE.types"

internal const val KOTLIN_IO_PATH = "kotlin.io.path"

internal val ANNOTATION_EXPERIMENTAL = ClassName(API_ARGUMENTS_PACKAGE, "ExperimentalCompilerArgument")
internal val ANNOTATION_USE_FROM_IMPL_RESTRICTED = ClassName("org.jetbrains.kotlin.buildtools.internal", "UseFromImplModuleRestricted")

internal const val KDOC_SINCE_2_3_0 = "@since 2.3.0"
internal const val KDOC_SINCE_2_4_0 = "@since 2.4.0"

internal val KDOC_BASE_OPTIONS_CLASS = """
    An option for configuring [%T].

    @see get
    @see set    
""".trimIndent()

internal val KDOC_OPTIONS_GET = """
    Get the value for option specified by [key] if it was previously [set] or if it has a default value.
    
    @return the previously set value for an option
    @throws IllegalStateException if the option was not set and has no default value
""".trimIndent()

internal val KDOC_OPTIONS_SET = """
    Set the [value] for option specified by [key], overriding any previous value for that option.
""".trimIndent()

internal val KDOC_OPTIONS_CONTAINS = """
    Check if an option specified by [key] has a value set.
    
    Note: trying to read an option (by using [get]) that has not been set will result in an exception.

    @return true if the option has a value set, false otherwise
""".trimIndent()

internal val experimentalLevelNames = listOf(
    CompilerArgumentsLevelNames.commonKlibBasedArguments,
    CompilerArgumentsLevelNames.jsArguments,
    CompilerArgumentsLevelNames.nativeArguments,
    CompilerArgumentsLevelNames.wasmArguments,
)

internal fun BtaCompilerArgument<*>.extractName(): String = name.uppercase().replace("-", "_").let {
    when {
        it.startsWith("XX") && it != "XX" -> it.replaceFirst("XX", "XX_")
        it.startsWith("X") && it != "X" -> it.replaceFirst("X", "X_")
        else -> it
    }
}

internal val customTypeAccessors = mutableSetOf(ProfileCompilerCommand::class)

internal fun KClass<*>.toBtaEnumClassName(): ClassName = ClassName(API_ENUMS_PACKAGE, simpleName!!)
internal fun KClass<*>.toBtaCustomClassName(): ClassName = ClassName(API_TYPES_PACKAGE, simpleName!!)

internal val KClass<*>?.isCustomType: Boolean get() = this in customTypeAccessors

internal val TypeName.isGeneratedEnum: Boolean get() = (this as? ClassName)?.packageName?.startsWith(API_ENUMS_PACKAGE) ?: false
internal val TypeName.isCustomType: Boolean get() = (this as? ClassName)?.copy(nullable = false) in customTypeAccessors.map { it.toBtaCustomClassName() }

internal fun createGeneratedFileAppendable(): StringBuilder = StringBuilder(GeneratorsFileUtil.GENERATED_MESSAGE_PREFIX)
    .appendLine("the README.md file").appendLine(GeneratorsFileUtil.GENERATED_MESSAGE_SUFFIX).appendLine()

internal fun getOldestSupportedVersion(kotlinVersion: KotlinReleaseVersion): KotlinReleaseVersion =
    KotlinReleaseVersion.entries.filter { it.patch == 0 }.let { majorVersionsReleases ->  // only get major releases
        majorVersionsReleases.indexOfFirst { it.major == kotlinVersion.major && it.minor == kotlinVersion.minor }.let {
            majorVersionsReleases[max(it - MAX_SUPPORTED_VERSIONS_BACK, 0)]
        }
    }

internal fun KotlinCompilerArgumentsLevel.isLeaf(): Boolean = nestedLevels.isEmpty()

internal val kotlinVersionType = ClassName(API_PACKAGE, "KotlinReleaseVersion")