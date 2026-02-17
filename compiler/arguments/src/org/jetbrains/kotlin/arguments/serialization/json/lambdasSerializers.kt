/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.LambdasMode
import org.jetbrains.kotlin.arguments.dsl.types.LambdasModeType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object KotlinLambdasModeAsNameSerializer : NamedTypeSerializer<LambdasMode>(
    serialName = "org.jetbrains.kotlin.arguments.LambdasMode",
    nameAccessor = LambdasMode::modeName,
    typeFinder = LambdasMode::modeName.typeFinder()
)

private object AllLambdasModeSerializer : AllNamedTypeSerializer<LambdasMode>(
    serialName = "org.jetbrains.kotlin.arguments.LambdasMode",
    jsonElementNameForName = "name",
    nameAccessor = LambdasMode::modeName,
    typeFinder = LambdasMode::modeName.typeFinder()
)

object AllDetailsLambdasModeSerializer : SetTypeSerializer<LambdasMode>(
    typeSerializer = AllLambdasModeSerializer,
    valueTypeQualifiedNamed = LambdasModeType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetLambdasMode",
)
