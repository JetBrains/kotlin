/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.JvmTarget
import org.jetbrains.kotlin.arguments.dsl.types.KotlinJvmTargetType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object KotlinJvmTargetAsNameSerializer : NamedTypeSerializer<JvmTarget>(
    serialName = "org.jetbrains.kotlin.arguments.JvmTarget",
    nameAccessor = JvmTarget::targetName,
    typeFinder = JvmTarget::targetName.typeFinder()
)

private object AllJvmTargetSerializer : AllNamedTypeSerializer<JvmTarget>(
    serialName = "org.jetbrains.kotlin.arguments.JvmTarget",
    jsonElementNameForName = "name",
    nameAccessor = JvmTarget::targetName,
    typeFinder = JvmTarget::targetName.typeFinder()
)

object AllDetailsJvmTargetSerializer : SetTypeSerializer<JvmTarget>(
    typeSerializer = AllJvmTargetSerializer,
    valueTypeQualifiedNamed = KotlinJvmTargetType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetJvmTarget",
)
