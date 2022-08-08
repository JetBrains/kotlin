/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.reflect.KDeclarationContainer


private val K_CLASS_CACHE = createCache { KClassImpl(it) }
private val K_PACKAGE_CACHE = createCache { KPackageImpl(it) }

// This function is invoked on each reflection access to Java classes, properties, etc. Performance is critical here.
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> getOrCreateKotlinClass(jClass: Class<T>): KClassImpl<T> = K_CLASS_CACHE.get(jClass) as KClassImpl<T>

internal fun <T : Any> getOrCreateKotlinPackage(jClass: Class<T>): KDeclarationContainer = K_PACKAGE_CACHE.get(jClass)

internal fun clearCaches() {
    K_CLASS_CACHE.clear()
    K_PACKAGE_CACHE.clear()
}
