/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.file.PsiPackageImpl
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirPackageSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.providers.createPackageProvider
import org.jetbrains.kotlin.name.FqName

class KtFirPackageSymbol(
    override val fqName: FqName,
    private val project: Project,
    override val token: KtLifetimeToken
) : KtPackageSymbol(), KtLifetimeOwner {
    override val psi: PsiElement? by cached {
        JavaPsiFacade.getInstance(project).findPackage(fqName.asString())
            ?: KtPackage(PsiManager.getInstance(project), fqName, GlobalSearchScope.allScope(project)/*TODO*/)
    }

    override val origin: KtSymbolOrigin
        get() = withValidityAssertion { KtSymbolOrigin.SOURCE } // TODO

    context(KtAnalysisSession)
    override fun createPointer(): KtSymbolPointer<KtPackageSymbol> = KtFirPackageSymbolPointer(fqName)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KtFirPackageSymbol

        if (fqName != other.fqName) return false
        if (token != other.token) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fqName.hashCode()
        result = 31 * result + token.hashCode()
        return result
    }
}

class KtPackage(
    manager: PsiManager,
    private val fqName: FqName,
    private val scope: GlobalSearchScope
) : PsiPackageImpl(manager, fqName.asString().replace('/', '.')) {
    override fun copy() = KtPackage(manager, fqName, scope)

    override fun isValid(): Boolean = project.createPackageProvider(scope).doKotlinPackageExists(fqName)
}
