/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.scopes.declaredMemberScope
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

internal open class SymbolLightClassForAnnotationClass : SymbolLightClassForInterfaceOrAnnotationClass {
    constructor(
        useSiteModule: KaModule,
        classSymbol: KaNamedClassSymbol,
    ) : super(
        useSiteModule = useSiteModule,
        classSymbol = classSymbol,
    ) {
        require(classSymbol.classKind == KaClassKind.ANNOTATION_CLASS)
    }

    constructor(classOrObject: KtClassOrObject, useSiteModule: KaModule) : super(classOrObject, useSiteModule) {
        require(classOrObject is KtClass && classOrObject.isAnnotation())
    }

    constructor(
        classOrObjectDeclaration: KtClassOrObject?,
        classSymbolPointer: KaSymbolPointer<KaNamedClassSymbol>,
        useSiteModule: KaModule,
    ) : super(
        classOrObjectDeclaration = classOrObjectDeclaration,
        classSymbolPointer = classSymbolPointer,
        useSiteModule = useSiteModule,
    )

    override fun classKind(): KaClassKind = KaClassKind.ANNOTATION_CLASS

    protected open fun computeOwnMethods(): List<PsiMethod> = withClassSymbol { classSymbol ->
        val result = mutableListOf<PsiMethod>()
        val visibleDeclarations = classSymbol.declaredMemberScope.callables
            .filterNot { it is KaNamedFunctionSymbol && it.visibility == KaSymbolVisibility.PRIVATE }
            .filterNot { it is KaConstructorSymbol }

        createMethods(this@SymbolLightClassForAnnotationClass, visibleDeclarations, result)
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
        classSymbolPointer = symbolPointer,
        useSiteModule = useSiteModule,
    )
}
