/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.types

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KaFe10DescNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktNullability
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKaClassSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtTypeProjection
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KaFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.renderForDebugging
import org.jetbrains.kotlin.analysis.api.descriptors.utils.KaFe10JvmTypeMapperContext
import org.jetbrains.kotlin.analysis.api.impl.base.KaContextReceiverImpl
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.functions.isSuspendOrKSuspendFunction
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.SimpleType

internal class KaFe10FunctionalType(
    override val fe10Type: SimpleType,
    private val descriptor: FunctionClassDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KaFunctionType(), KaFe10Type {
    override val nullability: KaTypeNullability
        get() = withValidityAssertion { fe10Type.ktNullability }

    override val abbreviatedType: KaUsualClassType?
        get() = withValidityAssertion { null }

    override val qualifiers: List<KaClassTypeQualifier.KaResolvedClassTypeQualifier>
        get() = withValidityAssertion {
            KaFe10JvmTypeMapperContext.getNestedType(fe10Type).allInnerTypes.map { innerType ->
                KaClassTypeQualifier.KaResolvedClassTypeQualifier(
                    innerType.classDescriptor.toKaClassSymbol(analysisContext),
                    innerType.arguments.map { it.toKtTypeProjection(analysisContext) },
                    token
                )
            }
        }

    override val isSuspend: Boolean
        get() = withValidityAssertion { descriptor.functionTypeKind.isSuspendOrKSuspendFunction }

    override val isReflectType: Boolean
        get() = withValidityAssertion { descriptor.functionTypeKind.isReflectType }

    override val arity: Int
        get() = withValidityAssertion { descriptor.arity }

    override val hasContextReceivers: Boolean
        get() = withValidityAssertion { fe10Type.contextFunctionTypeParamsCount() > 0 }

    @OptIn(KaAnalysisApiInternals::class)
    override val contextReceivers: List<KaContextReceiver>
        get() = withValidityAssertion {
            fe10Type.getContextReceiverTypesFromFunctionType().map { receiverType ->
                // Context receivers in function types may not have labels, hence the `null` label.
                KaContextReceiverImpl(
                    receiverType.toKtType(analysisContext),
                    label = null,
                    analysisContext.token,
                )
            }
        }

    override val hasReceiver: Boolean
        get() = withValidityAssertion {
            if (descriptor.functionTypeKind.isReflectType) false
            else fe10Type.getReceiverTypeFromFunctionType() != null
        }

    override val receiverType: KaType?
        get() = withValidityAssertion {
            if (descriptor.functionTypeKind.isReflectType) null
            else fe10Type.getReceiverTypeFromFunctionType()?.toKtType(analysisContext)
        }

    override val parameterTypes: List<KaType>
        get() = withValidityAssertion {
            when {
                descriptor.functionTypeKind.isReflectType -> fe10Type.arguments.dropLast(1)
                else -> fe10Type.getValueParameterTypesFromFunctionType()
            }.map { it.type.toKtType(analysisContext) }
        }

    override val returnType: KaType
        get() = withValidityAssertion {
            when {
                descriptor.functionTypeKind.isReflectType -> fe10Type.arguments.last().type
                else -> fe10Type.getReturnTypeFromFunctionType()
            }.toKtType(analysisContext)
        }

    override val classId: ClassId
        get() = withValidityAssertion {
            ClassId(
                descriptor.functionTypeKind.packageFqName,
                descriptor.functionTypeKind.numberedClassName(descriptor.arity)
            )
        }

    override val symbol: KaClassLikeSymbol
        get() = withValidityAssertion { KaFe10DescNamedClassOrObjectSymbol(descriptor, analysisContext) }

    override val typeArguments: List<KaTypeProjection>
        get() = withValidityAssertion { fe10Type.arguments.map { it.toKtTypeProjection(analysisContext) } }

    override fun toString(): String {
        return fe10Type.renderForDebugging(analysisContext)
    }
}
