/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReferenceList
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.analysis.api.isValid
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.isPrivateOrPrivateToThis
import org.jetbrains.kotlin.light.classes.symbol.classes.createInheritanceList
import org.jetbrains.kotlin.light.classes.symbol.classes.createMethods

internal class FirLightInterfaceClassSymbol(
    private val classOrObjectSymbol: KtNamedClassOrObjectSymbol,
    manager: PsiManager
) : FirLightInterfaceOrAnnotationClassSymbol(classOrObjectSymbol, manager) {

    init {
        require(classOrObjectSymbol.classKind == KtClassKind.INTERFACE)
    }

    private val _ownFields: List<KtLightField> by lazyPub {
        mutableListOf<KtLightField>().also {
            addCompanionObjectFieldIfNeeded(it)
        }
    }

    override fun getOwnFields(): List<KtLightField> = _ownFields

    private val _ownMethods: List<KtLightMethod> by lazyPub {

        val result = mutableListOf<KtLightMethod>()

        analyzeWithSymbolAsContext(classOrObjectSymbol) {
            val visibleDeclarations = classOrObjectSymbol.getDeclaredMemberScope().getCallableSymbols()
                .filterNot { it is KtFunctionSymbol && it.visibility.isPrivateOrPrivateToThis() }

            createMethods(visibleDeclarations, result)
        }

        result
    }

    override fun getOwnMethods(): List<PsiMethod> = _ownMethods

    override fun equals(other: Any?): Boolean =
        other === this || (other is FirLightInterfaceClassSymbol && classOrObjectSymbol == other.classOrObjectSymbol)

    override fun hashCode(): Int = classOrObjectSymbol.hashCode()

    override fun isAnnotationType(): Boolean = false

    override fun copy(): FirLightClassForClassOrObjectSymbol =
        FirLightInterfaceClassSymbol(classOrObjectSymbol, manager)

    private val _extendsList: PsiReferenceList by lazyPub {
        createInheritanceList(forExtendsList = false, classOrObjectSymbol.superTypes)
    }

    override fun getExtendsList(): PsiReferenceList? = _extendsList

    override fun isValid(): Boolean = super.isValid() && classOrObjectSymbol.isValid()
}
