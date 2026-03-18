/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.arguments.dsl.base.ReleaseDependent
import java.nio.file.Path

/**
 * [Kotlin compiler argument][org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgument] value type.
 */
@Serializable
sealed interface KotlinArgumentValueType<T : Any> {
    /**
     * Indicates if this value could have `null` value.
     */
    val isNullable: ReleaseDependent<Boolean>

    /**
     * Default value if no value was provided.
     */
    val defaultValue: ReleaseDependent<T?>

    /**
     * Converts a [value] into a human-readable string.
     */
    fun stringRepresentation(value: T?): String?
}

/**
 * A value which accepts [Boolean] type (`true` or `false`).
 */
@Serializable
class BooleanType(
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(true),
    override val defaultValue: ReleaseDependent<Boolean?> = ReleaseDependent(null),
) : KotlinArgumentValueType<Boolean> {
    override fun stringRepresentation(value: Boolean?): String? {
        return value?.toString()
    }
}

/**
 * A value which accepts [KotlinVersion] type.
 */
@Serializable
class KotlinVersionType(
    override val defaultValue: ReleaseDependent<KotlinVersion?> = ReleaseDependent(
        current = KotlinVersion.v2_2,
        KotlinReleaseVersion.v1_0_0..KotlinReleaseVersion.v1_0_7 to KotlinVersion.v1_0,
        KotlinReleaseVersion.v1_1_0..KotlinReleaseVersion.v1_1_61 to KotlinVersion.v1_1,
        KotlinReleaseVersion.v1_2_0..KotlinReleaseVersion.v1_2_71 to KotlinVersion.v1_2,
        KotlinReleaseVersion.v1_3_0..KotlinReleaseVersion.v1_3_72 to KotlinVersion.v1_3,
        KotlinReleaseVersion.v1_4_0..KotlinReleaseVersion.v1_4_32 to KotlinVersion.v1_4,
        KotlinReleaseVersion.v1_5_0..KotlinReleaseVersion.v1_5_32 to KotlinVersion.v1_5,
        KotlinReleaseVersion.v1_6_0..KotlinReleaseVersion.v1_6_21 to KotlinVersion.v1_6,
        KotlinReleaseVersion.v1_7_0..KotlinReleaseVersion.v1_7_21 to KotlinVersion.v1_7,
        KotlinReleaseVersion.v1_8_0..KotlinReleaseVersion.v1_8_22 to KotlinVersion.v1_8,
        KotlinReleaseVersion.v1_9_0..KotlinReleaseVersion.v1_9_25 to KotlinVersion.v1_9,
        KotlinReleaseVersion.v2_0_0..KotlinReleaseVersion.v2_0_21 to KotlinVersion.v2_0,
        KotlinReleaseVersion.v2_1_0..KotlinReleaseVersion.v2_1_21 to KotlinVersion.v2_1,
    ),
) : EnumType<KotlinVersion>(
    ReleaseDependent(true),
)

/**
 * A value which accepts [JvmTarget] type.
 */
@Serializable
class KotlinJvmTargetType(
    override val defaultValue: ReleaseDependent<JvmTarget?> = ReleaseDependent(
        JvmTarget.jvm1_8,
        KotlinReleaseVersion.v1_0_0..KotlinReleaseVersion.v1_9_20 to JvmTarget.jvm1_6
    ),
) : EnumType<JvmTarget>(
    ReleaseDependent(true),
)

/**
 * A value which accepts [String] type.
 */
@Serializable
class StringType(
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(true),
    override val defaultValue: ReleaseDependent<String?> = ReleaseDependent(null),
) : KotlinArgumentValueType<String> {
    override fun stringRepresentation(value: String?): String? {
        if (value == null) return null
        return "\"$value\""
    }
}

/**
 * A value which accepts [String] type.
 */
@Serializable
class IntType(
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(false),
    override val defaultValue: ReleaseDependent<Int?> = ReleaseDependent(null),
) : KotlinArgumentValueType<Int> {
    override fun stringRepresentation(value: Int?): String {
        return "\"$value\""
    }
}

/**
 * A value which accepts an array of [Strings][String] type.
 */
@Serializable
class StringArrayType(
    override val defaultValue: ReleaseDependent<Array<String>?> = ReleaseDependent(null),
) : KotlinArgumentValueType<Array<String>> {
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(true)

    override fun stringRepresentation(value: Array<String>?): String {
        if (value == null) return "null"
        return value.joinToString(separator = ", ", prefix = "arrayOf(", postfix = ")") { "\"$it\"" }
    }
}

