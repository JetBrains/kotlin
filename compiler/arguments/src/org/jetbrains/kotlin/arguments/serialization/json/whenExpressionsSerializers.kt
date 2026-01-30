/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.WhenExpressions
import org.jetbrains.kotlin.arguments.dsl.types.WhenExpressionsType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object WhenExpressionsAsNameSerializer : NamedTypeSerializer<WhenExpressions>(
    serialName = "org.jetbrains.kotlin.arguments.WhenExpressions",
    nameAccessor = WhenExpressions::schemeName,
    typeFinder = WhenExpressions::schemeName.typeFinder()
)

private object AllWhenExpressionsSerializer : AllNamedTypeSerializer<WhenExpressions>(
    serialName = "org.jetbrains.kotlin.arguments.WhenExpressions",
    jsonElementNameForName = "name",
    nameAccessor = WhenExpressions::schemeName,
    typeFinder = WhenExpressions::schemeName.typeFinder()
)

object AllDetailsWhenExpressionsSerializer : SetTypeSerializer<WhenExpressions>(
    typeSerializer = AllWhenExpressionsSerializer,
    valueTypeQualifiedNamed = WhenExpressionsType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetWhenExpressions",
)
