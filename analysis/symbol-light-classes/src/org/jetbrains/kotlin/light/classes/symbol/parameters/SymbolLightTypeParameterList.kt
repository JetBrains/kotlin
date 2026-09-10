/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.PsiTypeParameterListOwner
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner

internal class SymbolLightTypeParameterList(
    owner: PsiTypeParameterListOwner,
    private val symbolWithTypeParameterPointer: KaSymbolPointer<KaDeclarationSymbol>,
    internal val ktModule: KaModule,
    private val ktDeclaration: KtTypeParameterListOwner?,
) : SymbolLightTypeParameterListBase<PsiTypeParameterListOwner>(owner) {
    override val typeParametersCollection: Collection<PsiTypeParameter> by lazyPub {
        symbolWithTypeParameterPointer.withSymbol(ktModule) { symbol ->
            val parentInterface =
                (owner as? SymbolLightMethodBase)?.containingClass?.interfaceIfDefaultImpls

            val fromInterface = parentInterface?.typeParameters?.mapNotNull {
                (it as? SymbolLightTypeParameter)?.copyTo(this@SymbolLightTypeParameterList)
            }.orEmpty()

            fromInterface + symbol.typeParameters.mapIndexed { index, parameter ->
                SymbolLightTypeParameter(
                    parent = this@SymbolLightTypeParameterList,
                    index = fromInterface.size + index,
                    typeParameterSymbol = parameter,
                )
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightTypeParameterList || other.ktModule != ktModule) return false
        if (ktDeclaration != null || other.ktDeclaration != null) {
            return other.ktDeclaration == ktDeclaration
        }

        return other.owner == owner && compareSymbolPointers(symbolWithTypeParameterPointer, other.symbolWithTypeParameterPointer)
    }

    override fun hashCode(): Int = ktDeclaration.hashCode() + 1

    override fun getText(): String? = ktDeclaration?.typeParameterList?.text
    override fun getTextOffset(): Int = ktDeclaration?.typeParameterList?.textOffset ?: -1
    override fun getStartOffsetInParent(): Int = ktDeclaration?.typeParameterList?.startOffsetInParent ?: -1
}
