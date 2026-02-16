/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.JvmDefaultMode
import org.jetbrains.kotlin.arguments.dsl.types.JvmDefaultModeType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object KotlinJvmDefaultModeAsNameSerializer : NamedTypeSerializer<JvmDefaultMode>(
    serialName = "org.jetbrains.kotlin.arguments.JvmDefaultMode",
    nameAccessor = JvmDefaultMode::modeName,
    typeFinder = JvmDefaultMode::modeName.typeFinder()
)

private object AllJvmDefaultModeSerializer : AllNamedTypeSerializer<JvmDefaultMode>(
    serialName = "org.jetbrains.kotlin.arguments.JvmDefaultMode",
    jsonElementNameForName = "name",
    nameAccessor = JvmDefaultMode::modeName,
    typeFinder = JvmDefaultMode::modeName.typeFinder()
)

object AllDetailsJvmDefaultModeSerializer : SetTypeSerializer<JvmDefaultMode>(
    typeSerializer = AllJvmDefaultModeSerializer,
    valueTypeQualifiedNamed = JvmDefaultModeType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetJvmDefaultMode",
)
