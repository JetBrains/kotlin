/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:JvmName("SharedApiClassesClassLoader")

package org.jetbrains.kotlin.buildtools.api

import java.util.*
import kotlin.reflect.KClass

/**
 * Creates a [ClassLoader] which reuses the API classes from the ClassLoader which loaded the API.
 * This way an API implementation can be loaded with almost fully isolated classpath, sharing only the classes from `org.jetbrains.kotlin.buildtools.api`,
 * so a caller still able to pass API parameters in a compatible way.
 */
@Suppress("FunctionName")
@JvmName("newInstance")
public fun SharedApiClassesClassLoader(): ClassLoader = SharedApiClassesClassLoaderImpl(
    SharedApiClassesClassLoaderImpl::class.java.classLoader,
    ClassLoader.getSystemClassLoader(),
    SharedApiClassesClassLoaderImpl::class.java.`package`.name,
)

internal fun <T : Any> loadImplementation(cls: KClass<T>, classLoader: ClassLoader): T {
    val implementations = ServiceLoader.load(cls.java, classLoader)
    implementations.firstOrNull() ?: error("The classpath contains no implementation for ${cls.qualifiedName}")
    return implementations.singleOrNull()
        ?: error("The classpath contains more than one implementation for ${cls.qualifiedName}")
}

private class SharedApiClassesClassLoaderImpl(
    private val parent: ClassLoader,
    fallback: ClassLoader,
    private val allowedPackage: String,
) : ClassLoader(fallback) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        return if (name.startsWith(allowedPackage)) {
            parent.loadClass(name)
        } else {
            super.loadClass(name, resolve)
        }
    }
}