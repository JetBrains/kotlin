/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.GranularAnnotationsBox
import org.jetbrains.kotlin.light.classes.symbol.annotations.NullabilityAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolAnnotationsProvider
import org.jetbrains.kotlin.light.classes.symbol.annotations.suppressWildcardMode
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.light.classes.symbol.modifierLists.SymbolLightClassModifierList
import org.jetbrains.kotlin.psi.KtParameter

internal abstract class SymbolLightParameterCommon(
    protected val parameterSymbolPointer: KaSymbolPointer<KaParameterSymbol>,
    containingMethod: SymbolLightMethodBase,
    override val kotlinOrigin: KtParameter?,
) : SymbolLightParameterBase(containingMethod) {
    internal constructor(
        ktAnalysisSession: KaSession,
        parameterSymbol: KaParameterSymbol,
        containingMethod: SymbolLightMethodBase,
    ) : this(
        parameterSymbolPointer = with(ktAnalysisSession) { parameterSymbol.createPointer() },
        containingMethod = containingMethod,
        kotlinOrigin = parameterSymbol.sourcePsiSafe(),
    )

    private val _name: String by lazyPub {
        parameterSymbolPointer.withSymbol(ktModule) {
            it.name.asString()
        }
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    private val _modifierList: PsiModifierList by lazyPub {
        SymbolLightClassModifierList(
            containingDeclaration = this,
            annotationsBox = GranularAnnotationsBox(
                annotationsProvider = SymbolAnnotationsProvider(
                    ktModule = ktModule,
                    annotatedSymbolPointer = parameterSymbolPointer,
                ),
                additionalAnnotationsProvider = NullabilityAnnotationsProvider(::typeNullability),
            ),
        )
    }

    override fun getName(): String = _name

    override fun hasModifierProperty(name: String): Boolean = modifierList.hasModifierProperty(name)

    protected abstract fun isDeclaredAsVararg(): Boolean
    abstract override fun isVarArgs(): Boolean

    protected open fun typeNullability(): NullabilityAnnotation {
        if (isDeclaredAsVararg()) return NullabilityAnnotation.NON_NULLABLE

        val nullabilityApplicable = !method.hasModifierProperty(PsiModifier.PRIVATE) &&
                !method.containingClass.isAnnotationType &&
                // `enum` synthetic members (e.g., values or valueOf) are not applicable for nullability.
                // In other words, `enum` non-synthetic members are applicable for nullability.
                // Technically, we should retrieve the symbol for the containing method and see if its origin is not synthetic.
                // But, only `enum#valueOf` has a value parameter we want to filter out, so this is cheap yet feasible.
                (!method.containingClass.isEnum || method.name != StandardNames.ENUM_VALUE_OF.identifier)

        return if (nullabilityApplicable) {
            parameterSymbolPointer.withSymbol(ktModule) { getRequiredNullabilityAnnotation(it.returnType) }
        } else {
            NullabilityAnnotation.NOT_REQUIRED
        }
    }

    override fun getNameIdentifier(): PsiIdentifier = KtLightIdentifier(this, kotlinOrigin)

    private val _type by lazyPub {
        parameterSymbolPointer.withSymbol(ktModule) { parameterSymbol ->
            val convertedType = run {
                val ktType = parameterSymbol.returnType

                ktType.asPsiType(
                    this@SymbolLightParameterCommon,
                    allowErrorTypes = true,
                    getTypeMappingMode(ktType),
                    suppressWildcards = suppressWildcardMode(parameterSymbol),
                    allowNonJvmPlatforms = true,
                )
            } ?: nonExistentType()

            if (isDeclaredAsVararg()) {
                if (isVarArgs) {
                    // last vararg
                    PsiEllipsisType(convertedType, convertedType.annotationProvider)
                } else {
                    // non-last vararg
                    PsiArrayType(convertedType, convertedType.annotationProvider)
                }
            } else {
                convertedType
            }
        }
    }

    override fun getType(): PsiType = _type

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class != this::class || (other as SymbolLightParameterCommon).ktModule != ktModule) {
            return false
        }

        if (kotlinOrigin != null || other.kotlinOrigin != null) {
            return kotlinOrigin == other.kotlinOrigin
        }

        return compareSymbolPointers(parameterSymbolPointer, other.parameterSymbolPointer)
    }

    override fun hashCode(): Int = kotlinOrigin?.hashCode() ?: _name.hashCode()
    override fun isValid(): Boolean = super.isValid() && kotlinOrigin?.isValid ?: parameterSymbolPointer.isValid(ktModule)
}
