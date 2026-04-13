/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.records

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.analysis.api.types.KaTypeMappingMode
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.*
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.psi.KtParameter

internal class SymbolLightRecordComponent private constructor(
    private val parameterSymbolPointer: KaSymbolPointer<KaValueParameterSymbol>,
    private val backingFieldSymbolPointer: KaSymbolPointer<KaBackingFieldSymbol>,
    parent: PsiElement,
    private val containingClass: SymbolLightClassBase,
    override val kotlinOrigin: KtParameter?,
) : KtLightElementBase(parent), PsiRecordComponent, KtLightElement<KtParameter, PsiRecordComponent> {
    internal constructor(
        parameterSymbol: KaValueParameterSymbol,
        backingFieldSymbol: KaBackingFieldSymbol,
        parent: PsiElement,
        containingClass: SymbolLightClassBase,
    ) : this(
        parameterSymbolPointer = parameterSymbol.createPointer(),
        backingFieldSymbolPointer = backingFieldSymbol.createPointer(),
        parent = parent,
        containingClass = containingClass,
        kotlinOrigin = parameterSymbol.sourcePsiSafe(),
    )

    private val kaModule: KaModule get() = containingClass.ktModule

    private inline fun <T> withParameterSymbol(crossinline action: KaSession.(KaValueParameterSymbol) -> T): T {
        return parameterSymbolPointer.withSymbol(kaModule, action)
    }

    private val _name: String by lazyPub {
        withParameterSymbol { parameterSymbol ->
            parameterSymbol.name.asString()
        }
    }

    private val _type: PsiType by lazyPub {
        withParameterSymbol { parameterSymbol ->
            parameterSymbol.returnType.asPsiType(
                useSitePosition = this@SymbolLightRecordComponent,
                allowErrorTypes = true,
                mode = KaTypeMappingMode.VALUE_PARAMETER,
                suppressWildcards = suppressWildcardMode(parameterSymbol),
                allowNonJvmPlatforms = true,
            ) ?: this@SymbolLightRecordComponent.nonExistentType()
        }
    }

    override fun getModifierList(): PsiModifierList = cachedValue {
        SymbolLightClassModifierList(
            containingDeclaration = this,
            annotationsBox = GranularAnnotationsBox(
                annotationsProvider = SymbolAnnotationsProvider(ktModule = kaModule, annotatedSymbolPointer = backingFieldSymbolPointer),
                additionalAnnotationsProvider = NullabilityAnnotationsProvider(::typeNullability),
            ),
        )
    }

    override fun hasModifierProperty(name: String): Boolean = modifierList.hasModifierProperty(name)

    private fun typeNullability(): NullabilityAnnotation = withParameterSymbol { parameterSymbol ->
        getRequiredNullabilityAnnotation(parameterSymbol.returnType)
    }

    override fun getContainingClass() = containingClass

    override fun getTypeElement(): PsiTypeElement? = null

    override fun getType(): PsiType = _type

    override fun getInitializer(): PsiExpression? = null

    override fun hasInitializer(): Boolean = false

    override fun computeConstantValue(): Any? = null

    override fun getNameIdentifier(): PsiIdentifier = KtLightIdentifier(this, kotlinOrigin)

    override fun getTextOffset(): Int = kotlinOrigin?.textOffset ?: -1

    override fun setName(name: String): PsiElement = cannotModify()

    override fun getName(): String = _name

    override fun normalizeDeclaration() {
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitRecordComponent(this)
        } else {
            visitor.visitElement(this)
        }
    }

    // Kotlin data classes can't have vararg parameters
    override fun isVarArgs(): Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightRecordComponent || other.kaModule != kaModule) return false

        if (kotlinOrigin != null || other.kotlinOrigin != null) {
            return kotlinOrigin == other.kotlinOrigin
        }

        return compareSymbolPointers(parameterSymbolPointer, other.parameterSymbolPointer)
    }

    override fun hashCode(): Int = kotlinOrigin?.hashCode() ?: _name.hashCode()

    override fun isValid(): Boolean =
        super.isValid() && kotlinOrigin?.isValid() ?: parameterSymbolPointer.isValid(kaModule)
}
