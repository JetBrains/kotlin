/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.JspecifyAnnotations
import org.jetbrains.kotlin.arguments.dsl.types.JspecifyAnnotationsType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object JspecifyAnnotationsAsNameSerializer : NamedTypeSerializer<JspecifyAnnotations>(
    serialName = "org.jetbrains.kotlin.arguments.JspecifyAnnotations",
    nameAccessor = JspecifyAnnotations::stateName,
    typeFinder = JspecifyAnnotations::stateName.typeFinder()
)

private object AllJspecifyAnnotationsSerializer : AllNamedTypeSerializer<JspecifyAnnotations>(
    serialName = "org.jetbrains.kotlin.arguments.JspecifyAnnotations",
    jsonElementNameForName = "name",
    nameAccessor = JspecifyAnnotations::stateName,
    typeFinder = JspecifyAnnotations::stateName.typeFinder()
)

object AllDetailsJspecifyAnnotationsSerializer : SetTypeSerializer<JspecifyAnnotations>(
    typeSerializer = AllJspecifyAnnotationsSerializer,
    valueTypeQualifiedNamed = JspecifyAnnotationsType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetJspecifyAnnotations",
)
