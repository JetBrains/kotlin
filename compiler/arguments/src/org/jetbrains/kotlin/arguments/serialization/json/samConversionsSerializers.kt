/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.SamConversions
import org.jetbrains.kotlin.arguments.dsl.types.SamConversionsType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object SamConversionsAsNameSerializer : NamedTypeSerializer<SamConversions>(
    serialName = "org.jetbrains.kotlin.arguments.SamConversions",
    nameAccessor = SamConversions::schemeName,
    typeFinder = SamConversions::schemeName.typeFinder()
)

private object AllSamConversionsSerializer : AllNamedTypeSerializer<SamConversions>(
    serialName = "org.jetbrains.kotlin.arguments.SamConversions",
    jsonElementNameForName = "name",
    nameAccessor = SamConversions::schemeName,
    typeFinder = SamConversions::schemeName.typeFinder()
)

object AllDetailsSamConversionsSerializer : SetTypeSerializer<SamConversions>(
    typeSerializer = AllSamConversionsSerializer,
    valueTypeQualifiedNamed = SamConversionsType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetSamConversions",
)
