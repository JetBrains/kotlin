/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments

import KotlinArgumentsDslMarker
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.types.KotlinArgumentValueType

interface CompilerArgumentBase {
    val name: String
    val shortName: String?
    val deprecatedName: String?
    val description: String

    val valueType: KotlinArgumentValueType<*>
    val valueDescription: String?

    val addedInVersion: KotlinReleaseVersion
    val stableSinceVersion: KotlinReleaseVersion?
    val deprecatedInVersion: KotlinReleaseVersion?
    val removedInVersion: KotlinReleaseVersion?
}

@Serializable
data class CompilerArgument(
    override val name: String,
    override val shortName: String? = null,
    override val deprecatedName: String? = null,
    override val description: String,

    override val valueType: KotlinArgumentValueType<*>,
    override val valueDescription: String? = null,

    override val addedInVersion: KotlinReleaseVersion,
    override val stableSinceVersion: KotlinReleaseVersion? = null,
    override val deprecatedInVersion: KotlinReleaseVersion? = null,
    override val removedInVersion: KotlinReleaseVersion? = null,
) : CompilerArgumentBase

@KotlinArgumentsDslMarker
class CompilerArgumentBuilder : CompilerArgumentBase {
    override lateinit var name: String
    override var shortName: String? = null
    override var deprecatedName: String? = null
    override lateinit var description: String

    override lateinit var valueType: KotlinArgumentValueType<*>
    override var valueDescription: String? = null

    override lateinit var addedInVersion: KotlinReleaseVersion
    override var stableSinceVersion: KotlinReleaseVersion? = null
    override var deprecatedInVersion: KotlinReleaseVersion? = null
    override var removedInVersion: KotlinReleaseVersion? = null

    fun build(): CompilerArgument = CompilerArgument(
        name = name,
        shortName = shortName,
        deprecatedName = deprecatedName,
        description = description,
        valueType = valueType,
        valueDescription = valueDescription,
        addedInVersion = addedInVersion,
        stableSinceVersion = stableSinceVersion,
        deprecatedInVersion = deprecatedInVersion,
        removedInVersion = removedInVersion,
    )
}
