/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.JspecifyAnnotationsMode
import org.jetbrains.kotlin.arguments.dsl.types.JspecifyAnnotationsModeType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object KotlinJspecifyAnnotationsModeAsNameSerializer : NamedTypeSerializer<JspecifyAnnotationsMode>(
    serialName = "org.jetbrains.kotlin.arguments.JspecifyAnnotationsMode",
    nameAccessor = JspecifyAnnotationsMode::modeName,
    typeFinder = JspecifyAnnotationsMode::modeName.typeFinder()
)

private object AllJspecifyAnnotationsModeSerializer : AllNamedTypeSerializer<JspecifyAnnotationsMode>(
    serialName = "org.jetbrains.kotlin.arguments.JspecifyAnnotationsMode",
    jsonElementNameForName = "name",
    nameAccessor = JspecifyAnnotationsMode::modeName,
    typeFinder = JspecifyAnnotationsMode::modeName.typeFinder()
)

object AllDetailsJspecifyAnnotationsModeSerializer : SetTypeSerializer<JspecifyAnnotationsMode>(
    typeSerializer = AllJspecifyAnnotationsModeSerializer,
    valueTypeQualifiedNamed = JspecifyAnnotationsModeType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetJspecifyAnnotationsMode",
)
