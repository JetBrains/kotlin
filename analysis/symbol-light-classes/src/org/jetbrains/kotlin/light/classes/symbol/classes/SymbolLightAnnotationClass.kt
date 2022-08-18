/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReferenceList
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.isValid
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.isPrivateOrPrivateToThis
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod

context(KtAnalysisSession)
internal class SymbolLightAnnotationClass(
    private val classOrObjectSymbol: KtNamedClassOrObjectSymbol,
    manager: PsiManager
) : SymbolLightInterfaceOrAnnotationClass(classOrObjectSymbol, manager) {

    init {
        require(classOrObjectSymbol.classKind == KtClassKind.ANNOTATION_CLASS)
    }

    override fun isAnnotationType(): Boolean = true

    private val _ownFields: List<KtLightField> by lazyPub {
        mutableListOf<KtLightField>().also {
            addCompanionObjectFieldIfNeeded(it)
        }
    }

    override fun getOwnFields(): List<KtLightField> = _ownFields

    private val _ownMethods: List<KtLightMethod> by lazyPub {
        val result = mutableListOf<KtLightMethod>()
        val visibleDeclarations = classOrObjectSymbol.getDeclaredMemberScope().getCallableSymbols()
            .filterNot { it is KtFunctionSymbol && it.visibility.isPrivateOrPrivateToThis() }
            .filterNot { it is KtConstructorSymbol }

        createMethods(visibleDeclarations, result)
        result
    }

    override fun getOwnMethods(): List<PsiMethod> = _ownMethods

    override fun getExtendsList(): PsiReferenceList? = null

    override fun equals(other: Any?): Boolean =
        other is SymbolLightAnnotationClass && classOrObjectSymbol == other.classOrObjectSymbol

    override fun hashCode(): Int = classOrObjectSymbol.hashCode()


    override fun isValid(): Boolean = super.isValid() && classOrObjectSymbol.isValid()

    override fun copy(): SymbolLightClassForClassOrObject = SymbolLightAnnotationClass(classOrObjectSymbol, manager)
}
