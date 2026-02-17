/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.StringConcatMode
import org.jetbrains.kotlin.arguments.dsl.types.StringConcatModeType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object KotlinStringConcatModeAsNameSerializer : NamedTypeSerializer<StringConcatMode>(
    serialName = "org.jetbrains.kotlin.arguments.StringConcatMode",
    nameAccessor = StringConcatMode::modeName,
    typeFinder = StringConcatMode::modeName.typeFinder()
)

private object AllStringConcatModeSerializer : AllNamedTypeSerializer<StringConcatMode>(
    serialName = "org.jetbrains.kotlin.arguments.StringConcatMode",
    jsonElementNameForName = "name",
    nameAccessor = StringConcatMode::modeName,
    typeFinder = StringConcatMode::modeName.typeFinder()
)

object AllDetailsStringConcatModeSerializer : SetTypeSerializer<StringConcatMode>(
    typeSerializer = AllStringConcatModeSerializer,
    valueTypeQualifiedNamed = StringConcatModeType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetStringConcatMode",
)
