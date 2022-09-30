/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReferenceList
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.isPrivateOrPrivateToThis
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtClassOrObject

internal open class SymbolLightInterfaceClass(
    classOrObject: KtClassOrObject,
    ktModule: KtModule,
) : SymbolLightInterfaceOrAnnotationClass(classOrObject, ktModule) {
    init {
        require(isInterface)
    }

    private val _ownMethods: List<KtLightMethod> by lazyPub {
        withClassOrObjectSymbol { classOrObjectSymbol ->
            val result = mutableListOf<KtLightMethod>()

            val visibleDeclarations = classOrObjectSymbol.getDeclaredMemberScope().getCallableSymbols()
                .filterNot { it is KtFunctionSymbol && it.visibility.isPrivateOrPrivateToThis() }

            createMethods(visibleDeclarations, result)
            addMethodsFromCompanionIfNeeded(result)

            result
        }
    }

    override fun getOwnMethods(): List<PsiMethod> = _ownMethods

    override fun copy(): SymbolLightClassForClassOrObject = SymbolLightInterfaceClass(classOrObject, ktModule)

    private val _extendsList: PsiReferenceList by lazyPub {
        withClassOrObjectSymbol { classOrObjectSymbol ->
            createInheritanceList(forExtendsList = true, classOrObjectSymbol.superTypes)
        }
    }

    override fun getExtendsList(): PsiReferenceList? = _extendsList
}
