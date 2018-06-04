/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import org.mockito.Mockito

fun <T> anyMockitoInstance(): T {
    Mockito.any<T>()
    // Works because of KT-8135 and people use it
    return uninitialized()
}

private fun <T> uninitialized(): T = null as T
