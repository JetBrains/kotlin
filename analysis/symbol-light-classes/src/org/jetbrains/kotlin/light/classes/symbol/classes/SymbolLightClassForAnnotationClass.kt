/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.isPrivateOrPrivateToThis
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

internal open class SymbolLightClassForAnnotationClass : SymbolLightClassForInterfaceOrAnnotationClass {
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
        require(classOrObjectSymbol.classKind == KtClassKind.ANNOTATION_CLASS)
    }

    constructor(classOrObject: KtClassOrObject, ktModule: KtModule) : super(classOrObject, ktModule) {
        require(classOrObject is KtClass && classOrObject.isAnnotation())
    }

    constructor(
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

    override fun isAnnotationType(): Boolean = true

    protected open fun computeOwnMethods(): List<PsiMethod> = withClassOrObjectSymbol { classOrObjectSymbol ->
        val result = mutableListOf<KtLightMethod>()
        val visibleDeclarations = classOrObjectSymbol.getDeclaredMemberScope().getCallableSymbols()
            .filterNot { it is KtFunctionSymbol && it.visibility.isPrivateOrPrivateToThis() }
            .filterNot { it is KtConstructorSymbol }

        createMethods(visibleDeclarations, result)
        result
    }

    private val _ownMethods: List<PsiMethod> by lazyPub {
        computeOwnMethods()
    }

    final override fun getOwnMethods(): List<PsiMethod> = _ownMethods
    override fun getExtendsList(): PsiReferenceList? = null

    final override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
        val qualifiedName = baseClass.qualifiedName
        return qualifiedName == CommonClassNames.JAVA_LANG_ANNOTATION_ANNOTATION || qualifiedName == CommonClassNames.JAVA_LANG_OBJECT
    }

    override fun copy(): SymbolLightClassForAnnotationClass = SymbolLightClassForAnnotationClass(
        classOrObjectDeclaration = classOrObjectDeclaration,
        classOrObjectSymbolPointer = classOrObjectSymbolPointer,
        ktModule = ktModule,
        manager = manager,
    )
}
