/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.fields

import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiType
import kotlinx.collections.immutable.mutate
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.asPsiType
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.analysis.api.types.defaultType
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.ComputeAllAtOnceAnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolLightSimpleAnnotation
import org.jetbrains.kotlin.light.classes.symbol.annotations.hasDeprecatedAnnotation
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForClassLike
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.GranularModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.InitializedModifiersBox
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightMemberModifierList
import org.jetbrains.kotlin.psi.KtObjectDeclaration

@OptIn(KaImplementationDetail::class)
internal class SymbolLightFieldForObject private constructor(
    containingClass: SymbolLightClassForClassLike<*>,
    private val name: String,
    lightMemberOrigin: LightMemberOrigin?,
    override val symbolPointer: KaSymbolPointer<KaNamedClassSymbol>,
    override val kotlinOrigin: KtObjectDeclaration?,
    private val isCompanion: Boolean,
) : SymbolLightField(containingClass, lightMemberOrigin), KaSymbolJavaView<KaNamedClassSymbol> {
    internal constructor(
        objectSymbol: KaNamedClassSymbol,
        name: String,
        lightMemberOrigin: LightMemberOrigin?,
        containingClass: SymbolLightClassForClassLike<*>,
        isCompanion: Boolean,
    ) : this(
        containingClass = containingClass,
        name = name,
        lightMemberOrigin = lightMemberOrigin,
        kotlinOrigin = objectSymbol.sourcePsiSafe(),
        symbolPointer = objectSymbol.createPointer(),
        isCompanion = isCompanion,
    )

    private inline fun <T> withObjectDeclarationSymbol(crossinline action: context(KaSession) (KaNamedClassSymbol) -> T): T =
        symbolPointer.withSymbol(useSiteModule, action)

    override fun getName(): String = name

    override fun getModifierList(): PsiModifierList = cachedValue {
        SymbolLightMemberModifierList(
            containingDeclaration = this,
            modifiersBox = if (isCompanion) {
                GranularModifiersBox(
                    initialValue = GranularModifiersBox.MODALITY_MODIFIERS_MAP.mutate {
                        it[PsiModifier.FINAL] = true
                        it[PsiModifier.STATIC] = true
                    },
                    computer = ::computeCompanionModifiers,
                )
            } else {
                InitializedModifiersBox(PsiModifier.PUBLIC, PsiModifier.STATIC, PsiModifier.FINAL)
            },
            annotationsBox = ComputeAllAtOnceAnnotationsBox { modifierList ->
                listOf(SymbolLightSimpleAnnotation(NotNull::class.java.name, modifierList))
            },
        )
    }

    private fun computeCompanionModifiers(modifier: String): Map<String, Boolean>? {
        if (modifier !in GranularModifiersBox.VISIBILITY_MODIFIERS) return null
        return GranularModifiersBox.computeVisibilityForClass(useSiteModule, symbolPointer, isTopLevel = false)
    }

    private val _isDeprecated: Boolean by lazyPub {
        withObjectDeclarationSymbol { objectSymbol ->
            objectSymbol.hasDeprecatedAnnotation()
        }
    }

    override fun isDeprecated(): Boolean = _isDeprecated

    private val _type: PsiType by lazyPub {
        withObjectDeclarationSymbol { objectSymbol ->
            objectSymbol.defaultType
                .asPsiType(
                    this@SymbolLightFieldForObject,
                    allowErrorTypes = true,
                    allowNonJvmPlatforms = true,
                )
        } ?: nonExistentType()
    }

    override fun getType(): PsiType = _type

    override fun getInitializer(): PsiExpression? = null //TODO

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightFieldForObject || other.useSiteModule != useSiteModule) return false
        if (kotlinOrigin != null || other.kotlinOrigin != null) {
            return other.kotlinOrigin == kotlinOrigin
        }

        return other.containingClass == containingClass &&
                compareSymbolPointers(other.symbolPointer, symbolPointer)
    }

    override fun hashCode(): Int = kotlinOrigin.hashCode()

    override fun isValid(): Boolean = kotlinOrigin?.isValid ?: symbolPointer.isValid(useSiteModule)
}
