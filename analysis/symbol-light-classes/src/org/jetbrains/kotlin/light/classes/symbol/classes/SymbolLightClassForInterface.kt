/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReferenceList
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

internal open class SymbolLightClassForInterface : SymbolLightClassForInterfaceOrAnnotationClass {
    constructor(
        ktAnalysisSession: KaSession,
        ktModule: KaModule,
        classSymbol: KaNamedClassSymbol,
        manager: PsiManager
    ) : super(
        ktAnalysisSession = ktAnalysisSession,
        ktModule = ktModule,
        classSymbol = classSymbol,
        manager = manager,
    ) {
        require(classSymbol.classKind == KaClassKind.INTERFACE)
    }

    constructor(classOrObject: KtClassOrObject, ktModule: KaModule) : super(classOrObject, ktModule) {
        require(classOrObject is KtClass && classOrObject.isInterface())
    }

    protected constructor(
        classOrObjectDeclaration: KtClassOrObject?,
        classSymbolPointer: KaSymbolPointer<KaNamedClassSymbol>,
        ktModule: KaModule,
        manager: PsiManager,
    ) : super(
        classOrObjectDeclaration = classOrObjectDeclaration,
        classSymbolPointer = classSymbolPointer,
        ktModule = ktModule,
        manager = manager,
    )

    override fun getOwnMethods(): List<PsiMethod> = cachedValue {
        withClassSymbol { classSymbol ->
            val result = mutableListOf<PsiMethod>()

            val visibleDeclarations = classSymbol.declaredMemberScope.callables.filter { acceptCallableSymbol(this, it) }

            createMethods(this@SymbolLightClassForInterface, visibleDeclarations, result)
            addMethodsFromCompanionIfNeeded(result, classSymbol)

            result
        }
    }

    protected open fun acceptCallableSymbol(session: KaSession, symbol: KaCallableSymbol): Boolean = with(session) {
        !(symbol is KaNamedFunctionSymbol && symbol.visibility == KaSymbolVisibility.PRIVATE || hasTypeForValueClassInSignature(symbol))
    }

    override fun copy(): SymbolLightClassForInterface =
        SymbolLightClassForInterface(classOrObjectDeclaration, classSymbolPointer, ktModule, manager)

    private val _extendsList: PsiReferenceList by lazyPub {
        withClassSymbol { classSymbol ->
            createInheritanceList(this@SymbolLightClassForInterface, forExtendsList = true, classSymbol.superTypes)
        }
    }

    override fun getExtendsList(): PsiReferenceList? = _extendsList
    override fun classKind(): KaClassKind = KaClassKind.INTERFACE
}
