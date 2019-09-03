/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.cfg.pseudocode.containingDeclarationForPseudocode
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPsiUtil

fun KtElement.containingNonLocalDeclaration(): KtDeclaration? {
    var container = this.containingDeclarationForPseudocode
    while (container != null && KtPsiUtil.isLocal(container)) {
        container = container.containingDeclarationForPseudocode
    }
    return container
}