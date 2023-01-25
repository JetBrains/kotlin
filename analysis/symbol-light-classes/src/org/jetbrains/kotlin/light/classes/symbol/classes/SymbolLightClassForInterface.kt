/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReferenceList
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.isPrivateOrPrivateToThis
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

internal open class SymbolLightClassForInterface : SymbolLightClassForInterfaceOrAnnotationClass {
    constructor(
        ktAnalysisSession: KtAnalysisSession,
        ktModule: KtModule,
        classOrObjectSymbol: KtNamedClassOrObjectSymbol,
        manager: PsiManager
    ) : super(
        ktAnalysisSession = ktAnalysisSession,
        ktModule = ktModule,
        classOrObjectSymbol = classOrObjectSymbol,
        manager = manager,
    ) {
        require(classOrObjectSymbol.classKind == KtClassKind.INTERFACE)
    }

    constructor(classOrObject: KtClassOrObject, ktModule: KtModule) : super(classOrObject, ktModule) {
        require(classOrObject is KtClass && classOrObject.isInterface())
    }

    protected constructor(
        classOrObjectDeclaration: KtClassOrObject?,
        classOrObjectSymbolPointer: KtSymbolPointer<KtNamedClassOrObjectSymbol>,
        ktModule: KtModule,
        manager: PsiManager,
    ) : super(
        classOrObjectDeclaration = classOrObjectDeclaration,
        classOrObjectSymbolPointer = classOrObjectSymbolPointer,
        ktModule = ktModule,
        manager = manager,
    )

    private val _ownMethods: List<KtLightMethod> by lazyPub {
        withClassOrObjectSymbol { classOrObjectSymbol ->
            val result = mutableListOf<KtLightMethod>()

            val visibleDeclarations = classOrObjectSymbol.getDeclaredMemberScope().getCallableSymbols()
                .filterNot { it is KtFunctionSymbol && it.visibility.isPrivateOrPrivateToThis() }
                .filterNot { it.hasTypeForValueClassInSignature() }

            createMethods(visibleDeclarations, result)
            addMethodsFromCompanionIfNeeded(result, classOrObjectSymbol)

            result
        }
    }

    override fun getOwnMethods(): List<PsiMethod> = _ownMethods

    override fun copy(): SymbolLightClassForInterface =
        SymbolLightClassForInterface(classOrObjectDeclaration, classOrObjectSymbolPointer, ktModule, manager)

    private val _extendsList: PsiReferenceList by lazyPub {
        withClassOrObjectSymbol { classOrObjectSymbol ->
            createInheritanceList(forExtendsList = true, classOrObjectSymbol.superTypes)
        }
    }

    override fun getExtendsList(): PsiReferenceList? = _extendsList
    override fun classKind(): KtClassKind = KtClassKind.INTERFACE
}
