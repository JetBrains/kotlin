/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
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
        useSiteModule: KaModule,
        classSymbol: KaNamedClassSymbol,
        manager: PsiManager,
    ) : super(
        useSiteModule = useSiteModule,
        classSymbol = classSymbol,
        manager = manager,
    ) {
        val classKind = classSymbol.classKind
        require(classKind == KaClassKind.INTERFACE || classKind == KaClassKind.ANNOTATION_CLASS)
    }

    constructor(
        classOrObject: KtClassOrObject,
        useSiteModule: KaModule,
    ) : this(
        classOrObjectDeclaration = classOrObject,
        classSymbolPointer = classOrObject.createSymbolPointer(useSiteModule),
        useSiteModule = useSiteModule,
        manager = classOrObject.manager,
    ) {
        require(classOrObject is KtClass && (classOrObject.isInterface() || classOrObject.isAnnotation()))
    }

    protected constructor(
        classOrObjectDeclaration: KtClassOrObject?,
        classSymbolPointer: KaSymbolPointer<KaNamedClassSymbol>,
        useSiteModule: KaModule,
        manager: PsiManager,
    ) : super(
        classOrObjectDeclaration = classOrObjectDeclaration,
        classSymbolPointer = classSymbolPointer,
        useSiteModule = useSiteModule,
        manager = manager,
    )

    protected open fun computeModifierList(): PsiModifierList? = SymbolLightClassModifierList(
        containingDeclaration = this,
        modifiersBox = GranularModifiersBox(
            initialValue = GranularModifiersBox.MODALITY_MODIFIERS_MAP.with(PsiModifier.ABSTRACT),
            computer = ::computeModifiers
        ),
        annotationsBox = GranularAnnotationsBox(
            annotationsProvider = SymbolAnnotationsProvider(useSiteModule, symbolPointer),
            additionalAnnotationsProvider = AbstractClassAdditionalAnnotationsProvider,
        ),
    )

    final override fun getModifierList(): PsiModifierList? = cachedValue {
        computeModifierList()
    }

    override val ownConstructors: Array<PsiMethod> get() = PsiMethod.EMPTY_ARRAY

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
