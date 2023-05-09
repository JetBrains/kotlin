/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.psiSafe
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.annotations.annotateByKtType
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.psi.KtParameter

internal abstract class SymbolLightParameterCommon(
    protected val parameterSymbolPointer: KtSymbolPointer<KtValueParameterSymbol>,
    protected val parameterDeclaration: KtParameter?,
    private val containingMethod: SymbolLightMethodBase,
    override val kotlinOrigin: KtParameter?,
) : SymbolLightParameterBase(containingMethod) {
    internal constructor(
        ktAnalysisSession: KtAnalysisSession,
        parameterSymbol: KtValueParameterSymbol,
        containingMethod: SymbolLightMethodBase,
    ) : this(
        parameterSymbolPointer = with(ktAnalysisSession) { parameterSymbol.createPointer() },
        parameterDeclaration = parameterSymbol.sourcePsiSafe(),
        containingMethod = containingMethod,
        kotlinOrigin = parameterSymbol.psiSafe(),
    )

    private val _name: String by lazyPub {
        parameterSymbolPointer.withSymbol(ktModule) {
            it.name.asString()
        }
    }

    override fun getName(): String = _name

    override fun hasModifierProperty(name: String): Boolean = modifierList.hasModifierProperty(name)

    abstract override fun getModifierList(): PsiModifierList

    protected open fun isDeclaredAsVararg(): Boolean =
        parameterSymbolPointer.withSymbol(ktModule) { it.isVararg }

    override fun isVarArgs() =
        // true only if this is "last" `vararg`
        containingMethod.parameterList.parameters.lastOrNull() == this && isDeclaredAsVararg()

    protected open fun nullabilityType(): NullabilityType {
        if (isDeclaredAsVararg()) return NullabilityType.NotNull

        val nullabilityApplicable = !containingMethod.hasModifierProperty(PsiModifier.PRIVATE) &&
                !containingMethod.containingClass.isAnnotationType &&
                // `enum` synthetic members (e.g., values or valueOf) are not applicable for nullability.
                // In other words, `enum` non-synthetic members are applicable for nullability.
                // Technically, we should retrieve the symbol for the containing method and see if its origin is not synthetic.
                // But, only `enum#valueOf` has a value parameter we want to filter out, so this is cheap yet feasible.
                (!containingMethod.containingClass.isEnum || containingMethod.name != StandardNames.ENUM_VALUE_OF.identifier)

        return if (nullabilityApplicable) {
            parameterSymbolPointer.withSymbol(ktModule) { getTypeNullability(it.returnType) }
        } else {
            NullabilityType.Unknown
        }
    }

    override fun getNameIdentifier(): PsiIdentifier = KtLightIdentifier(this, parameterDeclaration)

    private val _type by lazyPub {
        parameterSymbolPointer.withSymbol(ktModule) { parameterSymbol ->
            val convertedType = run {
                val ktType = parameterSymbol.returnType
                val typeMappingMode = when {
                    ktType.isSuspendFunctionType -> KtTypeMappingMode.DEFAULT
                    // TODO: extract type mapping mode from annotation?
                    // TODO: methods with declaration site wildcards?
                    else -> KtTypeMappingMode.VALUE_PARAMETER
                }

                ktType.asPsiTypeElement(
                    this@SymbolLightParameterCommon,
                    allowErrorTypes = true,
                    typeMappingMode
                )?.let {
                    annotateByKtType(it.type, ktType, it, modifierList)
                }
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
        if (other !is SymbolLightParameterCommon || other.ktModule != ktModule) return false

        if (parameterDeclaration != null || other.parameterDeclaration != null) {
            return parameterDeclaration == other.parameterDeclaration
        }

        return compareSymbolPointers(parameterSymbolPointer, other.parameterSymbolPointer)
    }

    override fun hashCode(): Int = parameterDeclaration?.hashCode() ?: _name.hashCode()
    override fun isValid(): Boolean = super.isValid() && parameterDeclaration?.isValid ?: parameterSymbolPointer.isValid(ktModule)
}
