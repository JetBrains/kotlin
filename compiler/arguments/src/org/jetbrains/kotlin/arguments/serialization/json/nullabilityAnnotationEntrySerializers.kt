/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.NullabilityAnnotationConfig
import org.jetbrains.kotlin.arguments.dsl.types.NullabilityAnnotationListType
import org.jetbrains.kotlin.arguments.dsl.types.NullabilityAnnotationMode
import org.jetbrains.kotlin.arguments.serialization.json.base.*

object NullabilityAnnotationConfigSerializer : CustomTypeSerializer<NullabilityAnnotationConfig>(
    NullabilityAnnotationListType::class.qualifiedName!!,
    NullabilityAnnotationConfig::class.qualifiedName!!
)

object KotlinNullabilityAnnotationModeAsNameSerializer : NamedTypeSerializer<NullabilityAnnotationMode>(
    serialName = "org.jetbrains.kotlin.arguments.NullabilityAnnotationMode",
    nameAccessor = NullabilityAnnotationMode::modeName,
    typeFinder = NullabilityAnnotationMode::modeName.typeFinder()
)

private object AllNullabilityAnnotationModeSerializer : AllNamedTypeSerializer<NullabilityAnnotationMode>(
    serialName = "org.jetbrains.kotlin.arguments.NullabilityAnnotationMode",
    jsonElementNameForName = "name",
    nameAccessor = NullabilityAnnotationMode::modeName,
    typeFinder = NullabilityAnnotationMode::modeName.typeFinder()
)

object AllDetailsNullabilityAnnotationModeSerializer : SetTypeSerializer<NullabilityAnnotationMode>(
    typeSerializer = AllNullabilityAnnotationModeSerializer,
    valueTypeQualifiedNamed = NullabilityAnnotationMode::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetNullabilityAnnotationMode",
)
