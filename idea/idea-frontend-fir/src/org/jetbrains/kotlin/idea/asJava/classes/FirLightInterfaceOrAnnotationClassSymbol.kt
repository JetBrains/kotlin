/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava.classes

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.idea.asJava.*
import org.jetbrains.kotlin.idea.frontend.api.isValid
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassKind
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolKind

internal abstract class FirLightInterfaceOrAnnotationClassSymbol(
    private val classOrObjectSymbol: KtNamedClassOrObjectSymbol,
    manager: PsiManager
) : FirLightClassForClassOrObjectSymbol(classOrObjectSymbol, manager) {

    init {
        require(
            classOrObjectSymbol.classKind == KtClassKind.OBJECT ||
                    classOrObjectSymbol.classKind == KtClassKind.INTERFACE ||
                    classOrObjectSymbol.classKind == KtClassKind.ANNOTATION_CLASS
        )
    }

    private val _modifierList: PsiModifierList? by lazyPub {

        val isTopLevel: Boolean = classOrObjectSymbol.symbolKind == KtSymbolKind.TOP_LEVEL

        val modifiers = mutableSetOf(classOrObjectSymbol.toPsiVisibilityForClass(isTopLevel), PsiModifier.ABSTRACT)

        val annotations = classOrObjectSymbol.computeAnnotations(
            parent = this@FirLightInterfaceOrAnnotationClassSymbol,
            nullability = NullabilityType.Unknown,
            annotationUseSiteTarget = null,
        )

        FirLightClassModifierList(this@FirLightInterfaceOrAnnotationClassSymbol, modifiers, annotations)
    }

    override fun getModifierList(): PsiModifierList? = _modifierList

    override fun getImplementsList(): PsiReferenceList? = null

    override fun getOwnInnerClasses(): List<PsiClass> = emptyList()

    override fun isInterface(): Boolean = true

    override fun isEnum(): Boolean = false

    override fun isValid(): Boolean = super.isValid() && classOrObjectSymbol.isValid()
}