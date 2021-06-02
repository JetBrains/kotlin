/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization.builtins

import java.io.InputStream

class BuiltInsResourceLoader {
    fun loadResource(path: String): InputStream? {
        val classLoader = this::class.java.classLoader ?: return ClassLoader.getSystemResourceAsStream(path)

        // Do not use getResourceAsStream because URLClassLoader's implementation creates InputStream instances which refer to
        // a globally cached JarFile instance, which is closed as soon as URLClassLoader is closed, which instantly invalidates all
        // input streams referring to that JarFile and breaks kotlin-reflect in case it's used from different class loaders.
        val resource = classLoader.getResource(path) ?: return null
        return resource.openConnection().apply { useCaches = false }.getInputStream()
    }
}
