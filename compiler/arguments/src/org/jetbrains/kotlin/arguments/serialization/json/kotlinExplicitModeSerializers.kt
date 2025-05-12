/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.ExplicitApiMode
import org.jetbrains.kotlin.arguments.dsl.types.KotlinExplicitApiModeType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer

object KotlinExplicitApiModeAsModeSerializer : NamedTypeSerializer<ExplicitApiMode>(
    serialName = "org.jetbrains.kotlin.config.ExplicitApiMode",
    nameAccessor = { it.modeName },
    typeFinder = {
        ExplicitApiMode.entries.single { mode -> mode.modeName == it }
    }
)

private object AllExplicitApiModeSerializer : AllNamedTypeSerializer<ExplicitApiMode>(
    serialName = "org.jetbrains.kotlin.config.ExplicitApiMode",
    jsonElementNameForName = "modeName",
    nameAccessor = { it.modeName },
    typeFinder = {
        ExplicitApiMode.entries.single { mode -> mode.modeName == it }
    }
)

object AllDetailsExplicitApiModeSerializer : SetTypeSerializer<ExplicitApiMode>(
    typeSerializer = AllExplicitApiModeSerializer,
    valueTypeQualifiedNamed = KotlinExplicitApiModeType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.config.SetExplicitApiMode"
)
