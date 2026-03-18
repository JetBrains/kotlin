/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.AbiStabilityMode
import org.jetbrains.kotlin.arguments.dsl.types.AbiStabilityModeType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object KotlinAbiStabilityModeAsNameSerializer : NamedTypeSerializer<AbiStabilityMode>(
    serialName = "org.jetbrains.kotlin.arguments.AbiStabilityMode",
    nameAccessor = AbiStabilityMode::modeName,
    typeFinder = AbiStabilityMode::modeName.typeFinder()
)

private object AllAbiStabilityModeSerializer : AllNamedTypeSerializer<AbiStabilityMode>(
    serialName = "org.jetbrains.kotlin.arguments.AbiStabilityMode",
    jsonElementNameForName = "name",
    nameAccessor = AbiStabilityMode::modeName,
    typeFinder = AbiStabilityMode::modeName.typeFinder()
)

object AllDetailsAbiStabilityModeSerializer : SetTypeSerializer<AbiStabilityMode>(
    typeSerializer = AllAbiStabilityModeSerializer,
    valueTypeQualifiedNamed = AbiStabilityModeType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetAbiStabilityMode",
)
