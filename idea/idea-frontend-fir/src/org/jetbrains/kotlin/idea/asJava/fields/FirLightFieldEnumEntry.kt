/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.asJava.elements.FirLightIdentifier
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.idea.asJava.*
import org.jetbrains.kotlin.idea.frontend.api.isValid
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.psi.KtEnumEntry

internal class FirLightFieldForEnumEntry(
    private val enumEntrySymbol: KtEnumEntrySymbol,
    containingClass: FirLightClassForSymbol,
    override val lightMemberOrigin: LightMemberOrigin?
) : FirLightField(containingClass, lightMemberOrigin), PsiEnumConstant {

    private val _modifierList by lazyPub {
        FirLightClassModifierList(
            containingDeclaration = this@FirLightFieldForEnumEntry,
            modifiers = setOf(PsiModifier.STATIC, PsiModifier.FINAL, PsiModifier.PUBLIC),
            annotations = emptyList()
        )
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override val kotlinOrigin: KtEnumEntry? = enumEntrySymbol.psi as? KtEnumEntry

    override fun isDeprecated(): Boolean = false

    //TODO Make with KtSymbols
    private val hasBody: Boolean get() = kotlinOrigin?.let { it.body != null } ?: true

    private val _initializingClass: PsiEnumConstantInitializer? by lazyPub {
        hasBody.ifTrue {
            FirLightClassForEnumEntry(
                enumEntrySymbol = enumEntrySymbol,
                enumConstant = this@FirLightFieldForEnumEntry,
                enumClass = containingClass,
                manager = manager
            )
        }
    }

    override fun getInitializingClass(): PsiEnumConstantInitializer? = _initializingClass
    override fun getOrCreateInitializingClass(): PsiEnumConstantInitializer =
        _initializingClass ?: cannotModify()

    override fun getArgumentList(): PsiExpressionList? = null
    override fun resolveMethod(): PsiMethod? = null
    override fun resolveConstructor(): PsiMethod? = null

    override fun resolveMethodGenerics(): JavaResolveResult = JavaResolveResult.EMPTY

    override fun hasInitializer() = true
    override fun computeConstantValue(visitedVars: MutableSet<PsiVariable>?) = this

    override fun getName(): String = enumEntrySymbol.name.asString()

    private val _type: PsiType by lazyPub {
        enumEntrySymbol.annotatedType.asPsiType(enumEntrySymbol, this@FirLightFieldForEnumEntry, FirResolvePhase.TYPES)
    }

    override fun getType(): PsiType = _type
    override fun getInitializer(): PsiExpression? = null

    override fun hashCode(): Int = enumEntrySymbol.hashCode()

    private val _identifier: PsiIdentifier by lazyPub {
        FirLightIdentifier(this, enumEntrySymbol)
    }

    override fun getNameIdentifier(): PsiIdentifier = _identifier

    override fun isValid(): Boolean = super.isValid() && enumEntrySymbol.isValid()


    override fun equals(other: Any?): Boolean =
        other is FirLightFieldForEnumEntry &&
                enumEntrySymbol == other.enumEntrySymbol
}