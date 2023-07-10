/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightElement
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithTypeParameters
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.basicIsEquivalentTo
import org.jetbrains.kotlin.light.classes.symbol.compareSymbolPointers
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.light.classes.symbol.toArrayIfNotEmptyOrDefault
import org.jetbrains.kotlin.light.classes.symbol.withSymbol
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner

internal class SymbolLightTypeParameterList(
    internal val owner: PsiTypeParameterListOwner,
    private val symbolWithTypeParameterPointer: KtSymbolPointer<KtSymbolWithTypeParameters>,
    internal val ktModule: KtModule,
    private val ktDeclaration: KtTypeParameterListOwner?,
) : LightElement(owner.manager, KotlinLanguage.INSTANCE), PsiTypeParameterList {
    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitTypeParameterList(this)
        } else {
            visitor.visitElement(this)
        }
    }

    override fun processDeclarations(
        processor: PsiScopeProcessor,
        state: ResolveState,
        lastParent: PsiElement?,
        place: PsiElement
    ): Boolean = typeParameters.all { processor.execute(it, state) }

    private val _typeParameters: Collection<PsiTypeParameter> by lazyPub {
        symbolWithTypeParameterPointer.withSymbol(ktModule) {
            val parentInterface =
                (owner as? SymbolLightMethodBase)?.containingClass?.interfaceIfDefaultImpls

            val fromInterface = parentInterface?.typeParameters?.mapNotNull {
                (it as? SymbolLightTypeParameter)?.copyTo(this@SymbolLightTypeParameterList)
            }.orEmpty()

            fromInterface + it.typeParameters.mapIndexed { index, parameter ->
                SymbolLightTypeParameter(
                    ktAnalysisSession = this,
                    parent = this@SymbolLightTypeParameterList,
                    index = fromInterface.size + index,
                    typeParameterSymbol = parameter,
                )
            }
        }
    }

    override fun getTypeParameters(): Array<PsiTypeParameter> = _typeParameters.toArrayIfNotEmptyOrDefault(PsiTypeParameter.EMPTY_ARRAY)

    override fun getTypeParameterIndex(typeParameter: PsiTypeParameter?): Int = _typeParameters.indexOf(typeParameter)

    override fun toString(): String = "SymbolLightTypeParameterList"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightTypeParameterList || other.ktModule != ktModule) return false
        if (ktDeclaration != null || other.ktDeclaration != null) {
            return other.ktDeclaration == ktDeclaration
        }

        return other.owner == owner && compareSymbolPointers(symbolWithTypeParameterPointer, other.symbolWithTypeParameterPointer)
    }

    override fun hashCode(): Int = ktDeclaration.hashCode() + 1

    override fun isEquivalentTo(another: PsiElement?): Boolean = basicIsEquivalentTo(this, another)

    override fun getParent(): PsiElement = owner
    override fun getContainingFile(): PsiFile = parent.containingFile
    override fun getText(): String? = ktDeclaration?.typeParameterList?.text
    override fun getTextOffset(): Int = ktDeclaration?.typeParameterList?.textOffset ?: -1
    override fun getStartOffsetInParent(): Int = ktDeclaration?.typeParameterList?.startOffsetInParent ?: -1
}
