/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.KlibIrInlinerMode
import org.jetbrains.kotlin.arguments.dsl.types.KlibIrInlinerModeType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

private const val SERIAL_NAME = "org.jetbrains.kotlin.arguments.KlibIrInlinerMode"
private const val SET_SERIAL_NAME = "org.jetbrains.kotlin.arguments.SetKlibIrInlinerMode"

object KotlinKlibIrInlinerModeAsNameSerializer : NamedTypeSerializer<KlibIrInlinerMode>(
    serialName = SERIAL_NAME,
    nameAccessor = KlibIrInlinerMode::modeState,
    typeFinder = KlibIrInlinerMode::modeState.typeFinder()
)

private object AllKlibIrInlinerModeSerializer : AllNamedTypeSerializer<KlibIrInlinerMode>(
    serialName = SERIAL_NAME,
    jsonElementNameForName = "name",
    nameAccessor = KlibIrInlinerMode::modeState,
    typeFinder = KlibIrInlinerMode::modeState.typeFinder()
)

object AllDetailsKlibIrInlinerModeSerializer : SetTypeSerializer<KlibIrInlinerMode>(
    typeSerializer = AllKlibIrInlinerModeSerializer,
    valueTypeQualifiedNamed = KlibIrInlinerModeType::class.qualifiedName!!,
    serialName = SET_SERIAL_NAME,
)
