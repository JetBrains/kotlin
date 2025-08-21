/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.ReturnValueCheckerMode
import org.jetbrains.kotlin.arguments.dsl.types.ReturnValueCheckerModeType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

private const val SERIAL_NAME = "org.jetbrains.kotlin.arguments.ReturnValueCheckerMode"
private const val SET_SERIAL_NAME = "org.jetbrains.kotlin.arguments.SetReturnValueCheckerMode"

object KotlinReturnValueCheckerModeAsNameSerializer : NamedTypeSerializer<ReturnValueCheckerMode>(
    serialName = SERIAL_NAME,
    nameAccessor = ReturnValueCheckerMode::modeState,
    typeFinder = ReturnValueCheckerMode::modeState.typeFinder()
)

private object AllReturnValueCheckerModeSerializer : AllNamedTypeSerializer<ReturnValueCheckerMode>(
    serialName = SERIAL_NAME,
    jsonElementNameForName = "name",
    nameAccessor = ReturnValueCheckerMode::modeState,
    typeFinder = ReturnValueCheckerMode::modeState.typeFinder()
)

object AllDetailsReturnValueCheckerModeSerializer : SetTypeSerializer<ReturnValueCheckerMode>(
    typeSerializer = AllReturnValueCheckerModeSerializer,
    valueTypeQualifiedNamed = ReturnValueCheckerModeType::class.qualifiedName!!,
    serialName = SET_SERIAL_NAME,
)
