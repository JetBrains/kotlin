/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.SerializeIr
import org.jetbrains.kotlin.arguments.dsl.types.SerializeIrType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object SerializeIrAsNameSerializer : NamedTypeSerializer<SerializeIr>(
    serialName = "org.jetbrains.kotlin.arguments.SerializeIr",
    nameAccessor = SerializeIr::modeName,
    typeFinder = SerializeIr::modeName.typeFinder()
)

private object AllSerializeIrSerializer : AllNamedTypeSerializer<SerializeIr>(
    serialName = "org.jetbrains.kotlin.arguments.SerializeIr",
    jsonElementNameForName = "name",
    nameAccessor = SerializeIr::modeName,
    typeFinder = SerializeIr::modeName.typeFinder()
)

object AllDetailsSerializeIrSerializer : SetTypeSerializer<SerializeIr>(
    typeSerializer = AllSerializeIrSerializer,
    valueTypeQualifiedNamed = SerializeIrType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetSerializeIr",
)
