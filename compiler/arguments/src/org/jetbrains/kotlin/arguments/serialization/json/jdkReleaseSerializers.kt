/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.serialization.json

import org.jetbrains.kotlin.arguments.dsl.types.JdkRelease
import org.jetbrains.kotlin.arguments.dsl.types.JdkReleaseType
import org.jetbrains.kotlin.arguments.serialization.json.base.AllNamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.NamedTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.SetTypeSerializer
import org.jetbrains.kotlin.arguments.serialization.json.base.typeFinder

object KotlinJdkReleaseAsNameSerializer : NamedTypeSerializer<JdkRelease>(
    serialName = "org.jetbrains.kotlin.arguments.JdkRelease",
    nameAccessor = JdkRelease::releaseName,
    typeFinder = JdkRelease::releaseName.typeFinder()
)

private object AllJdkReleaseSerializer : AllNamedTypeSerializer<JdkRelease>(
    serialName = "org.jetbrains.kotlin.arguments.JdkRelease",
    jsonElementNameForName = "name",
    nameAccessor = JdkRelease::releaseName,
    typeFinder = JdkRelease::releaseName.typeFinder()
)

object AllDetailsJdkReleaseSerializer : SetTypeSerializer<JdkRelease>(
    typeSerializer = AllJdkReleaseSerializer,
    valueTypeQualifiedNamed = JdkReleaseType::class.qualifiedName!!,
    serialName = "org.jetbrains.kotlin.arguments.SetJdkRelease",
)
