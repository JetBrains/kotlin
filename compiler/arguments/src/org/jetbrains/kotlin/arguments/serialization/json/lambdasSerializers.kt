/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.Lambdas
import org.jetbrains.kotlin.arguments.dsl.types.LambdasType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object LambdasAsNameSerializer : NamedTypeSerializer<Lambdas>(
    serialName = "org.jetbrains.kotlin.arguments.Lambdas",
    nameAccessor = Lambdas::schemeName,
    typeFinder = Lambdas::schemeName.typeFinder()
)

private object AllLambdasSerializer : AllNamedTypeSerializer<Lambdas>(
    serialName = "org.jetbrains.kotlin.arguments.Lambdas",
    jsonElementNameForName = "name",
    nameAccessor = Lambdas::schemeName,
    typeFinder = Lambdas::schemeName.typeFinder()
)

object AllDetailsLambdasSerializer : SetTypeSerializer<Lambdas>(
    typeSerializer = AllLambdasSerializer,
    valueTypeQualifiedNamed = LambdasType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetLambdas",
)
