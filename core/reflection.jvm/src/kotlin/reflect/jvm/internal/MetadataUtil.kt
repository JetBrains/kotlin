/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import kotlin.metadata.ClassName
import kotlin.reflect.KClass

internal fun ClassName.toClassId(): ClassId {
    val isLocal = startsWith(".")
    val fullName = if (isLocal) substring(1) else this
    return ClassId(
        FqName(fullName.substringBeforeLast('/', "").replace('/', '.')),
        FqName(fullName.substringAfterLast('/')),
        isLocal,
    )
}

internal fun ClassName.toNonLocalSimpleName(): String {
    require(!startsWith(".")) { "Local class is not supported: $this" }
    return substringAfterLast('/').substringAfterLast('.')
}

internal fun ClassLoader.loadKClass(name: ClassName): KClass<*>? =
    loadClass(name.toClassId())?.kotlin
