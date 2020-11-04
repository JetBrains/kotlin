/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.structure

import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

internal object FileStructureUtil {
    fun isStructureElementContainer(ktDeclaration: KtDeclaration): Boolean = when {
        ktDeclaration !is KtClassOrObject && ktDeclaration !is KtDeclarationWithBody && ktDeclaration !is KtProperty && ktDeclaration !is KtTypeAlias -> false
        ktDeclaration is KtEnumEntry -> false
        ktDeclaration.containingClassOrObject is KtEnumEntry -> false
        else -> !KtPsiUtil.isLocal(ktDeclaration)
    }
}