/**
 * A value which accepts [ExplicitApiMode] type.
 */
@Serializable
class KotlinExplicitApiModeType : EnumType<ExplicitApiMode>() {
    override val defaultValue: ReleaseDependent<ExplicitApiMode?> = ReleaseDependent(ExplicitApiMode.disable)
}

/**
 * A value which accepts [HeaderMode] type.
 */
@Serializable
class KotlinHeaderModeType : EnumType<HeaderMode>() {
    override val defaultValue: ReleaseDependent<HeaderMode?> = ReleaseDependent(HeaderMode.any)
}

/**
 * A value which accepts [ReturnValueCheckerMode] type.
 */
@Serializable
class ReturnValueCheckerModeType : EnumType<ReturnValueCheckerMode>() {
    override val defaultValue: ReleaseDependent<ReturnValueCheckerMode?> = ReleaseDependent(ReturnValueCheckerMode.disabled)
}

/**
 * A value which accepts [KlibIrInlinerMode] type.
 */
@Serializable
class KlibIrInlinerModeType : EnumType<KlibIrInlinerMode>() {
    override val defaultValue: ReleaseDependent<KlibIrInlinerMode?> = ReleaseDependent(KlibIrInlinerMode.default)
}

@Serializable
sealed class EnumType<T : WithStringRepresentation>(
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(false),
) : KotlinArgumentValueType<T> {
    override fun stringRepresentation(value: T?): String? {
        return value?.stringRepresentation?.valueOrNullStringLiteral
    }
}

/**
 * A value which accepts [Path] type.
 */
@Serializable
class PathType(
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(true),
    override val defaultValue: ReleaseDependent<Path?> = ReleaseDependent(null),
) : KotlinArgumentValueType<Path> {

    override fun stringRepresentation(value: Path?): String? {
        if (value == null) return null
        return "\"${value.absolutePathStringOrThrow()}\""
    }
}

/**
 * A value which accepts [ProfileCompilerCommand] type.
 */
@Serializable
object ProfileCompilerCommandType : KotlinArgumentValueType<ProfileCompilerCommand> {
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(true)
    override val defaultValue: ReleaseDependent<ProfileCompilerCommand?> = ReleaseDependent(null)

    override fun stringRepresentation(value: ProfileCompilerCommand?): String? {
        if (value == null) return null
        return with(value) {
            $$"\"$${profilerPath.absolutePathStringOrThrow()}${File.pathSeparator}$$command${File.pathSeparator}$${outputDir.absolutePathStringOrThrow()}\""
        }
    }
}

/**
 * A value which accepts [JvmDefaultMode] type.
 */
@Serializable
class JvmDefaultModeType : EnumType<JvmDefaultMode>(ReleaseDependent(true)) {
    override val defaultValue: ReleaseDependent<JvmDefaultMode?> = ReleaseDependent(null)
}

/**
 * A value which accepts [AbiStabilityMode] type.
 */
@Serializable
class AbiStabilityModeType : EnumType<AbiStabilityMode>(ReleaseDependent(true)) {
    override val defaultValue: ReleaseDependent<AbiStabilityMode?> = ReleaseDependent(null)
}

/**
 * A value which accepts [AssertionsMode] type.
 */
@Serializable
class AssertionsModeType : EnumType<AssertionsMode>(ReleaseDependent(true)) {
    override val defaultValue: ReleaseDependent<AssertionsMode?> = ReleaseDependent(AssertionsMode.LEGACY)
}

/**
 * A value which accepts [JspecifyAnnotationsMode] type.
 */
@Serializable
class JspecifyAnnotationsModeType : EnumType<JspecifyAnnotationsMode>(ReleaseDependent(true)) {
    override val defaultValue: ReleaseDependent<JspecifyAnnotationsMode?> = ReleaseDependent(null)
}

/**
 * A value which accepts [LambdasMode] type.
 */
@Serializable
class LambdasModeType : EnumType<LambdasMode>(ReleaseDependent(true)) {
    override val defaultValue: ReleaseDependent<LambdasMode?> = ReleaseDependent(null)
}

/**
 * A value which accepts [SamConversionsMode] type.
 */
@Serializable
class SamConversionsModeType : EnumType<SamConversionsMode>(ReleaseDependent(true)) {
    override val defaultValue: ReleaseDependent<SamConversionsMode?> = ReleaseDependent(null)
}

/**
 * A value which accepts [StringConcatMode] type.
 */
