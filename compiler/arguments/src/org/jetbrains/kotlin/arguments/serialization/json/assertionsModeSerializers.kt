/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.AssertionsMode
import org.jetbrains.kotlin.arguments.dsl.types.AssertionsModeType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object AssertionsModeAsNameSerializer : NamedTypeSerializer<AssertionsMode>(
    serialName = "org.jetbrains.kotlin.arguments.AssertionsMode",
    nameAccessor = AssertionsMode::modeName,
    typeFinder = AssertionsMode::modeName.typeFinder()
)

private object AllAssertionsModeSerializer : AllNamedTypeSerializer<AssertionsMode>(
    serialName = "org.jetbrains.kotlin.arguments.AssertionsMode",
    jsonElementNameForName = "name",
    nameAccessor = AssertionsMode::modeName,
    typeFinder = AssertionsMode::modeName.typeFinder()
)

object AllDetailsAssertionsModeSerializer : SetTypeSerializer<AssertionsMode>(
    typeSerializer = AllAssertionsModeSerializer,
    valueTypeQualifiedNamed = AssertionsModeType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetAssertionsMode",
)
