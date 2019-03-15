/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

fun Class<*>.getMethodOrNull(name: String, vararg parameterTypes: Class<*>) =
    try {
        getMethod(name, *parameterTypes)
    } catch (e: Exception) {
        null
    }

fun Class<*>.getDeclaredMethodOrNull(name: String, vararg parameterTypes: Class<*>) =
    try {
        getDeclaredMethod(name, *parameterTypes)?.also { it.isAccessible = true }
    } catch (e: Exception) {
        null
    }

fun compilationFullName(simpleName: String, classifier: String?) =
    if (classifier != null) classifier + simpleName.capitalize() else simpleName