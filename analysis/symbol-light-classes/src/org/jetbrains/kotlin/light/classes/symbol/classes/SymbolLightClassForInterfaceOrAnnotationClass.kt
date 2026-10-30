/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.scopes.combinedDeclaredMemberScope
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.light.classes.symbol.annotations.AbstractClassAdditionalAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.fields.SymbolLightField
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.GranularModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.with
import org.jetbrains.kotlin.light.classes.symbol.utils.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.utils.isConstOrJvmField
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

internal abstract class SymbolLightClassForInterfaceOrAnnotationClass : SymbolLightClassForNamedClassLike {
    constructor(
        useSiteModule: KaModule,
        classSymbol: KaNamedClassSymbol,
    ) : super(
        useSiteModule = useSiteModule,
        classSymbol = classSymbol,
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
    ) {
        require(classOrObject is KtClass && (classOrObject.isInterface() || classOrObject.isAnnotation()))
    }

    protected constructor(
        classOrObjectDeclaration: KtClassOrObject?,
        classSymbolPointer: KaSymbolPointer<KaNamedClassSymbol>,
        useSiteModule: KaModule,
    ) : super(
        classOrObjectDeclaration = classOrObjectDeclaration,
        classSymbolPointer = classSymbolPointer,
        useSiteModule = useSiteModule,
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
                val nameGenerator = SymbolLightField.FieldNameGenerator()
                addFieldsFromCompanionIfNeeded(this, classSymbol, nameGenerator)
                addFieldsFromCompanionBlock(this, classSymbol, nameGenerator)
            }
        }
    }

    /**
     * Adds the `const` and `@JvmField` properties of the companion block, which are public static fields of the interface.
     * The JVM backend moves the backing fields of other companion block properties to a nested class, as an interface can only
     * have public fields, see `org.jetbrains.kotlin.backend.jvm.lower.JvmInterfacePrivateFieldsLowering`.
     */
    context(_: KaSession)
    private fun addFieldsFromCompanionBlock(
        result: MutableList<PsiField>,
        classSymbol: KaNamedClassSymbol,
        nameGenerator: SymbolLightField.FieldNameGenerator,
    ) {
        classSymbol.combinedDeclaredMemberScope
            .callables
            .filterIsInstance<KaPropertySymbol>()
            .filter { it.isCompanion && it.isConstOrJvmField }
            .forEach {
                createAndAddField(
                    lightClass = this@SymbolLightClassForInterfaceOrAnnotationClass,
                    declaration = it,
                    nameGenerator = nameGenerator,
                    isStatic = true,
                    result = result,
                )
            }
    }

    override fun getImplementsList(): PsiReferenceList? = null
}
