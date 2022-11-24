/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.fields

import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiType
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.analyzeForLightClasses
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolLightSimpleAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasDeprecatedAnnotation
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForClassOrObject
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.nonExistentType
import org.jetbrains.kotlin.light.classes.symbol.toPsiVisibilityForMember
import org.jetbrains.kotlin.psi.KtObjectDeclaration

internal class SymbolLightFieldForObject(
    private val objectDeclaration: KtObjectDeclaration,
    containingClass: SymbolLightClassForClassOrObject,
    private val name: String,
    lightMemberOrigin: LightMemberOrigin?,
) : SymbolLightField(containingClass, lightMemberOrigin) {
    private fun <T> withObjectDeclarationSymbol(action: KtAnalysisSession.(KtNamedClassOrObjectSymbol) -> T): T =
        analyzeForLightClasses(ktModule) {
            action(requireNotNull(objectDeclaration.getNamedClassOrObjectSymbol()))
        }

    override val kotlinOrigin: KtObjectDeclaration = objectDeclaration

    override fun getName(): String = name

    private val _modifierList: PsiModifierList by lazyPub {
        val modifiers = withObjectDeclarationSymbol { objectSymbol ->
            setOf(objectSymbol.toPsiVisibilityForMember(), PsiModifier.STATIC, PsiModifier.FINAL)
        }

        val notNullAnnotation = SymbolLightSimpleAnnotation(NotNull::class.java.name, this)
        SymbolLightMemberModifierList(this, modifiers, listOf(notNullAnnotation))
    }

    private val _isDeprecated: Boolean by lazyPub {
        withObjectDeclarationSymbol { objectSymbol ->
            objectSymbol.hasDeprecatedAnnotation()
        }
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _type: PsiType by lazyPub {
        withObjectDeclarationSymbol { objectSymbol ->
            objectSymbol.buildSelfClassType().asPsiType(this@SymbolLightFieldForObject)
        } ?: nonExistentType()
    }

    override fun getType(): PsiType = _type

    override fun getInitializer(): PsiExpression? = null //TODO

    override fun equals(other: Any?): Boolean =
        this === other || other is SymbolLightFieldForObject && objectDeclaration == other.objectDeclaration

    override fun hashCode(): Int = objectDeclaration.hashCode()

    override fun isValid(): Boolean = objectDeclaration.isValid
}
