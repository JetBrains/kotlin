/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.symbols

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.file.PsiPackageImpl
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.symbolPointer
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil
import org.jetbrains.kotlin.name.FqName

class KtFirPackageSymbol(
    override val fqName: FqName,
    private val project: Project,
    override val token: ValidityToken
) : KtPackageSymbol(), ValidityTokenOwner {
    override val psi: PsiElement? by cached {
        KtPackage(PsiManager.getInstance(project), fqName, GlobalSearchScope.allScope(project)/*TODO*/)
    }

    override val origin: KtSymbolOrigin
        get() = KtSymbolOrigin.SOURCE // TODO

    override fun createPointer(): KtSymbolPointer<KtPackageSymbol> = symbolPointer { session ->
        check(session is KtFirAnalysisSession)
        session.firSymbolBuilder.createPackageSymbolIfOneExists(fqName)
    }
}

class KtPackage(
    manager: PsiManager,
    private val fqName: FqName,
    private val scope: GlobalSearchScope
) : PsiPackageImpl(manager, fqName.asString().replace('/', '.')) {
    override fun copy() = KtPackage(manager, fqName, scope)

    override fun isValid(): Boolean = PackageIndexUtil.packageExists(fqName, scope, project)
}
