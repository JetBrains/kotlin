/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

/**
 * Provides additional information for [SymbolLightPsiJavaCodeReferenceElementBase][org.jetbrains.kotlin.light.classes.symbol.codeReferences.SymbolLightPsiJavaCodeReferenceElementBase]
 * to reach better API coverage of [com.intellij.psi.PsiReference]
 *
 * @see org.jetbrains.kotlin.light.classes.symbol.codeReferences.SymbolLightPsiJavaCodeReferenceElementBase
 * @see ReferenceInformationHolder
 */
internal interface ReferenceInformationProvider {
    /**
     * @see com.intellij.psi.PsiQualifiedReference.getReferenceName
     */
    val referenceName: String?
}

/**
 * @see ReferenceInformationProvider
 */
internal class ReferenceInformationHolder(override val referenceName: String?) : ReferenceInformationProvider