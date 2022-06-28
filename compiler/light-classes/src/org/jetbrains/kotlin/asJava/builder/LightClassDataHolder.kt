/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.builder

import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics

interface LightClassDataHolder {
    val javaFileStub: PsiJavaFileStub
    val extraDiagnostics: Diagnostics

    interface ForClass : LightClassDataHolder
    interface ForFacade : LightClassDataHolder
    interface ForScript : ForClass
}

object InvalidLightClassDataHolder : LightClassDataHolder.ForClass {
    override val javaFileStub: PsiJavaFileStub
        get() = shouldNotBeCalled()

    override val extraDiagnostics: Diagnostics
        get() = shouldNotBeCalled()

    private fun shouldNotBeCalled(): Nothing = throw UnsupportedOperationException("Should not be called")
}

class LightClassDataHolderImpl(
    override val javaFileStub: PsiJavaFileStub,
    override val extraDiagnostics: Diagnostics
) : LightClassDataHolder.ForClass, LightClassDataHolder.ForFacade, LightClassDataHolder.ForScript
