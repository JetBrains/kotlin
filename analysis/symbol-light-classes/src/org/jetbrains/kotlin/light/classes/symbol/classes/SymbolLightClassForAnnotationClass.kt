/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.isPrivateOrPrivateToThis
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

internal open class SymbolLightClassForAnnotationClass : SymbolLightClassForInterfaceOrAnnotationClass {
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
        require(classOrObjectSymbol.classKind == KaClassKind.ANNOTATION_CLASS)
    }

    constructor(classOrObject: KtClassOrObject, ktModule: KtModule) : super(classOrObject, ktModule) {
        require(classOrObject is KtClass && classOrObject.isAnnotation())
    }

    constructor(
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

    override fun classKind(): KaClassKind = KaClassKind.ANNOTATION_CLASS

    protected open fun computeOwnMethods(): List<PsiMethod> = withClassOrObjectSymbol { classOrObjectSymbol ->
        val result = mutableListOf<KtLightMethod>()
        val visibleDeclarations = classOrObjectSymbol.declaredMemberScope.callables
            .filterNot { it is KaFunctionSymbol && it.visibility.isPrivateOrPrivateToThis() }
            .filterNot { it is KaConstructorSymbol }

        createMethods(visibleDeclarations, result)
        result
    }

    final override fun getOwnMethods(): List<PsiMethod> = cachedValue {
        computeOwnMethods()
    }

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
