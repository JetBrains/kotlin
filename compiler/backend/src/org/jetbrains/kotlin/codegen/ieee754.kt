/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactoryImpl
import org.jetbrains.kotlin.resolve.calls.smartcasts.getKotlinTypeForComparison
import org.jetbrains.kotlin.resolve.calls.smartcasts.getKotlinTypeWithPossibleSmartCastToFP
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.org.objectweb.asm.Type

class TypeAndNullability(@JvmField val type: Type, @JvmField val isNullable: Boolean)

fun calcProperTypeForIeee754ArithmeticIfNeeded(
    expression: KtExpression,
    bindingContext: BindingContext,
    inferredPrimitiveType: KotlinType?,
    typeMapper: KotlinTypeMapper
): TypeAndNullability? {
    if (inferredPrimitiveType == null) return null
    val ktType = expression.getKotlinTypeForComparison(bindingContext) ?: return null
    val isNullable = TypeUtils.isNullableType(ktType)
    val asmType = typeMapper.mapType(inferredPrimitiveType)
    if (!AsmUtil.isPrimitive(asmType)) return null
    return TypeAndNullability(asmType, isNullable)
}

fun legacyCalcTypeForIeee754ArithmeticIfNeeded(
    expression: KtExpression?,
    bindingContext: BindingContext,
    descriptor: DeclarationDescriptor,
    languageVersionSettings: LanguageVersionSettings
): TypeAndNullability? {
    val ktType = expression.getKotlinTypeWithPossibleSmartCastToFP(
        // NB. Using DataFlowValueFactoryImpl is a hack, but it is ok for 'legacy'
        bindingContext, descriptor, languageVersionSettings, DataFlowValueFactoryImpl(languageVersionSettings)
    )

    if (ktType != null) {
        if (KotlinBuiltIns.isDoubleOrNullableDouble(ktType)) {
            return TypeAndNullability(
                Type.DOUBLE_TYPE,
                TypeUtils.isNullableType(ktType)
            )
        }

        if (KotlinBuiltIns.isFloatOrNullableFloat(ktType)) {
            return TypeAndNullability(
                Type.FLOAT_TYPE,
                TypeUtils.isNullableType(ktType)
            )
        }
    }
    return null
}