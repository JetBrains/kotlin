/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReferenceList
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.isPrivateOrPrivateToThis
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

internal open class SymbolLightClassForInterface : SymbolLightClassForInterfaceOrAnnotationClass {
    constructor(
        ktAnalysisSession: KaSession,
        ktModule: KtModule,
        classOrObjectSymbol: KaNamedClassOrObjectSymbol,
        manager: PsiManager
    ) : super(
        ktAnalysisSession = ktAnalysisSession,
        ktModule = ktModule,
        classOrObjectSymbol = classOrObjectSymbol,
        manager = manager,
    ) {
        require(classOrObjectSymbol.classKind == KaClassKind.INTERFACE)
    }

    constructor(classOrObject: KtClassOrObject, ktModule: KtModule) : super(classOrObject, ktModule) {
        require(classOrObject is KtClass && classOrObject.isInterface())
    }

    protected constructor(
        classOrObjectDeclaration: KtClassOrObject?,
        classOrObjectSymbolPointer: KaSymbolPointer<KaNamedClassOrObjectSymbol>,
        ktModule: KtModule,
        manager: PsiManager,
    ) : super(
        classOrObjectDeclaration = classOrObjectDeclaration,
        classOrObjectSymbolPointer = classOrObjectSymbolPointer,
        ktModule = ktModule,
        manager = manager,
    )

    override fun getOwnMethods(): List<PsiMethod> = cachedValue {
        withClassOrObjectSymbol { classOrObjectSymbol ->
            val result = mutableListOf<KtLightMethod>()

            val visibleDeclarations = classOrObjectSymbol.declaredMemberScope.callables.filter { acceptCallableSymbol(it) }

            createMethods(visibleDeclarations, result)
            addMethodsFromCompanionIfNeeded(result, classOrObjectSymbol)

            result
        }
    }

    context(KaSession)
    protected open fun acceptCallableSymbol(symbol: KaCallableSymbol): Boolean =
        !(symbol is KaFunctionSymbol && symbol.visibility.isPrivateOrPrivateToThis() || symbol.hasTypeForValueClassInSignature())

    override fun copy(): SymbolLightClassForInterface =
        SymbolLightClassForInterface(classOrObjectDeclaration, classOrObjectSymbolPointer, ktModule, manager)

    private val _extendsList: PsiReferenceList by lazyPub {
        withClassOrObjectSymbol { classOrObjectSymbol ->
            createInheritanceList(forExtendsList = true, classOrObjectSymbol.superTypes)
        }
    }

    override fun getExtendsList(): PsiReferenceList? = _extendsList
    override fun classKind(): KaClassKind = KaClassKind.INTERFACE
}
