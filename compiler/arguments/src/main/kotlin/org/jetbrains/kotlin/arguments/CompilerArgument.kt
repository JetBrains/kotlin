/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments

import KotlinArgumentsDslMarker
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.types.KotlinArgumentValueType
import kotlin.properties.ReadOnlyProperty

@Serializable
data class CompilerArgument(
    val name: String,
    val shortName: String? = null,
    val deprecatedName: String? = null,
    val description: ReleaseDependent<String>,

    val valueType: KotlinArgumentValueType<*>,
    val valueDescription: ReleaseDependent<String?> = null.asReleaseDependent(),

    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle,
) : WithKotlinReleaseVersionsMetadata

@KotlinArgumentsDslMarker
class CompilerArgumentBuilder {
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

    fun build(): CompilerArgument = CompilerArgument(
        name = name,
        shortName = shortName,
        deprecatedName = deprecatedName,
        description = description,
        valueType = valueType,
        valueDescription = valueDescription,
        releaseVersionsMetadata = releaseVersionsMetadata,
    )
}

fun compilerArgument(
    config: CompilerArgumentBuilder.() -> Unit
) = ReadOnlyProperty<Any?, CompilerArgument> { _, _ ->
    val builder = CompilerArgumentBuilder()
    config(builder)
    builder.build()
}
