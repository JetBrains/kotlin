/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.modification

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.platform.modification.KaElementModificationType
import org.jetbrains.kotlin.analysis.api.platform.modification.KaSourceModificationService
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLFirDeclarationModificationService

@OptIn(LLFirInternals::class)
internal class KaFirSourceModificationService(private val project: Project) : KaSourceModificationService {
    override fun handleElementModification(
        element: PsiElement,
        modificationType: KaElementModificationType,
    ) {
        LLFirDeclarationModificationService.getInstance(project).elementModified(element, modificationType)
    }

    override fun ancestorAffectedByInBlockModification(element: PsiElement): PsiElement? =
        LLFirDeclarationModificationService.getInstance(project).ancestorAffectedByInBlockModification(element)
}
