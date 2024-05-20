/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KaFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.pointers.KaFe10PackageSymbolPointer
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.name.FqName

internal class KaFe10PackageSymbol(
    private val packageName: FqName,
    override val analysisContext: Fe10AnalysisContext
) : KaPackageSymbol(), KaFe10Symbol {
    override val fqName: FqName
        get() = withValidityAssertion { packageName }

    override val psi: PsiElement? by cached {
        val project = analysisContext.resolveSession.project
        JavaPsiFacade.getInstance(project).findPackage(fqName.asString())
    }

    override fun createPointer(): KaSymbolPointer<KaPackageSymbol> = withValidityAssertion {
        KaFe10PackageSymbolPointer(fqName)
    }

    override val origin: KaSymbolOrigin
        get() = withValidityAssertion {
            val virtualFile = PsiUtilCore.getVirtualFile(psi)
            return if (virtualFile != null) {
                analysisContext.getOrigin(virtualFile)
            } else {
                KaSymbolOrigin.LIBRARY
            }
        }

    override fun hashCode(): Int {
        return packageName.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return packageName == (other as? KaFe10PackageSymbol)?.fqName
    }
}