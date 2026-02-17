/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.CompatqualAnnotationsMode
import org.jetbrains.kotlin.arguments.dsl.types.CompatqualAnnotationsModeType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object KotlinCompatqualAnnotationsModeAsNameSerializer : NamedTypeSerializer<CompatqualAnnotationsMode>(
    serialName = "org.jetbrains.kotlin.arguments.CompatqualAnnotationsMode",
    nameAccessor = CompatqualAnnotationsMode::modeName,
    typeFinder = CompatqualAnnotationsMode::modeName.typeFinder()
)

private object AllCompatqualAnnotationsModeSerializer : AllNamedTypeSerializer<CompatqualAnnotationsMode>(
    serialName = "org.jetbrains.kotlin.arguments.CompatqualAnnotationsMode",
    jsonElementNameForName = "name",
    nameAccessor = CompatqualAnnotationsMode::modeName,
    typeFinder = CompatqualAnnotationsMode::modeName.typeFinder()
)

object AllDetailsCompatqualAnnotationsModeSerializer : SetTypeSerializer<CompatqualAnnotationsMode>(
    typeSerializer = AllCompatqualAnnotationsModeSerializer,
    valueTypeQualifiedNamed = CompatqualAnnotationsModeType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetCompatqualAnnotationsMode",
)
