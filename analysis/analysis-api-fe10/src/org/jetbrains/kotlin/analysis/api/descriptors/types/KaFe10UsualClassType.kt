/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KaFe10DescNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.KaFe10DescTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.*
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KaFe10Type
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.renderForDebugging
import org.jetbrains.kotlin.analysis.api.descriptors.utils.KaFe10JvmTypeMapperContext
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseResolvedClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.getAbbreviation

internal class KaFe10UsualClassType(
    override val fe10Type: SimpleType,
    private val descriptor: ClassifierDescriptorWithTypeParameters,
    override val analysisContext: Fe10AnalysisContext
) : KaUsualClassType(), KaFe10Type {
    override val qualifiers: List<KaResolvedClassTypeQualifier>
        get() = withValidityAssertion {
            val nestedType = KaFe10JvmTypeMapperContext.getNestedType(fe10Type)
            val nonInnerQualifiers =
                generateSequence(nestedType.root.classifierDescriptor.containingDeclaration as? ClassDescriptor) { it.containingDeclaration as? ClassDescriptor }

            buildList {
                nonInnerQualifiers.mapTo(this) {
                    KaBaseResolvedClassTypeQualifier(
                        it.toKaClassSymbol(analysisContext),
                        emptyList(),
                    )
                }

                nestedType.allInnerTypes.mapTo(this) { innerType ->
                    KaBaseResolvedClassTypeQualifier(
                        innerType.classifierDescriptor.toKtClassifierSymbol(analysisContext)!!,
                        innerType.arguments.map { it.toKtTypeProjection(analysisContext) },
                    )
                }

            }
        }

    override val classId: ClassId
        get() = withValidityAssertion { descriptor.maybeLocalClassId }

    override val symbol: KaClassLikeSymbol
        get() = withValidityAssertion {
            when (descriptor) {
                is ClassDescriptor -> KaFe10DescNamedClassSymbol(descriptor, analysisContext)
                is TypeAliasDescriptor -> KaFe10DescTypeAliasSymbol(descriptor, analysisContext)
                else -> error("Unexpected classifier descriptor type ${descriptor::class.simpleName}")
            }
        }

    override val typeArguments: List<KaTypeProjection>
        get() = withValidityAssertion { fe10Type.arguments.map { it.toKtTypeProjection(analysisContext) } }

    @Deprecated(
        "Use `isMarkedNullable`, `isNullable` or `hasFlexibleNullability` instead. See KDocs for the migration guide",
        replaceWith = ReplaceWith("this.isMarkedNullable")
    )
    @Suppress("Deprecation")
    override val nullability: KaTypeNullability
        get() = withValidityAssertion { fe10Type.ktNullability }

    override val abbreviation: KaUsualClassType?
        get() = withValidityAssertion { fe10Type.getAbbreviation()?.toKtType(analysisContext) as? KaUsualClassType }

    override fun toString(): String {
        return fe10Type.renderForDebugging(analysisContext)
    }

    @KaExperimentalApi
    override fun createPointer(): KaTypePointer<KaUsualClassType> = withValidityAssertion {
        throw NotImplementedError("Type pointers are not implemented for FE 1.0")
    }
}
