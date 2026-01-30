/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.StringConcat
import org.jetbrains.kotlin.arguments.dsl.types.StringConcatType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object StringConcatAsNameSerializer : NamedTypeSerializer<StringConcat>(
    serialName = "org.jetbrains.kotlin.arguments.StringConcat",
    nameAccessor = StringConcat::schemeName,
    typeFinder = StringConcat::schemeName.typeFinder()
)

private object AllStringConcatSerializer : AllNamedTypeSerializer<StringConcat>(
    serialName = "org.jetbrains.kotlin.arguments.StringConcat",
    jsonElementNameForName = "name",
    nameAccessor = StringConcat::schemeName,
    typeFinder = StringConcat::schemeName.typeFinder()
)

object AllDetailsStringConcatSerializer : SetTypeSerializer<StringConcat>(
    typeSerializer = AllStringConcatSerializer,
    valueTypeQualifiedNamed = StringConcatType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetStringConcat",
)
