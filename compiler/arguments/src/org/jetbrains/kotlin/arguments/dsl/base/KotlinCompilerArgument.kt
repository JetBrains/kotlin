/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.base

import org.jetbrains.kotlin.arguments.dsl.types.KotlinArgumentValueType
import kotlin.properties.ReadOnlyProperty

data class KotlinCompilerArgument(
    val name: String,
    val shortName: String? = null,
    val deprecatedName: String? = null,
    val description: ReleaseDependent<String>,

    val valueType: KotlinArgumentValueType<*>,
    val valueDescription: ReleaseDependent<String?> = null.asReleaseDependent(),

    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle,
) : WithKotlinReleaseVersionsMetadata

@KotlinArgumentsDslMarker
internal class KotlinCompilerArgumentBuilder {
    lateinit var name: String
    var shortName: String? = null
    var deprecatedName: String? = null
    lateinit var description: ReleaseDependent<String>

    lateinit var valueType: KotlinArgumentValueType<*>
    var valueDescription: ReleaseDependent<String?> = null.asReleaseDependent()

    private lateinit var releaseVersionsMetadata: KotlinReleaseVersionLifecycle

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

    fun build(): KotlinCompilerArgument = KotlinCompilerArgument(
        name = name,
        shortName = shortName,
        deprecatedName = deprecatedName,
        description = description,
        valueType = valueType,
        valueDescription = valueDescription,
        releaseVersionsMetadata = releaseVersionsMetadata,
    )
}

internal fun compilerArgument(
    config: KotlinCompilerArgumentBuilder.() -> Unit
) = ReadOnlyProperty<Any?, KotlinCompilerArgument> { _, _ ->
    val builder = KotlinCompilerArgumentBuilder()
    config(builder)
    builder.build()
}