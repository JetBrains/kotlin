/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.types

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KaFe10DescNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktNullability
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.maybeLocalClassId
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKaClassSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtTypeProjection
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KaFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.renderForDebugging
import org.jetbrains.kotlin.analysis.api.descriptors.utils.KaFe10JvmTypeMapperContext
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.getAbbreviation

internal class KaFe10UsualClassType(
    override val fe10Type: SimpleType,
    private val descriptor: ClassDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KaUsualClassType(), KaFe10Type {
    override val qualifiers: List<KaClassTypeQualifier.KaResolvedClassTypeQualifier>
        get() = withValidityAssertion {
            val nestedType = KaFe10JvmTypeMapperContext.getNestedType(fe10Type)
            val nonInnerQualifiers =
                generateSequence(nestedType.root.classifierDescriptor.containingDeclaration as? ClassDescriptor) { it.containingDeclaration as? ClassDescriptor }

            buildList {
                nonInnerQualifiers.mapTo(this) {
                    KaClassTypeQualifier.KaResolvedClassTypeQualifier(
                        it.toKaClassSymbol(analysisContext),
                        emptyList(),
                        token
                    )
                }

                nestedType.allInnerTypes.mapTo(this) { innerType ->
                    KaClassTypeQualifier.KaResolvedClassTypeQualifier(
                        innerType.classDescriptor.toKaClassSymbol(analysisContext),
                        innerType.arguments.map { it.toKtTypeProjection(analysisContext) },
                        token
                    )
                }

            }
        }

    override val classId: ClassId
        get() = withValidityAssertion { descriptor.maybeLocalClassId }

    override val symbol: KaClassLikeSymbol
        get() = withValidityAssertion { KaFe10DescNamedClassOrObjectSymbol(descriptor, analysisContext) }

    override val typeArguments: List<KaTypeProjection>
        get() = withValidityAssertion { fe10Type.arguments.map { it.toKtTypeProjection(analysisContext) } }

    override val nullability: KaTypeNullability
        get() = withValidityAssertion { fe10Type.ktNullability }

    override val abbreviatedType: KaUsualClassType?
        get() = withValidityAssertion { fe10Type.getAbbreviation()?.toKtType(analysisContext) as? KaUsualClassType }

    override fun toString(): String {
        return fe10Type.renderForDebugging(analysisContext)
    }
}
