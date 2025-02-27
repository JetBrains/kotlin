/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments

import KotlinArgumentsDslMarker
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.types.KotlinArgumentValueType

interface CompilerArgumentBase : WithKotlinReleaseVersionsMetadata {
    val name: String
    val shortName: String?
    val deprecatedName: String?
    val description: ReleaseDependent<String>

    val valueType: KotlinArgumentValueType<*>
    val valueDescription: ReleaseDependent<String?>
}

@Serializable
data class CompilerArgument(
    override val name: String,
    override val shortName: String? = null,
    override val deprecatedName: String? = null,
    override val description: ReleaseDependent<String>,

    override val valueType: KotlinArgumentValueType<*>,
    override val valueDescription: ReleaseDependent<String?> = null.asReleaseDependent(),

    override val releaseVersionsMetadata: KotlinReleaseVersionLifecycle,
) : CompilerArgumentBase

@KotlinArgumentsDslMarker
class CompilerArgumentBuilder : CompilerArgumentBase {
    override lateinit var name: String
    override var shortName: String? = null
    override var deprecatedName: String? = null
    override lateinit var description: ReleaseDependent<String>

    override lateinit var valueType: KotlinArgumentValueType<*>
    override var valueDescription: ReleaseDependent<String?> = null.asReleaseDependent()

    override lateinit var releaseVersionsMetadata: KotlinReleaseVersionLifecycle

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
