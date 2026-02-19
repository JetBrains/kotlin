/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.KotlinVersion
import org.jetbrains.kotlin.arguments.dsl.types.KotlinVersionType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object KotlinVersionAsNameSerializer : NamedTypeSerializer<KotlinVersion>(
    serialName = "org.jetbrains.kotlin.arguments.KotlinVersion",
    nameAccessor = KotlinVersion::versionName,
    typeFinder = KotlinVersion::versionName.typeFinder()
)

private object AllKotlinVersionSerializer : AllNamedTypeSerializer<KotlinVersion>(
    serialName = "org.jetbrains.kotlin.arguments.KotlinVersion",
    jsonElementNameForName = "name",
    nameAccessor = KotlinVersion::versionName,
    typeFinder = KotlinVersion::versionName.typeFinder()
)


object AllDetailsKotlinVersionSerializer : SetTypeSerializer<KotlinVersion>(
    typeSerializer = AllKotlinVersionSerializer,
    valueTypeQualifiedNamed = KotlinVersionType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetKotlinVersion"
)
