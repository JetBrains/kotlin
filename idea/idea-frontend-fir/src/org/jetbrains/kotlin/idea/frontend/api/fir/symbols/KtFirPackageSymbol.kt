/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.file.PsiPackageImpl
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.name.FqName

class KtFirPackageSymbol(
    override val fqName: FqName,
    private val project: Project,
    override val token: ValidityToken
) : KtPackageSymbol(), ValidityTokenOwner {
    override val psi: PsiElement? by cached {
        PsiPackageImpl(PsiManager.getInstance(project), fqName.asString().replace('/', '.'))
    }

    override val origin: KtSymbolOrigin
        get() = KtSymbolOrigin.SOURCE // TODO

    override fun createPointer(): KtSymbolPointer<KtPackageSymbol> = symbolPointer { session ->
        check(session is KtFirAnalysisSession)
        session.firSymbolBuilder.createPackageSymbolIfOneExists(fqName)
    }
}