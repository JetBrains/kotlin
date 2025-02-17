/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.testFramework

import com.intellij.openapi.application.Application
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.utils.rethrow

fun resetApplicationToNull(old: Application?) {
    if (old != null) return
    resetApplicationToNull()
}

fun resetApplicationToNull() {
    try {
        KotlinCoreEnvironment.resetApplicationManager()
    } catch (e: Exception) {
        throw rethrow(e)
    }
}