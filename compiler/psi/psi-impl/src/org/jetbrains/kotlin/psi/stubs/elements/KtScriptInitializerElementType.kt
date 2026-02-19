/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtScriptInitializer

object KtScriptInitializerElementType : KtPlaceHolderStubElementType<KtScriptInitializer>(
    "SCRIPT_INITIALIZER", KtScriptInitializer::class.java,
) {
    override fun shouldCreateStub(node: ASTNode): Boolean = true
}
