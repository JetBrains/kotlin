// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
@file:JvmName("KotlinStubUtils")
package org.jetbrains.kotlin.analysis.api.dumdum.stubindex

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.stubs.KotlinFileStub

val StubBasedPsiElementBase<*>.containingKotlinFileStub: PsiFileStub<*>?
    get() = containingFileStub as? KotlinFileStub

val StubBasedPsiElementBase<*>.containingFileStub: PsiFileStub<*>?
    get() {
        val stub = this.greenStub ?: return null
        return stub.containingFileStub
    }

val StubElement<*>.containingKotlinFileStub: KotlinFileStub?
    get() = containingFileStub as? KotlinFileStub