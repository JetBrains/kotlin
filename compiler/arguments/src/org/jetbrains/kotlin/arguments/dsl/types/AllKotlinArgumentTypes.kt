/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.types

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.serialization.json.*

/**
 * Class containing all non-primitive compiler argument types which are serialized in more detailed form.
 */
@Suppress("unused")
@Serializable
class AllKotlinArgumentTypes {
    @Serializable(with = AllDetailsKotlinVersionSerializer::class)
    val kotlinVersions = KotlinVersion.entries.toSet()

    @Serializable(with = AllDetailsJvmTargetSerializer::class)
    val jvmTargets = JvmTarget.entries.toSet()

    @Serializable(with = AllDetailsExplicitApiModeSerializer::class)
    val explicitApiModes = ExplicitApiMode.entries.toSet()

    @Serializable(with = AllDetailsReturnValueCheckerModeSerializer::class)
    val returnValueCheckerMode = ReturnValueCheckerMode.entries.toSet()

    @Serializable(with = AllDetailsKlibIrInlinerModeSerializer::class)
    val klibIrInlinerMode = KlibIrInlinerMode.entries.toSet()

    @Serializable(with = AllDetailsJvmDefaultModeSerializer::class)
    val jvmDefaultMode = JvmDefaultMode.entries.toSet()

    @Serializable(with = AllDetailsAbiStabilityModeSerializer::class)
    val abiStabilityMode = AbiStabilityMode.entries.toSet()

    @Serializable(with = AllDetailsAssertionsModeSerializer::class)
    val assertionsMode = AssertionsMode.entries.toSet()

    @Serializable(with = AllDetailsJspecifyAnnotationsModeSerializer::class)
    val jspecifyAnnotationsMode = JspecifyAnnotationsMode.entries.toSet()

    @Serializable(with = AllDetailsLambdasModeSerializer::class)
    val lambdasMode = LambdasMode.entries.toSet()

    @Serializable(with = AllDetailsSamConversionsModeSerializer::class)
    val samConversionsMode = SamConversionsMode.entries.toSet()

    @Serializable(with = AllDetailsStringConcatModeSerializer::class)
    val stringConcatMode = StringConcatMode.entries.toSet()

}
