/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.WhenExpressionsMode
import org.jetbrains.kotlin.arguments.dsl.types.WhenExpressionsModeType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object KotlinWhenExpressionsModeAsNameSerializer : NamedTypeSerializer<WhenExpressionsMode>(
    serialName = "org.jetbrains.kotlin.arguments.WhenExpressionsMode",
    nameAccessor = WhenExpressionsMode::modeName,
    typeFinder = WhenExpressionsMode::modeName.typeFinder()
)

private object AllWhenExpressionsModeSerializer : AllNamedTypeSerializer<WhenExpressionsMode>(
    serialName = "org.jetbrains.kotlin.arguments.WhenExpressionsMode",
    jsonElementNameForName = "name",
    nameAccessor = WhenExpressionsMode::modeName,
    typeFinder = WhenExpressionsMode::modeName.typeFinder()
)

object AllDetailsWhenExpressionsModeSerializer : SetTypeSerializer<WhenExpressionsMode>(
    typeSerializer = AllWhenExpressionsModeSerializer,
    valueTypeQualifiedNamed = WhenExpressionsModeType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetWhenExpressionsMode",
)
