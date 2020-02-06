/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.impl.carriers

// TODO degenerify
// TODO degenerify
interface Carrier<in T : Carrier<T>> {
    val lastModified: Int

    fun clone(): Carrier<T>
}