/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.tokens.HackToForceAllowRunningAnalyzeOnEDT
import org.jetbrains.kotlin.idea.frontend.api.tokens.hackyAllowRunningOnEdt
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtPsiFactory

@OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
fun KtCallableDeclaration.setType(typeString: String, classId: ClassId?, shortenReferences: Boolean = true) {
    val typeReference = KtPsiFactory(project).createType(typeString)
    setTypeReference(typeReference)
    if (shortenReferences && classId != null) {
        hackyAllowRunningOnEdt {
            analyse(this) {
                collectPossibleReferenceShortenings(
                    containingKtFile,
                    getTypeReference()!!.textRange,
                ).invokeShortening()
            }
        }
    }
}
