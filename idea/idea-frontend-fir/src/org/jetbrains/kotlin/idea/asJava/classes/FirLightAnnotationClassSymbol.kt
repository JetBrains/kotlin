/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava.classes

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReferenceList
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.idea.asJava.FirLightClassForClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.analyzeWithSymbolAsContext
import org.jetbrains.kotlin.idea.frontend.api.isValid
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.isPrivateOrPrivateToThis

internal class FirLightAnnotationClassSymbol(
    private val classOrObjectSymbol: KtNamedClassOrObjectSymbol,
    manager: PsiManager
) : FirLightInterfaceOrAnnotationClassSymbol(classOrObjectSymbol, manager) {

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

        analyzeWithSymbolAsContext(classOrObjectSymbol) {
            val visibleDeclarations = classOrObjectSymbol.getDeclaredMemberScope().getCallableSymbols()
                .filterNot { it is KtFunctionSymbol && it.visibility.isPrivateOrPrivateToThis() }
                .filterNot { it is KtConstructorSymbol }

            createMethods(visibleDeclarations, result)
        }

        result
    }

    override fun getOwnMethods(): List<PsiMethod> = _ownMethods

    override fun getExtendsList(): PsiReferenceList? = null

    override fun equals(other: Any?): Boolean =
        other is FirLightAnnotationClassSymbol && classOrObjectSymbol == other.classOrObjectSymbol

    override fun hashCode(): Int = classOrObjectSymbol.hashCode()


    override fun isValid(): Boolean = super.isValid() && classOrObjectSymbol.isValid()

    override fun copy(): FirLightClassForClassOrObjectSymbol = FirLightAnnotationClassSymbol(classOrObjectSymbol, manager)
}