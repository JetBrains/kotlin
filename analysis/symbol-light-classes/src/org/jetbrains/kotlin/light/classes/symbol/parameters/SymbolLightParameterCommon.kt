/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.*
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.psiSafe
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.analysis.api.types.KtTypeMappingMode
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightIdentifier
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.psi.KtParameter

internal abstract class SymbolLightParameterCommon(
    parameterSymbol: KtValueParameterSymbol,
    private val containingMethod: SymbolLightMethodBase
) : SymbolLightParameterBase(containingMethod) {
    protected val parameterSymbolPointer: KtSymbolPointer<KtValueParameterSymbol> = parameterSymbol.createPointer()
    protected val parameterDeclaration: KtParameter? = parameterSymbol.sourcePsiSafe()

    private val _name: String by lazy {
        parameterSymbolPointer.withSymbol(ktModule) {
            it.name.asString()
        }
    }

    override fun getName(): String = _name

    override fun hasModifierProperty(name: String): Boolean = modifierList.hasModifierProperty(name)

    override val kotlinOrigin: KtParameter? = parameterDeclaration ?: parameterSymbol.psiSafe()

    abstract override fun getModifierList(): PsiModifierList

    private val _identifier: PsiIdentifier by lazy {
        KtLightIdentifier(this, parameterDeclaration)
    }

    protected val nullabilityType: NullabilityType by lazyPub {
        val nullabilityApplicable = !containingMethod.hasModifierProperty(PsiModifier.PRIVATE) &&
                !containingMethod.containingClass.let { it.isAnnotationType || it.isEnum }

        if (nullabilityApplicable) {
            parameterSymbolPointer.withSymbol(ktModule) { getTypeNullability(it.returnType) }
        } else {
            NullabilityType.Unknown
        }
    }

    override fun getNameIdentifier(): PsiIdentifier = _identifier

    private val _type by lazy {
        parameterSymbolPointer.withSymbol(ktModule) { parameterSymbol ->
            val convertedType = run {
                val ktType = parameterSymbol.returnType
                val typeMappingMode = when {
                    ktType.isSuspendFunctionType -> KtTypeMappingMode.DEFAULT
                    // TODO: extract type mapping mode from annotation?
                    // TODO: methods with declaration site wildcards?
                    else -> KtTypeMappingMode.VALUE_PARAMETER
                }

                ktType.asPsiType(this@SymbolLightParameterCommon, typeMappingMode)
            } ?: nonExistentType()

            if (parameterSymbol.isVararg) {
                PsiEllipsisType(convertedType, convertedType.annotationProvider)
            } else {
                convertedType
            }
        }
    }

    override fun getType(): PsiType = _type

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightParameterCommon) return false

        if (parameterDeclaration != null) {
            return parameterDeclaration == other.parameterDeclaration && ktModule == other.ktModule
        }

        return ktModule == other.ktModule && compareSymbolPointers(ktModule, parameterSymbolPointer, other.parameterSymbolPointer)
    }

    override fun hashCode(): Int = parameterDeclaration?.hashCode() ?: _name.hashCode()
    override fun isValid(): Boolean = super.isValid() && parameterDeclaration?.isValid ?: parameterSymbolPointer.isValid(ktModule)
}
