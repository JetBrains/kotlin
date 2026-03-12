/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.NameBasedDestructuringMode
import org.jetbrains.kotlin.arguments.dsl.types.NameBasedDestructuringModeType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object KotlinNameBasedDestructuringModeAsNameSerializer : NamedTypeSerializer<NameBasedDestructuringMode>(
    serialName = "org.jetbrains.kotlin.arguments.NameBasedDestructuringMode",
    nameAccessor = NameBasedDestructuringMode::modeName,
    typeFinder = NameBasedDestructuringMode::modeName.typeFinder()
)

private object AllNameBasedDestructuringModeSerializer : AllNamedTypeSerializer<NameBasedDestructuringMode>(
    serialName = "org.jetbrains.kotlin.arguments.NameBasedDestructuringMode",
    jsonElementNameForName = "name",
    nameAccessor = NameBasedDestructuringMode::modeName,
    typeFinder = NameBasedDestructuringMode::modeName.typeFinder()
)

object AllDetailsNameBasedDestructuringModeSerializer : SetTypeSerializer<NameBasedDestructuringMode>(
    typeSerializer = AllNameBasedDestructuringModeSerializer,
    valueTypeQualifiedNamed = NameBasedDestructuringModeType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetNameBasedDestructuringMode",
)
