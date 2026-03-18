/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.SamConversionsMode
import org.jetbrains.kotlin.arguments.dsl.types.SamConversionsModeType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object KotlinSamConversionsModeAsNameSerializer : NamedTypeSerializer<SamConversionsMode>(
    serialName = "org.jetbrains.kotlin.arguments.SamConversionsMode",
    nameAccessor = SamConversionsMode::modeName,
    typeFinder = SamConversionsMode::modeName.typeFinder()
)

private object AllSamConversionsModeSerializer : AllNamedTypeSerializer<SamConversionsMode>(
    serialName = "org.jetbrains.kotlin.arguments.SamConversionsMode",
    jsonElementNameForName = "name",
    nameAccessor = SamConversionsMode::modeName,
    typeFinder = SamConversionsMode::modeName.typeFinder()
)

object AllDetailsSamConversionsModeSerializer : SetTypeSerializer<SamConversionsMode>(
    typeSerializer = AllSamConversionsModeSerializer,
    valueTypeQualifiedNamed = SamConversionsModeType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetSamConversionsMode",
)
