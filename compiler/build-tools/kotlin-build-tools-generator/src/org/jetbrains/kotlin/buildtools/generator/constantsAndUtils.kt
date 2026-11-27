/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.generator

import com.squareup.kotlinpoet.ClassName
import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.base.WithKotlinReleaseVersionsMetadata
import org.jetbrains.kotlin.arguments.dsl.types.AbiStabilityMode
import org.jetbrains.kotlin.arguments.dsl.types.AnnotationDefaultTargetMode
import org.jetbrains.kotlin.arguments.dsl.types.AssertionsMode
import org.jetbrains.kotlin.arguments.dsl.types.CompatqualAnnotationsMode
import org.jetbrains.kotlin.arguments.dsl.types.ExplicitApiMode
import org.jetbrains.kotlin.arguments.dsl.types.HeaderMode
import org.jetbrains.kotlin.arguments.dsl.types.JdkRelease
import org.jetbrains.kotlin.arguments.dsl.types.JspecifyAnnotationsMode
import org.jetbrains.kotlin.arguments.dsl.types.JvmDefaultMode
import org.jetbrains.kotlin.arguments.dsl.types.JvmTarget
import org.jetbrains.kotlin.arguments.dsl.types.KotlinVersion as KotlinVersionArgumentType
import org.jetbrains.kotlin.arguments.dsl.types.LambdasMode
import org.jetbrains.kotlin.arguments.dsl.types.NameBasedDestructuringMode
import org.jetbrains.kotlin.arguments.dsl.types.ReturnValueCheckerMode
import org.jetbrains.kotlin.arguments.dsl.types.SamConversionsMode
import org.jetbrains.kotlin.arguments.dsl.types.StringConcatMode
import org.jetbrains.kotlin.arguments.dsl.types.VerifyIrMode
import org.jetbrains.kotlin.arguments.dsl.types.WhenExpressionsMode
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import kotlin.math.max
import kotlin.reflect.KClass

private const val MAX_SUPPORTED_VERSIONS_BACK = 3

internal const val IMPL_ARGUMENTS_PACKAGE = "org.jetbrains.kotlin.buildtools.internal.arguments"
internal const val API_PACKAGE = "org.jetbrains.kotlin.buildtools.api"
internal const val API_ARGUMENTS_PACKAGE = "$API_PACKAGE.arguments"
internal const val API_ENUMS_PACKAGE = "$API_ARGUMENTS_PACKAGE.enums"

internal const val JAVA_IO = "java.io"
internal const val KOTLIN_IO_PATH = "kotlin.io.path"
internal const val KOTLIN_COLLECTIONS = "kotlin.collections"
internal const val KOTLIN_TEXT = "kotlin.text"

internal val ANNOTATION_EXPERIMENTAL = ClassName(API_ARGUMENTS_PACKAGE, "ExperimentalCompilerArgument")
internal val ANNOTATION_USE_FROM_IMPL_RESTRICTED = ClassName("org.jetbrains.kotlin.buildtools.internal", "UseFromImplModuleRestricted")
internal val ANNOTATION_DELICATE_BUILDTOOLS_API = ClassName("org.jetbrains.kotlin.buildtools.api", "DelicateBuildToolsApi")
internal val ANNOTATION_TRANSIENT = ClassName("kotlinx.serialization", "Transient")
internal val ANNOTATION_SERIALIZABLE = ClassName("kotlinx.serialization", "Serializable")
internal val ANNOTATION_SERIAL_NAME= ClassName("kotlinx.serialization", "SerialName")

internal const val KDOC_SINCE = "@since"
internal const val KDOC_SINCE_2_3_0 = "$KDOC_SINCE 2.3.0"
internal const val KDOC_SINCE_2_4_20 = "$KDOC_SINCE 2.4.20"

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

private val realExperimentalLevelNames = setOf(
    CompilerArgumentsLevelNames.nativeArguments,
)

/**
 * Synthetic argument interfaces are experimental if the compiler arguments level they are based on is experimental,
 * unless [SyntheticArgumentInterface.experimental] says otherwise.
 */
internal val experimentalLevelNames = realExperimentalLevelNames + syntheticArgumentInterfaces
    .filter { it.experimental || (it.level.name in realExperimentalLevelNames) }
    .map { it.name }

private val realLevelsIntroducedIn = mapOf(
    CompilerArgumentsLevelNames.commonToolArguments to KotlinReleaseVersion.v2_3_0,
    CompilerArgumentsLevelNames.commonCompilerArguments to KotlinReleaseVersion.v2_3_0,
    CompilerArgumentsLevelNames.jvmCompilerArguments to KotlinReleaseVersion.v2_3_0,
    CompilerArgumentsLevelNames.jsArguments to KotlinReleaseVersion.v2_4_20,
    CompilerArgumentsLevelNames.wasmArguments to KotlinReleaseVersion.v2_4_20,
    CompilerArgumentsLevelNames.commonJsAndWasmArguments to KotlinReleaseVersion.v2_4_20,
    CompilerArgumentsLevelNames.commonKlibBasedArguments to KotlinReleaseVersion.v2_4_20,
    CompilerArgumentsLevelNames.metadataArguments to KotlinReleaseVersion.v2_4_20,
    CompilerArgumentsLevelNames.nativeArguments to KotlinReleaseVersion.v2_5_20,
)

/**
 * The versions the argument levels (both the real and the synthetic ones) became available in.
 *
 * Synthetic argument interfaces become available together with the compiler arguments level they are based on,
 * unless [SyntheticArgumentInterface.since] says otherwise.
 */
