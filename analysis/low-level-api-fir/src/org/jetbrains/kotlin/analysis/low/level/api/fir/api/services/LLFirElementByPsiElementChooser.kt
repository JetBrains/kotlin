/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.services

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtTypeParameter

/**
 * [LLFirElementByPsiElementChooser] helps with choosing a [org.jetbrains.kotlin.fir.FirElement] from multiple *sibling* FIR elements that
 * matches a given [org.jetbrains.kotlin.psi.KtElement].
 *
 * The chooser requires the FIR elements to belong to the same conceptual parent (such as top-level scope, class, parameter list, etc.), as
 * the algorithm is only required to consider structural aspects of the given elements, but not their parents.
 *
 * Using this service only makes sense when a FIR element may not have an underlying PSI, which may be the case for deserialized elements.
 * When elements are deserialized from stubs, this issue does not occur because the PSI is provided during deserialization. However, in
 * Standalone mode, deserialized symbols do not have sources and require more sophisticated choosing logic.
 */
abstract class LLFirElementByPsiElementChooser {
    abstract fun isMatchingValueParameter(psi: KtParameter, fir: FirValueParameter): Boolean

    abstract fun isMatchingTypeParameter(psi: KtTypeParameter, fir: FirTypeParameter): Boolean

    abstract fun isMatchingEnumEntry(psi: KtEnumEntry, fir: FirEnumEntry): Boolean

    abstract fun isMatchingCallableDeclaration(psi: KtCallableDeclaration, fir: FirCallableDeclaration): Boolean

    companion object {
        fun getInstance(project: Project): LLFirElementByPsiElementChooser =
            project.getService(LLFirElementByPsiElementChooser::class.java)
    }
}
