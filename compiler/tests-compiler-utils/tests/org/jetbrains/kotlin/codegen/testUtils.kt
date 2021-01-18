/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.load.java.JvmAbi
import java.net.URL
import java.net.URLClassLoader

fun clearReflectionCache(classLoader: ClassLoader) {
    try {
        val klass = classLoader.loadClass(JvmAbi.REFLECTION_FACTORY_IMPL.asSingleFqName().asString())
        val method = klass.getDeclaredMethod("clearCaches")
        method.invoke(null)
    } catch (e: ClassNotFoundException) {
        // This is OK for a test without kotlin-reflect in the dependencies
    }
}

fun ClassLoader?.extractUrls(): List<URL> {
    return (this as? URLClassLoader)?.let {
        it.urLs.toList() + it.parent.extractUrls()
    } ?: emptyList()
}