@Serializable
class StringConcatModeType : EnumType<StringConcatMode>(ReleaseDependent(true)) {
    override val defaultValue: ReleaseDependent<StringConcatMode?> = ReleaseDependent(null)
}

/**
 * A value which accepts [CompatqualAnnotationsMode] type.
 */
@Serializable
class CompatqualAnnotationsModeType : EnumType<CompatqualAnnotationsMode>(ReleaseDependent(true)) {
    override val defaultValue: ReleaseDependent<CompatqualAnnotationsMode?> = ReleaseDependent(null)
}

/**
 * A value which accepts [WhenExpressionsMode] type.
 */
@Serializable
class WhenExpressionsModeType : EnumType<WhenExpressionsMode>(ReleaseDependent(true)) {
    override val defaultValue: ReleaseDependent<WhenExpressionsMode?> = ReleaseDependent(null)
}

/**
 * A value which accepts [JdkRelease] type.
 */
@Serializable
class JdkReleaseType : EnumType<JdkRelease>(ReleaseDependent(true)) {
    override val defaultValue: ReleaseDependent<JdkRelease?> = ReleaseDependent(null)
}

/**
 * A value which accepts a list of [String] type.
 */
@Serializable
class StringListType(
    override val defaultValue: ReleaseDependent<List<String>?> = ReleaseDependent(emptyList()),
) : KotlinArgumentValueType<List<String>> {

    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(false)

    override fun stringRepresentation(value: List<String>?): String? {
        if (value == null) return null
        return value.joinToString { it.valueOrNullStringLiteral }
    }
}

/**
 * A value type that accepts a list of [Path] elements.
 *
 * There are two rendering strategies:
 * - [SystemPathType]: paths joined with OS separator into a single string
 * - [LiteralPathType]: paths rendered as individual comma-separated literals
 */
@Serializable
sealed class PathListType() : KotlinArgumentValueType<List<Path>>


/**
 * A [PathListType] rendered as a single quoted string using the OS path separator.
 * Example: `"/usr/bin:/usr/local/bin"` (Unix) or `"C:\bin;D:\bin"` (Windows)
 */
@Serializable
class SystemPathType(
    override val defaultValue: ReleaseDependent<List<Path>?> = ReleaseDependent(null),
) : PathListType() {
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(true)

    override fun stringRepresentation(value: List<Path>?): String? {
        if (value == null) return null

        return "\"${value.joinToString($$"${File.pathSeparator}") { it.absolutePathStringOrThrow() }}\""
    }
}

/**
 * A [PathListType] rendered as individually quoted paths separated by commas.
 * Example: `"/usr/bin", "/usr/local/bin"`
 */
@Serializable
class LiteralPathType(
    override val defaultValue: ReleaseDependent<List<Path>?> = ReleaseDependent(emptyList()),
) : PathListType() {
    override val isNullable: ReleaseDependent<Boolean> = ReleaseDependent(false)

    override fun stringRepresentation(value: List<Path>?): String? {
        if (value == null) return null

        return value.joinToString { it.absolutePathStringOrThrow().valueOrNullStringLiteral }
    }
}

/**
 * A value which accepts [AnnotationDefaultTargetMode] type.
 */
@Serializable
class AnnotationDefaultTargetModeType : EnumType<AnnotationDefaultTargetMode>(ReleaseDependent(true)) {
    override val defaultValue: ReleaseDependent<AnnotationDefaultTargetMode?> = ReleaseDependent(null)
}

/**
 * A value which accepts [NameBasedDestructuringMode] type.
 */
@Serializable
class NameBasedDestructuringModeType : EnumType<NameBasedDestructuringMode>(ReleaseDependent(true)) {
    override val defaultValue: ReleaseDependent<NameBasedDestructuringMode?> = ReleaseDependent(null)
}

/**
 * A value which accepts [VerifyIrMode] type.
 */
@Serializable
class VerifyIrModeType : EnumType<VerifyIrMode>(ReleaseDependent(true)) {
    override val defaultValue: ReleaseDependent<VerifyIrMode?> = ReleaseDependent(null)
}

private val String?.valueOrNullStringLiteral: String
    get() = "\"${this}\""

/**
 * The implementation uses [Path.toFile] followed by [java.io.File.getAbsolutePath] rather than
 * [kotlin.io.path.absolutePathString] to validate that the path comes from the default file system.
 *
 * See https://youtrack.jetbrains.com/issue/KT-83715 for details.
 */
private fun Path.absolutePathStringOrThrow(): String = toFile().absolutePath
