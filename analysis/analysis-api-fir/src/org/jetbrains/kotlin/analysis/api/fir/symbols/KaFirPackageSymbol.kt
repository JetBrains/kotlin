/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.file.PsiPackageImpl
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KaFirPackageSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.platform.packages.createPackageProvider
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.name.FqName

internal class KaFirPackageSymbol(
    override val fqName: FqName,
    private val project: Project,
    override val token: KaLifetimeToken,
) : KaPackageSymbol(), KaLifetimeOwner {
    override val psi: PsiElement? by cached {
        JavaPsiFacade.getInstance(project).findPackage(fqName.asString())
            ?: KtPackage(PsiManager.getInstance(project), fqName, GlobalSearchScope.allScope(project)/*TODO*/)
    }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion { KaSymbolOrigin.SOURCE } // TODO

    override fun createPointer(): KaSymbolPointer<KaPackageSymbol> = withValidityAssertion {
        KaFirPackageSymbolPointer(fqName, this)
    }

    override fun equals(other: Any?): Boolean = this === other || other is KaFirPackageSymbol && other.fqName == fqName
    override fun hashCode(): Int = fqName.hashCode()
}

private class KtPackage(
    manager: PsiManager,
    private val fqName: FqName,
    private val scope: GlobalSearchScope,
) : PsiPackageImpl(manager, fqName.asString().replace('/', '.')) {
    override fun copy() = KtPackage(manager, fqName, scope)
    override fun isValid(): Boolean = project.createPackageProvider(scope).doesKotlinOnlyPackageExist(fqName)
}
