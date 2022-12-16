/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.types

import org.jetbrains.kotlin.analysis.api.KtTypeProjection
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KtFe10DescNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.ktNullability
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.maybeLocalClassId
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtClassSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtTypeProjection
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KtFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.asStringForDebugging
import org.jetbrains.kotlin.analysis.api.descriptors.utils.KtFe10JvmTypeMapperContext
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KtClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KtUsualClassType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.SimpleType

internal class KtFe10UsualClassType(
    override val fe10Type: SimpleType,
    private val descriptor: ClassDescriptor,
    override val analysisContext: Fe10AnalysisContext
) : KtUsualClassType(), KtFe10Type {
    override fun asStringForDebugging(): String = withValidityAssertion { fe10Type.asStringForDebugging() }

    override val qualifiers: List<KtClassTypeQualifier.KtResolvedClassTypeQualifier>
        get() = withValidityAssertion {
            val nestedType = KtFe10JvmTypeMapperContext.getNestedType(fe10Type)
            val nonInnerQualifiers =
                generateSequence(nestedType.root.classifierDescriptor.containingDeclaration as? ClassDescriptor) { it.containingDeclaration as? ClassDescriptor }

            buildList {
                nonInnerQualifiers.mapTo(this) {
                    KtClassTypeQualifier.KtResolvedClassTypeQualifier(
                        it.toKtClassSymbol(analysisContext),
                        emptyList(),
                        token
                    )
                }

                nestedType.allInnerTypes.mapTo(this) { innerType ->
                    KtClassTypeQualifier.KtResolvedClassTypeQualifier(
                        innerType.classDescriptor.toKtClassSymbol(analysisContext),
                        innerType.arguments.map { it.toKtTypeProjection(analysisContext) },
                        token
                    )
                }

            }
        }

    override val classId: ClassId
        get() = withValidityAssertion { descriptor.maybeLocalClassId }

    override val classSymbol: KtClassLikeSymbol
        get() = withValidityAssertion { KtFe10DescNamedClassOrObjectSymbol(descriptor, analysisContext) }

    override val ownTypeArguments: List<KtTypeProjection>
        get() = withValidityAssertion { fe10Type.arguments.map { it.toKtTypeProjection(analysisContext) } }

    override val nullability: KtTypeNullability
        get() = withValidityAssertion { fe10Type.ktNullability }

}