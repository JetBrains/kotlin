/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.PsiField
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiReferenceList
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.symbolPointerOfType
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.annotations.AbstractClassAdditionalAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightField
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.GranularModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.with
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

internal abstract class SymbolLightClassForInterfaceOrAnnotationClass : SymbolLightClassForNamedClassLike {
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
        val classKind = classSymbol.classKind
        require(classKind == KaClassKind.INTERFACE || classKind == KaClassKind.ANNOTATION_CLASS)
    }

    constructor(
        classOrObject: KtClassOrObject,
        ktModule: KaModule,
    ) : this(
        classOrObjectDeclaration = classOrObject,
        classSymbolPointer = classOrObject.symbolPointerOfType(),
        ktModule = ktModule,
        manager = classOrObject.manager,
    ) {
        require(classOrObject is KtClass && (classOrObject.isInterface() || classOrObject.isAnnotation()))
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

    protected open fun computeModifierList(): PsiModifierList? = SymbolLightClassModifierList(
        containingDeclaration = this,
        modifiersBox = GranularModifiersBox(
            initialValue = GranularModifiersBox.MODALITY_MODIFIERS_MAP.with(PsiModifier.ABSTRACT),
            computer = ::computeModifiers
        ),
        annotationsBox = GranularAnnotationsBox(
            annotationsProvider = SymbolAnnotationsProvider(ktModule, classSymbolPointer),
            additionalAnnotationsProvider = AbstractClassAdditionalAnnotationsProvider,
        ),
    )

    private val _modifierList: PsiModifierList? by lazyPub {
        computeModifierList()
    }

    final override fun getModifierList(): PsiModifierList? = _modifierList

    override fun getOwnFields(): List<PsiField> = cachedValue {
        withClassSymbol { classSymbol ->
            buildList {
                addCompanionObjectFieldIfNeeded(this, classSymbol)
                addFieldsFromCompanionIfNeeded(this, classSymbol, SymbolLightField.FieldNameGenerator())
            }
        }
    }

    override fun getImplementsList(): PsiReferenceList? = null
}
