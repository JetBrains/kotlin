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
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolLightSimpleAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasDeprecatedAnnotation
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForClassLike
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.psi.KtObjectDeclaration

internal class SymbolLightFieldForObject private constructor(
    containingClass: SymbolLightClassForClassLike<*>,
    private val name: String,
    lightMemberOrigin: LightMemberOrigin?,
    private val objectSymbolPointer: KtSymbolPointer<KtNamedClassOrObjectSymbol>,
    override val kotlinOrigin: KtObjectDeclaration?,
) : SymbolLightField(containingClass, lightMemberOrigin) {
    internal constructor(
        ktAnalysisSession: KtAnalysisSession,
        objectSymbol: KtNamedClassOrObjectSymbol,
        name: String,
        lightMemberOrigin: LightMemberOrigin?,
        containingClass: SymbolLightClassForClassLike<*>,
    ) : this(
        containingClass = containingClass,
        name = name,
        lightMemberOrigin = lightMemberOrigin,
        kotlinOrigin = objectSymbol.sourcePsiSafe(),
        objectSymbolPointer = with(ktAnalysisSession) { objectSymbol.createPointer() },
    )

    private fun <T> withObjectDeclarationSymbol(action: KtAnalysisSession.(KtNamedClassOrObjectSymbol) -> T): T =
        objectSymbolPointer.withSymbol(ktModule, action)

    override fun getName(): String = name

    private val _modifierList: PsiModifierList by lazyPub {
        val lazyModifiers = lazyPub {
            withObjectDeclarationSymbol { objectSymbol ->
                setOf(objectSymbol.toPsiVisibilityForMember(), PsiModifier.STATIC, PsiModifier.FINAL)
            }
        }

        val lazyAnnotations = lazyPub {
            val notNullAnnotation = SymbolLightSimpleAnnotation(NotNull::class.java.name, this)
            listOf(notNullAnnotation)
        }

        SymbolLightMemberModifierList(this, lazyModifiers, lazyAnnotations)
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightFieldForObject || other.ktModule != ktModule) return false
        if (kotlinOrigin != null) {
            return other.kotlinOrigin == kotlinOrigin
        }

        return other.kotlinOrigin == null &&
                other.containingClass == containingClass &&
                compareSymbolPointers(ktModule, other.objectSymbolPointer, objectSymbolPointer)
    }

    override fun hashCode(): Int = kotlinOrigin.hashCode()

    override fun isValid(): Boolean = kotlinOrigin?.isValid ?: objectSymbolPointer.isValid(ktModule)
}
