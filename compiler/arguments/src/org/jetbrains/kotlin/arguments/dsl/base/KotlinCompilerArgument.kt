/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.base

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import kotlin.properties.ReadOnlyProperty

@Serializable
data class KotlinCompilerArgument(
    val name: String,
    val shortName: String? = null,
    val deprecatedName: String? = null,
    val description: ReleaseDependent<String>,
    val delimiter: Delimiter?,

    val valueType: KotlinArgumentValueType<*>,
    val valueDescription: ReleaseDependent<String?> = null.asReleaseDependent(),

    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle,

    @kotlinx.serialization.Transient
    val additionalAnnotations: List<Annotation> = emptyList(),

    @kotlinx.serialization.Transient
    val compilerName: String? = null,

    val isObsolete: Boolean = false,
) : WithKotlinReleaseVersionsMetadata {

    // corresponds to [org.jetbrains.kotlin.cli.common.arguments.Argument.Delimiters]
    enum class Delimiter(val constantName: String) {
        Default("default"),
        None("none"),
        PathSeparator("path-separator"),
        Space("space"),
        Semicolon("semicolon"),
    }
}

@KotlinArgumentsDslMarker
internal class KotlinCompilerArgumentBuilder {
    lateinit var name: String
    var shortName: String? = null
    var deprecatedName: String? = null
    lateinit var description: ReleaseDependent<String>

    lateinit var valueType: KotlinArgumentValueType<*>
    var valueDescription: ReleaseDependent<String?> = null.asReleaseDependent()

    var compilerName: String? = null
    var delimiter: KotlinCompilerArgument.Delimiter? = null

    var isObsolete: Boolean = false


    private lateinit var releaseVersionsMetadata: KotlinReleaseVersionLifecycle
    private val additionalAnnotations: MutableList<Annotation> = mutableListOf()

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

    fun additionalAnnotations(vararg annotation: Annotation) {
        additionalAnnotations.addAll(annotation)
    }

    fun build(): KotlinCompilerArgument = KotlinCompilerArgument(
        name = name,
        shortName = shortName,
        deprecatedName = deprecatedName,
        description = description,
        valueType = valueType,
        valueDescription = valueDescription,
        releaseVersionsMetadata = releaseVersionsMetadata,
        additionalAnnotations = additionalAnnotations,
        compilerName = compilerName,
        delimiter = delimiter,
        isObsolete = isObsolete,
    )
}

internal fun compilerArgument(
    config: KotlinCompilerArgumentBuilder.() -> Unit
) = ReadOnlyProperty<Any?, KotlinCompilerArgument> { _, _ ->
    val builder = KotlinCompilerArgumentBuilder()
    config(builder)
    builder.build()
}