/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.fields

import com.intellij.psi.*
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.isValid
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.SymbolLightIdentifier
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolLightSimpleAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasDeprecatedAnnotation
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.light.classes.symbol.nonExistentType
import org.jetbrains.kotlin.light.classes.symbol.toPsiVisibilityForMember
import org.jetbrains.kotlin.psi.KtDeclaration

context(KtAnalysisSession)
internal class SymbolLightFieldForObject(
    private val objectSymbol: KtNamedClassOrObjectSymbol,
    containingClass: KtLightClass,
    private val name: String,
    lightMemberOrigin: LightMemberOrigin?,
) : SymbolLightField(containingClass, lightMemberOrigin) {

    override val kotlinOrigin: KtDeclaration? = objectSymbol.psi as? KtDeclaration

    override fun getName(): String = name

    private val _modifierList: PsiModifierList by lazyPub {
        val modifiers = setOf(objectSymbol.toPsiVisibilityForMember(), PsiModifier.STATIC, PsiModifier.FINAL)
        val notNullAnnotation = SymbolLightSimpleAnnotation(NotNull::class.java.name, this)
        SymbolLightMemberModifierList(this, modifiers, listOf(notNullAnnotation))
    }

    private val _isDeprecated: Boolean by lazyPub {
        objectSymbol.hasDeprecatedAnnotation()
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    override fun getModifierList(): PsiModifierList? = _modifierList

    private val _type: PsiType by lazyPub {
        objectSymbol.buildSelfClassType().asPsiType(this@SymbolLightFieldForObject)
            ?: nonExistentType()
    }

    private val _identifier: PsiIdentifier by lazyPub {
        SymbolLightIdentifier(this, objectSymbol)
    }

    override fun getNameIdentifier(): PsiIdentifier = _identifier


    override fun getType(): PsiType = _type

    override fun getInitializer(): PsiExpression? = null //TODO

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is SymbolLightFieldForObject &&
                        kotlinOrigin == other.kotlinOrigin &&
                        objectSymbol == other.objectSymbol)

    override fun hashCode(): Int = kotlinOrigin.hashCode()

    override fun isValid(): Boolean = super.isValid() && objectSymbol.isValid()
}
