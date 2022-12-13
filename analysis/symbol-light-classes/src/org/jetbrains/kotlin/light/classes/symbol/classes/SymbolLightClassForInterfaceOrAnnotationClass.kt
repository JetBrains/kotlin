/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiManager
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiReferenceList
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.symbolPointerOfType
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.light.classes.symbol.NullabilityType
import org.jetbrains.kotlin.light.classes.symbol.annotations.computeAnnotations
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.LazyModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.with
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

internal abstract class SymbolLightClassForInterfaceOrAnnotationClass : SymbolLightClassForNamedClassLike {
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
        val classKind = classOrObjectSymbol.classKind
        require(classKind == KtClassKind.INTERFACE || classKind == KtClassKind.ANNOTATION_CLASS)
    }

    constructor(
        classOrObject: KtClassOrObject,
        ktModule: KtModule,
    ) : this(
        classOrObjectDeclaration = classOrObject,
        classOrObjectSymbolPointer = classOrObject.symbolPointerOfType(),
        ktModule = ktModule,
        manager = classOrObject.manager,
    ) {
        require(classOrObject is KtClass && (classOrObject.isInterface() || classOrObject.isAnnotation()))
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

    protected open fun computeModifierList(): PsiModifierList? = SymbolLightClassModifierList(
        containingDeclaration = this,
        initialValue = LazyModifiersBox.MODALITY_MODIFIERS_MAP.with(PsiModifier.ABSTRACT),
        lazyModifiersComputer = ::computeModifiers
    ) { modifierList ->
        withClassOrObjectSymbol { classOrObjectSymbol ->
            classOrObjectSymbol.computeAnnotations(
                modifierList = modifierList,
                nullability = NullabilityType.Unknown,
                annotationUseSiteTarget = null,
            )
        }
    }

    private val _modifierList: PsiModifierList? by lazyPub {
        computeModifierList()
    }

    override fun isInterface(): Boolean = true
    override fun isEnum(): Boolean = false

    final override fun getModifierList(): PsiModifierList? = _modifierList

    private val _ownFields: List<KtLightField> by lazyPub {
        withClassOrObjectSymbol { classOrObjectSymbol ->
            buildList {
                addCompanionObjectFieldIfNeeded(this, classOrObjectSymbol)
                addFieldsFromCompanionIfNeeded(this, classOrObjectSymbol)
            }
        }
    }

    override fun getOwnFields(): List<KtLightField> = _ownFields

    override fun getImplementsList(): PsiReferenceList? = null
}
