/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.java.source

import com.intellij.psi.PsiType
import com.intellij.psi.SmartTypePointer
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementTypeSource

internal class JavaElementTypeSourceWithSmartPointer<TYPE : PsiType>(
    private val pointer: SmartTypePointer,
    override val factory: JavaElementSourceFactory,
) : JavaElementTypeSource<TYPE>() {
    override val type: TYPE
        get() {
            val type = pointer.type
                ?: error("Cannot restore a PsiType from $pointer")

            @Suppress("UNCHECKED_CAST")
            return type as TYPE
        }

    override fun toString(): String {
        return pointer.type?.toString() ?: "Cannot restore a PsiType from $pointer"
    }
}