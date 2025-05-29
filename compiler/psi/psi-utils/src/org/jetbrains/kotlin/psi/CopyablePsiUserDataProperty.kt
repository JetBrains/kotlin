/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import kotlin.reflect.KProperty

class CopyablePsiUserDataProperty<in R : PsiElement, T : Any>(val key: Key<T>) {
    operator fun getValue(thisRef: R, property: KProperty<*>) = thisRef.getCopyableUserData(key)

    operator fun setValue(thisRef: R, property: KProperty<*>, value: T?) = thisRef.putCopyableUserData(key, value)
}