internal val levelsIntroducedIn = realLevelsIntroducedIn + syntheticArgumentInterfaces.associate {
    it.name to (it.since ?: realLevelsIntroducedIn[it.level.name] ?: error("Level ${it.level.name} is missing in levelSince map"))
}

/**
 * The `@since` KDoc lines of the argument levels.
 */
internal val levelsSince = levelsIntroducedIn.mapValues { "$KDOC_SINCE ${it.value.releaseName}" }

/**
 * The version the compiler arguments API first appeared in.
 *
 * The levels introduced in this version are also available for older compilers through `kotlin-build-tools-compat`.
 */
private val firstArgumentsApiVersion = KotlinReleaseVersion.v2_3_0

/**
 * An argument or an argument value removed from the compiler not later than the [level] became available could never be used
 * through that level, so there's no reason to generate it.
 *
 * This doesn't apply to the levels introduced in [firstArgumentsApiVersion]: they can be used with the compilers older
 * than the level itself, and those compilers still have such arguments and values.
 */
private fun KotlinReleaseVersion.isNotLaterThanIntroductionOf(level: KotlinCompilerArgumentsLevel): Boolean {
    val levelIntroducedIn = levelsIntroducedIn[level.name] ?: error("Level ${level.name} is missing in levelSince map")
    if (levelIntroducedIn == firstArgumentsApiVersion) return false
    return this <= levelIntroducedIn
}

/**
 * Whether the argument was removed from the compiler before it could be used through the [level].
 */
internal fun BtaCompilerArgument<*>.wasRemovedBeforeIntroductionOf(level: KotlinCompilerArgumentsLevel): Boolean =
    removedSinceVersion?.isNotLaterThanIntroductionOf(level) ?: false

/**
 * The enum entries that had been published before the removed entries started to be filtered out.
 * They are kept for compatibility, even though they were removed from the compiler long ago.
 *
 * Don't add new entries here.
 */
private val legacyRemovedEnumEntries: Set<Enum<*>> = setOf(
    JvmTarget.jvm1_6,
    KotlinVersionArgumentType.v1_0,
    KotlinVersionArgumentType.v1_1,
    KotlinVersionArgumentType.v1_2,
    KotlinVersionArgumentType.v1_3,
    KotlinVersionArgumentType.v1_4,
    KotlinVersionArgumentType.v1_5,
    KotlinVersionArgumentType.v1_6,
    KotlinVersionArgumentType.v1_7,
)

/**
 * Whether the enum entry shouldn't be generated, because it's not available in any of the compilers
 * the enum generated for the [level] can be used with. That is the case when the entry was either
 * - removed before it could be used through the [level], or
 * - removed in or before the oldest compiler version supported by [kotlinVersion].
 */
internal fun Enum<*>.isRemovedFor(level: KotlinCompilerArgumentsLevel, kotlinVersion: KotlinReleaseVersion): Boolean {
    if (this in legacyRemovedEnumEntries) return false
    val removedVersion = (this as? WithKotlinReleaseVersionsMetadata)?.releaseVersionsMetadata?.removedVersion ?: return false
    return removedVersion <= getOldestSupportedVersion(kotlinVersion) || removedVersion.isNotLaterThanIntroductionOf(level)
}

internal fun BtaCompilerArgument<*>.getOptionId(): String = name.uppercase().replace("-", "_").let {
    when {
        it.startsWith("XX") && it != "XX" -> it.replaceFirst("XX", "XX_")
        it.startsWith("X") && it != "X" -> it.replaceFirst("X", "X_")
        else -> it
    }
}

internal fun KClass<*>.toBtaEnumClassName(): ClassName = ClassName(API_ENUMS_PACKAGE, simpleName!!)
internal fun KClass<*>.toBtaImplEnumClassName(targetPackage: String): ClassName = ClassName("$targetPackage.enums", simpleName!!)

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

internal val btaEnumVersionMap: Map<ClassName, KotlinReleaseVersion> =
    mapOf(
        AbiStabilityMode::class to KotlinReleaseVersion.v2_4_0,
        AnnotationDefaultTargetMode::class to KotlinReleaseVersion.v2_4_0,
        AssertionsMode::class to KotlinReleaseVersion.v2_4_0,
        CompatqualAnnotationsMode::class to KotlinReleaseVersion.v2_4_0,
        ExplicitApiMode::class to KotlinReleaseVersion.v2_3_0,
        HeaderMode::class to KotlinReleaseVersion.v2_4_0,
        JdkRelease::class to KotlinReleaseVersion.v2_4_0,
        JspecifyAnnotationsMode::class to KotlinReleaseVersion.v2_4_0,
        JvmDefaultMode::class to KotlinReleaseVersion.v2_4_0,
        JvmTarget::class to KotlinReleaseVersion.v2_3_0,
        KotlinVersion::class to KotlinReleaseVersion.v2_3_0,
        LambdasMode::class to KotlinReleaseVersion.v2_4_0,
        NameBasedDestructuringMode::class to KotlinReleaseVersion.v2_4_0,
        ReturnValueCheckerMode::class to KotlinReleaseVersion.v2_3_0,
        SamConversionsMode::class to KotlinReleaseVersion.v2_4_0,
        StringConcatMode::class to KotlinReleaseVersion.v2_4_0,
        VerifyIrMode::class to KotlinReleaseVersion.v2_4_0,
        WhenExpressionsMode::class to KotlinReleaseVersion.v2_4_0
    ).mapKeys { [clazz, _] -> clazz.toBtaEnumClassName() }
