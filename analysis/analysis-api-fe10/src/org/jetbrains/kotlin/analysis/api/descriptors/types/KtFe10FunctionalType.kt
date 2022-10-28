/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.types

import org.jetbrains.kotlin.analysis.api.KtTypeProjection
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KtFe10DescNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktNullability
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtClassSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtTypeProjection
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KtFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.asStringForDebugging
import org.jetbrains.kotlin.analysis.api.descriptors.utils.KtFe10JvmTypeMapperContext
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KtClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KtFunctionalType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.SimpleType

internal class KtFe10FunctionalType(
    override val type: SimpleType,
    private val descriptor: FunctionClassDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KtFunctionalType(), KtFe10Type {
    override fun asStringForDebugging(): String = withValidityAssertion { type.asStringForDebugging() }

    override val nullability: KtTypeNullability
        get() = withValidityAssertion { type.ktNullability }

    override val qualifiers: List<KtClassTypeQualifier.KtResolvedClassTypeQualifier>
        get() = withValidityAssertion {
            KtFe10JvmTypeMapperContext.getNestedType(type).allInnerTypes.map { innerType ->
                KtClassTypeQualifier.KtResolvedClassTypeQualifier(
                    innerType.classDescriptor.toKtClassSymbol(analysisContext),
                    innerType.arguments.map { it.toKtTypeProjection(analysisContext) },
                    token
                )
            }
        }

    override val isSuspend: Boolean
        get() = withValidityAssertion { descriptor.functionKind.isSuspendType }

    override val arity: Int
        get() = withValidityAssertion { descriptor.arity }

    override val hasReceiver: Boolean
        get() = withValidityAssertion {
            if (descriptor.functionKind.isReflectType) false
            else type.getReceiverTypeFromFunctionType() != null
        }

    override val receiverType: KtType?
        get() = withValidityAssertion {
            if (descriptor.functionKind.isReflectType) null
            else type.getReceiverTypeFromFunctionType()?.toKtType(analysisContext)
        }

    override val parameterTypes: List<KtType>
        get() = withValidityAssertion {
            when {
                descriptor.functionKind.isReflectType -> type.arguments.dropLast(1)
                else -> type.getValueParameterTypesFromFunctionType()
            }.map { it.type.toKtType(analysisContext) }
        }

    override val returnType: KtType
        get() = withValidityAssertion {
            when {
                descriptor.functionKind.isReflectType -> type.arguments.last().type
                else -> type.getReturnTypeFromFunctionType()
            }.toKtType(analysisContext)
        }

    override val classId: ClassId
        get() = withValidityAssertion {
            ClassId(
                descriptor.functionKind.packageFqName,
                descriptor.functionKind.numberedClassName(descriptor.arity)
            )
        }

    override val classSymbol: KtClassLikeSymbol
        get() = withValidityAssertion { KtFe10DescNamedClassOrObjectSymbol(descriptor, analysisContext) }

    override val ownTypeArguments: List<KtTypeProjection>
        get() = withValidityAssertion { type.arguments.map { it.toKtTypeProjection(analysisContext) } }
}