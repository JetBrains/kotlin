/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.AbiStability
import org.jetbrains.kotlin.arguments.dsl.types.AbiStabilityType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object AbiStabilityAsNameSerializer : NamedTypeSerializer<AbiStability>(
    serialName = "org.jetbrains.kotlin.arguments.AbiStability",
    nameAccessor = AbiStability::stabilityName,
    typeFinder = AbiStability::stabilityName.typeFinder()
)

private object AllAbiStabilitySerializer : AllNamedTypeSerializer<AbiStability>(
    serialName = "org.jetbrains.kotlin.arguments.AbiStability",
    jsonElementNameForName = "name",
    nameAccessor = AbiStability::stabilityName,
    typeFinder = AbiStability::stabilityName.typeFinder()
)

object AllDetailsAbiStabilitySerializer : SetTypeSerializer<AbiStability>(
    typeSerializer = AllAbiStabilitySerializer,
    valueTypeQualifiedNamed = AbiStabilityType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetAbiStability",
)