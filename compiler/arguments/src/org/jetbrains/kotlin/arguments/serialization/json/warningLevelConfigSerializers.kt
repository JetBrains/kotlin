/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.WarningLevel
import org.jetbrains.kotlin.arguments.dsl.types.WarningLevelConfig
import org.jetbrains.kotlin.arguments.dsl.types.WarningLevelConfigListType
import org.jetbrains.kotlin.arguments.serialization.json.base.*

object WarningLevelConfigSerializer : CustomTypeSerializer<WarningLevelConfig>(
    WarningLevelConfigListType::class.qualifiedName!!,
    WarningLevelConfig::class.qualifiedName!!
)

object KotlinWarningLevelAsNameSerializer : NamedTypeSerializer<WarningLevel>(
    serialName = "org.jetbrains.kotlin.arguments.WarningLevel",
    nameAccessor = WarningLevel::level,
    typeFinder = WarningLevel::level.typeFinder()
)

private object AllWarningLevelSerializer : AllNamedTypeSerializer<WarningLevel>(
    serialName = "org.jetbrains.kotlin.arguments.WarningLevel",
    jsonElementNameForName = "name",
    nameAccessor = WarningLevel::level,
    typeFinder = WarningLevel::level.typeFinder()
)

object AllDetailsWarningLevelSerializer : SetTypeSerializer<WarningLevel>(
    typeSerializer = AllWarningLevelSerializer,
    valueTypeQualifiedNamed = WarningLevel::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetWarningLevel",
)
