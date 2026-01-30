/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.JvmDefault
import org.jetbrains.kotlin.arguments.dsl.types.JvmDefaultType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object JvmDefaultAsNameSerializer : NamedTypeSerializer<JvmDefault>(
    serialName = "org.jetbrains.kotlin.arguments.JvmDefault",
    nameAccessor = JvmDefault::methodName,
    typeFinder = JvmDefault::methodName.typeFinder()
)

private object AllJvmDefaultSerializer : AllNamedTypeSerializer<JvmDefault>(
    serialName = "org.jetbrains.kotlin.arguments.JvmDefault",
    jsonElementNameForName = "name",
    nameAccessor = JvmDefault::methodName,
    typeFinder = JvmDefault::methodName.typeFinder()
)

object AllDetailsJvmDefaultSerializer : SetTypeSerializer<JvmDefault>(
    typeSerializer = AllJvmDefaultSerializer,
    valueTypeQualifiedNamed = JvmDefaultType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetJvmDefault",
)
