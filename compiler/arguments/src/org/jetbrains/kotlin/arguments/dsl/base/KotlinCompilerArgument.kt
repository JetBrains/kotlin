/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.base

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import kotlin.properties.ReadOnlyProperty

/**
 * A Kotlin compiler argument description.
 *
 * @param name the full name of the argument (e.g. "help").
 * @param shortName the alternative short name of the argument (e.g. "h").
 * @param deprecatedName the old deprecated compiler argument name from which users should migrate, but it is still supported.
 * @param description the full description of this argument.
 * The description text may have a different value for different Kotlin releases,
 * see [ReleaseDependent] on how to define the description for older versions.
 * @param delimiter if an argument accepts a list of file paths - defines an accepted delimiter between these paths.
 * @param valueType (Deprecated) Use [argumentType] instead; it provides a type-safe representation of the argument's type.
 * @param valueDescription (Deprecated) Use [argumentTypeDescription] instead (renamed for naming consistency).
 * @param argumentType the argument value type.
 * @param argumentTypeDescription describes which values are accepted by the argument.
 * The description text may have a different value for different Kotlin releases,
 * see [ReleaseDependent] on how to define the description for older versions.
 * @param additionalAnnotations additional annotations that should be added for the Kotlin compiler argument representation (e.g. [Deprecated]).
 * @param compilerName alternative property name in the generated Kotlin compiler argument representation
 *
 * Usually compiler arguments should either be defined via compiler argument level builder [KotlinCompilerArgumentsLevelBuilder.compilerArgument]
 * or via special standalone builder DSL - [compilerArgument].
 */
@Serializable
data class KotlinCompilerArgument(
    val name: String,
    val shortName: String? = null,
    val deprecatedName: String? = null,
    val description: ReleaseDependent<String>,
    val delimiter: Delimiter?,

    val argumentType: KotlinArgumentValueType<*>,
    val argumentTypeDescription: ReleaseDependent<String?> = null.asReleaseDependent(),

    @Deprecated(
        message = "Use argumentType instead; it provides a type-safe representation of the argument's type. " +
                "Will be promoted to an error in KT-84084.",
        replaceWith = ReplaceWith("argumentType")
    )
    val valueType: KotlinArgumentValueType<*> = argumentType,
    @Deprecated(
        message = "Renamed for naming consistency; use argumentTypeDescription instead. " +
                "Will be promoted to an error in KT-84084.",
        replaceWith = ReplaceWith("argumentTypeDescription")
    )
    val valueDescription: ReleaseDependent<String?> = argumentTypeDescription,

    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle,

    @kotlinx.serialization.Transient
    val additionalAnnotations: List<Annotation> = emptyList(),

    @kotlinx.serialization.Transient
    val compilerName: String? = null,

    @kotlinx.serialization.Transient
    val isObsolete: Boolean = false,
) : WithKotlinReleaseVersionsMetadata {

    constructor(
        name: String,
        shortName: String? = null,
        deprecatedName: String? = null,
        description: ReleaseDependent<String>,
        delimiter: Delimiter?,
        valueType: KotlinArgumentValueType<*>,
        valueDescription: ReleaseDependent<String?> = null.asReleaseDependent(),
        releaseVersionsMetadata: KotlinReleaseVersionLifecycle,
        additionalAnnotations: List<Annotation> = emptyList(),
        compilerName: String? = null,
    ) : this(
        name = name,
        shortName = shortName,
        deprecatedName = deprecatedName,
        description = description,
        delimiter = delimiter,
        valueType = valueType,
        valueDescription = valueDescription,
        argumentType = valueType,
        argumentTypeDescription = valueDescription,
        releaseVersionsMetadata = releaseVersionsMetadata,
        additionalAnnotations = additionalAnnotations,
        compilerName = compilerName,
    )

    // corresponds to [org.jetbrains.kotlin.cli.common.arguments.Argument.Delimiters]
    enum class Delimiter(val constantName: String) {
        Default("default"),
        None("none"),
        PathSeparator("path-separator"),
        Space("space"),
        Semicolon("semicolon"),
    }
}

/**
 * DSL builder for [KotlinCompilerArgument].
 */
@KotlinArgumentsDslMarker
internal class KotlinCompilerArgumentBuilder {

    /**
     * @see KotlinCompilerArgument.name
     */
    lateinit var name: String

