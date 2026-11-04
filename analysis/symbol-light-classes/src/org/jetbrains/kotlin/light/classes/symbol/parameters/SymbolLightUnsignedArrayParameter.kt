/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.parameters

import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.asPsiType
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.typeCreation.typeCreator
import org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBase
import org.jetbrains.kotlin.light.classes.symbol.utils.NullabilityAnnotation
import org.jetbrains.kotlin.light.classes.symbol.utils.nonExistentType

/**
 * The parameter for an unsigned `vararg`, as in `vararg xs: UInt`, of the boxed method that takes the unsigned array, such as `UIntArray`,
 * instead of a Java vararg.
 *
 * @see org.jetbrains.kotlin.light.classes.symbol.methods.SymbolLightMethodBaseImpl.hasUnsignedVarargAsArray
 */
internal class SymbolLightUnsignedArrayParameter(
    parameterSymbol: KaValueParameterSymbol,
    containingMethod: SymbolLightMethodBase,
) : SymbolLightParameterCommon(parameterSymbol, containingMethod) {
    override fun isDeclaredAsVararg(): Boolean = false

    override fun isVarArgs(): Boolean = false

    override fun typeNullability(): NullabilityAnnotation = NullabilityAnnotation.NON_NULLABLE

    context(session: KaSession)
    override fun computeType(parameterSymbol: KaParameterSymbol): PsiType {
        val arrayType = typeCreator.varargArrayType(parameterSymbol.returnType)
        return arrayType.asPsiType(
            this@SymbolLightUnsignedArrayParameter,
            allowErrorTypes = true,
            getTypeMappingMode(arrayType),
            allowNonJvmPlatforms = true,
        ) ?: nonExistentType()
    }
}
