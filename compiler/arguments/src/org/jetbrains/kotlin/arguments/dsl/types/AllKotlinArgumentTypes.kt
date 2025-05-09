/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.serialization.json.AllDetailsExplicitApiModeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.AllDetailsJvmTargetSerializer
import org.jetbrains.kotlin.arguments.serialization.json.AllDetailsKotlinVersionSerializer

@Serializable
class AllKotlinArgumentTypes {
    @Serializable(with = AllDetailsKotlinVersionSerializer::class)
    val kotlinVersions = KotlinVersion.entries.toSet()

    @Serializable(with = AllDetailsJvmTargetSerializer::class)
    val jvmTargets = JvmTarget.entries.toSet()

    @Serializable(with = AllDetailsExplicitApiModeSerializer::class)
    val explicitApiModes = ExplicitApiMode.entries.toSet()
}
