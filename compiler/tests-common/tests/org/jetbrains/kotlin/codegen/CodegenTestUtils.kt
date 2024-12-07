/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import java.lang.reflect.Method

private fun ClassLoader.loadClassOrNull(name: String): Class<*>? =
    try {
        loadClass(name)
    } catch (e: ClassNotFoundException) {
        null
    }

private fun Class<*>.getMethodOrNull(name: String, vararg parameterTypes: Class<*>): Method? =
    try {
        getMethod(name, *parameterTypes)
    } catch (e: NoSuchMethodException) {
        null
    }

fun getGeneratedClass(classLoader: ClassLoader, className: String): Class<*> =
    classLoader.loadClassOrNull(className) ?: error("No class file was generated for: $className")

fun getBoxMethodOrNull(aClass: Class<*>): Method? =
    aClass.getMethodOrNull("box")
        ?: aClass.classLoader.loadClassOrNull("kotlin.coroutines.Continuation")?.let { aClass.getMethodOrNull("box", it) }
        ?: aClass.classLoader.loadClassOrNull("kotlin.coroutines.experimental.Continuation")?.let { aClass.getMethodOrNull("box", it) }

fun runBoxMethod(method: Method): String? {
    if (method.parameterTypes.isEmpty()) {
        return method.invoke(null) as? String
    }
    val emptyContinuationClass = method.declaringClass.classLoader.loadClass("helpers.ResultContinuation")
    val emptyContinuation = emptyContinuationClass.declaredConstructors.single().newInstance()
    val result = method.invoke(null, emptyContinuation)
    val resultAfterSuspend = emptyContinuationClass.getField("result").get(emptyContinuation)
    return (resultAfterSuspend ?: result) as? String
}
