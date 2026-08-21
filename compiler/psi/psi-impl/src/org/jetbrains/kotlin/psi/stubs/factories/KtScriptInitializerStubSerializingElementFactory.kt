/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtScriptInitializer

internal object KtScriptInitializerStubSerializingElementFactory :
    KtPlaceHolderStubSerializingElementFactory<KtScriptInitializer>(
        type = KtNodeTypes.SCRIPT_INITIALIZER,
        psiFactory = ::KtScriptInitializer,
    ) {

    override fun shouldCreateStub(node: ASTNode): Boolean = true
}
