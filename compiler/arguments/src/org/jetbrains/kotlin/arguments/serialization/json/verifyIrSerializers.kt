/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.VerifyIrMode
import org.jetbrains.kotlin.arguments.dsl.types.VerifyIrModeType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object KotlinVerifyIrModeAsNameSerializer : NamedTypeSerializer<VerifyIrMode>(
    serialName = "org.jetbrains.kotlin.arguments.VerifyIrMode",
    nameAccessor = VerifyIrMode::modeName,
    typeFinder = VerifyIrMode::modeName.typeFinder()
)

private object AllVerifyIrModeSerializer : AllNamedTypeSerializer<VerifyIrMode>(
    serialName = "org.jetbrains.kotlin.arguments.VerifyIrMode",
    jsonElementNameForName = "name",
    nameAccessor = VerifyIrMode::modeName,
    typeFinder = VerifyIrMode::modeName.typeFinder()
)

object AllDetailsVerifyIrModeSerializer : SetTypeSerializer<VerifyIrMode>(
    typeSerializer = AllVerifyIrModeSerializer,
    valueTypeQualifiedNamed = VerifyIrModeType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetVerifyIrMode",
)
