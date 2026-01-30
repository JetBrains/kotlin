/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.CompatqualCheckerFrameworkAnnotations
import org.jetbrains.kotlin.arguments.dsl.types.CompatqualCheckerFrameworkAnnotationsType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object CompatqualCheckerFrameworkAnnotationsAsNameSerializer : NamedTypeSerializer<CompatqualCheckerFrameworkAnnotations>(
    serialName = "org.jetbrains.kotlin.arguments.CompatqualCheckerFrameworkAnnotations",
    nameAccessor = CompatqualCheckerFrameworkAnnotations::stateName,
    typeFinder = CompatqualCheckerFrameworkAnnotations::stateName.typeFinder()
)

private object AllCompatqualCheckerFrameworkAnnotationsSerializer : AllNamedTypeSerializer<CompatqualCheckerFrameworkAnnotations>(
    serialName = "org.jetbrains.kotlin.arguments.CompatqualCheckerFrameworkAnnotations",
    jsonElementNameForName = "name",
    nameAccessor = CompatqualCheckerFrameworkAnnotations::stateName,
    typeFinder = CompatqualCheckerFrameworkAnnotations::stateName.typeFinder()
)

object AllDetailsCompatqualCheckerFrameworkAnnotationsSerializer : SetTypeSerializer<CompatqualCheckerFrameworkAnnotations>(
    typeSerializer = AllCompatqualCheckerFrameworkAnnotationsSerializer,
    valueTypeQualifiedNamed = CompatqualCheckerFrameworkAnnotationsType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetCompatqualCheckerFrameworkAnnotations",
)