    /**
     * @see KotlinCompilerArgument.shortName
     */
    var shortName: String? = null

    /**
     * @see KotlinCompilerArgument.deprecatedName
     */
    var deprecatedName: String? = null

    /**
     * @see KotlinCompilerArgument.description
     */
    lateinit var description: ReleaseDependent<String>

    /**
     * @see KotlinCompilerArgument.valueType
     */
    @Deprecated(
        message = "Use argumentType instead; it provides a type-safe representation of the argument's type. " +
                "Will be promoted to an error in KT-84084.",
        replaceWith = ReplaceWith("argumentType")
    )
    var valueType: KotlinArgumentValueType<*>? = null

    /**
     * @see KotlinCompilerArgument.valueDescription
     */
    @Deprecated(
        message = "Renamed for naming consistency; use argumentTypeDescription instead. " +
                "Will be promoted to an error in KT-84084.",
        replaceWith = ReplaceWith("argumentTypeDescription")
    )
    var valueDescription: ReleaseDependent<String?>? = null

    /**
     * @see KotlinCompilerArgument.argumentType
     */
    var argumentType: KotlinArgumentValueType<*>? = null

    /**
     * @see KotlinCompilerArgument.argumentTypeDescription
     */
    var argumentTypeDescription: ReleaseDependent<String?>? = null

    /**
     * @see KotlinCompilerArgument.compilerName
     */
    var compilerName: String? = null

    /**
     * @see KotlinCompilerArgument.delimiter
     */
    var delimiter: KotlinCompilerArgument.Delimiter? = null

    /**
     * @see KotlinCompilerArgument.releaseVersionsMetadata
     */
    private lateinit var releaseVersionsMetadata: KotlinReleaseVersionLifecycle

    /**
     * @see KotlinCompilerArgument.additionalAnnotations
     */
    private val additionalAnnotations: MutableList<Annotation> = mutableListOf()

    /**
     * Convenient method to define this argument [KotlinReleaseVersionLifecycle] metadata.
     */
    fun lifecycle(
        introducedVersion: KotlinReleaseVersion,
        stabilizedVersion: KotlinReleaseVersion? = null,
        deprecatedVersion: KotlinReleaseVersion? = null,
        removedVersion: KotlinReleaseVersion? = null,
    ) {
        releaseVersionsMetadata = KotlinReleaseVersionLifecycle(
            introducedVersion,
            stabilizedVersion,
            deprecatedVersion,
            removedVersion
        )
    }

    /**
     * Convenient method to add additional into [KotlinCompilerArgument.additionalAnnotations].
     */
    fun additionalAnnotations(vararg annotation: Annotation) {
        additionalAnnotations.addAll(annotation)
    }

    /**
     * Build a new instance of [KotlinCompilerArgument].
     */
    @Suppress("DEPRECATION")
    fun build(): KotlinCompilerArgument = KotlinCompilerArgument(
        name = name,
        shortName = shortName,
        deprecatedName = deprecatedName,
        description = description,
        valueType = requireNotNull(valueType ?: argumentType),
        valueDescription = valueDescription ?: argumentTypeDescription ?: null.asReleaseDependent(),
        argumentType = requireNotNull(argumentType ?: valueType),
        argumentTypeDescription = argumentTypeDescription ?: valueDescription ?: null.asReleaseDependent(),
        releaseVersionsMetadata = releaseVersionsMetadata,
        additionalAnnotations = additionalAnnotations,
        compilerName = compilerName,
        delimiter = delimiter,
    )
}

/**
 * Allows creating compiler argument definitions that are separate from the main DSL and could later be added to the main DSL.
 *
 * Usage example:
 * ```
 * val helpCompilerArgument by compilerArgument {
 *    name = "help"
 *    shortName = "h"
 *    description = "Provides a help message"
 *    ...
 * }
 * ```
 *
 * Such standalone compiler argument could be added into the main definition via [KotlinCompilerArgumentsLevelBuilder.addCompilerArguments]
 * method.
 *
 * @see KotlinCompilerArgumentBuilder
 */
internal fun compilerArgument(
    config: KotlinCompilerArgumentBuilder.() -> Unit,
) = ReadOnlyProperty<Any?, KotlinCompilerArgument> { _, _ ->
    val builder = KotlinCompilerArgumentBuilder()
    config(builder)
    builder.build()
}