/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.testFramework.ExtensionTestUtil

fun <T> maskExtensions(
    pointName: ExtensionPointName<T>,
    newExtensions: List<T>,
    parentDisposable: Disposable
) {
    ExtensionTestUtil.maskExtensions(pointName, newExtensions, parentDisposable)
}