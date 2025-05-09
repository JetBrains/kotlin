/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirCompileTimeConstantEvaluator
import org.jetbrains.kotlin.analysis.api.fir.types.qualifiers.UsualClassTypeQualifierBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.buildAbbreviatedType
import org.jetbrains.kotlin.analysis.api.fir.utils.createPointer
import org.jetbrains.kotlin.analysis.api.impl.base.KaBaseContextReceiver
import org.jetbrains.kotlin.analysis.api.impl.base.resolution.KaBaseFunctionValueParameter
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolvedTypeDeclaration
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KaFirFunctionType(
    override val coneType: ConeClassLikeTypeImpl,
    private val builder: KaSymbolByFirBuilder,
) : KaFunctionType(), KaFirType {
    override val token: KaLifetimeToken get() = builder.token

    override val classId: ClassId get() = withValidityAssertion { coneType.lookupTag.classId }

    override val symbol: KaClassLikeSymbol
        get() = withValidityAssertion {
            builder.classifierBuilder.buildClassLikeSymbolByLookupTag(coneType.lookupTag)
                ?: errorWithFirSpecificEntries("Class was not found", coneType = coneType)
        }

    override val typeArguments: List<KaTypeProjection> get() = withValidityAssertion { qualifiers.last().typeArguments }

    override val qualifiers: List<KaResolvedClassTypeQualifier>
        get() = withValidityAssertion {
            UsualClassTypeQualifierBuilder.buildQualifiers(coneType, builder)
        }

    override val annotations: KaAnnotationList
        get() = withValidityAssertion {
            KaFirAnnotationListForType.create(coneType, builder)
        }

    @Deprecated(
        "Use `isMarkedNullable`, `isNullable` or `hasFlexibleNullability` instead. See KDocs for the migration guide",
        replaceWith = ReplaceWith("this.isMarkedNullable")
    )
    @Suppress("Deprecation")
    override val nullability: KaTypeNullability get() = withValidityAssertion { KaTypeNullability.create(coneType.isMarkedNullable) }

    override val abbreviation: KaUsualClassType?
        get() = withValidityAssertion {
            builder.buildAbbreviatedType(coneType)
        }

    override val isSuspend: Boolean get() = withValidityAssertion { coneType.isSuspendOrKSuspendFunctionType(builder.rootSession) }

    override val isReflectType: Boolean
        get() = withValidityAssertion { coneType.functionTypeKind(builder.rootSession)?.isReflectType == true }

    override val arity: Int get() = withValidityAssertion { parameterTypes.size }

    @KaExperimentalApi
    override val contextReceivers: List<KaContextReceiver>
        get() = withValidityAssertion {
            coneType.contextParameterTypes(builder.rootSession)
                .map {
                    // Context receivers in function types may not have labels, hence the `null` label.
                    KaBaseContextReceiver(it.buildKtType(), label = null, token)
                }
        }

    override val hasContextReceivers: Boolean get() = withValidityAssertion { contextReceivers.isNotEmpty() }

    override val receiverType: KaType?
        get() = withValidityAssertion {
            coneType.receiverType(builder.rootSession)?.buildKtType()
        }

    override val hasReceiver: Boolean get() = withValidityAssertion { receiverType != null }

    override val parameterTypes: List<KaType>
        get() = withValidityAssertion {
            coneType.valueParameterTypesWithoutReceivers(builder.rootSession).map { it.buildKtType() }
        }

    override val parameters: List<KaFunctionValueParameter>
        get() = withValidityAssertion {
            buildList {
                parameterTypes.mapIndexedTo(this) { index, parameterType ->
                    val parameterConeType = (parameterType as? KaFirType)?.coneType

                    // Parameters have to be resolved to FirResolvePhase.ANNOTATION_ARGUMENTS
                    // as parameter names can be explicitly provided via @ParameterName annotations.
                    parameterConeType.ensureResolvedTypeDeclaration(builder.rootSession, FirResolvePhase.ANNOTATION_ARGUMENTS)

                    // TODO: Replace with just `parameterConeType.parameterName` after KT-77401
                    val parameterNameAnnotation = parameterConeType?.parameterNameAnnotation
                    val nameArgument = parameterNameAnnotation?.argumentMapping?.mapping[StandardNames.NAME]
                    val name = (FirCompileTimeConstantEvaluator.evaluate(nameArgument)?.value as? String)?.let {
                        Name.identifier(it)
                    }

                    KaBaseFunctionValueParameter(name, parameterType)
                }
            }
        }

    override val returnType: KaType
        get() = withValidityAssertion {
            coneType.returnType(builder.rootSession).buildKtType()
        }

    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
    override fun toString() = coneType.renderForDebugging()

    private fun ConeKotlinType.buildKtType(): KaType = builder.typeBuilder.buildKtType(this)

    @KaExperimentalApi
    override fun createPointer(): KaTypePointer<KaFunctionType> = withValidityAssertion {
        return KaFirFunctionalClassTypePointer(coneType, builder)
    }
}

private class KaFirFunctionalClassTypePointer(
    coneType: ConeClassLikeTypeImpl,
    builder: KaSymbolByFirBuilder,
) : KaTypePointer<KaFunctionType> {
    private val coneTypePointer = coneType.createPointer(builder)

    @KaImplementationDetail
    override fun restore(session: KaSession): KaFunctionType? {
        requireIsInstance<KaFirSession>(session)

        val coneType = coneTypePointer.restore(session) ?: return null
        if (!coneType.isSomeFunctionType(session.resolutionFacade.useSiteFirSession)) {
            return null
        }

        return KaFirFunctionType(coneType, session.firSymbolBuilder)
    }
}