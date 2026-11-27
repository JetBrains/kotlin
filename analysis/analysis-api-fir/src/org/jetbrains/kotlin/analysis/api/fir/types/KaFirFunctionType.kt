/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KaFirContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.components.firSymbolBuilder
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.resolution.KaFirFunctionValueParameter
import org.jetbrains.kotlin.analysis.api.fir.types.qualifiers.UsualClassTypeQualifierBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.ConeTypeRecursionGuard
import org.jetbrains.kotlin.analysis.api.fir.utils.buildAbbreviatedType
import org.jetbrains.kotlin.analysis.api.fir.utils.createPointer
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolvedTypeDeclaration
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.addToStdlib.requireIsInstance

internal class KaFirFunctionType(
    override val coneType: ConeClassLikeTypeImpl,
    override val analysisSession: KaFirSession,
) : KaFunctionType(), KaFirType {

    override val classId: ClassId get() = entryPointImplementation { coneType.lookupTag.classId }

    override val symbol: KaClassLikeSymbol
        get() = entryPointImplementation {
            firSymbolBuilder.classifierBuilder.buildClassLikeSymbolByLookupTag(coneType.lookupTag)
                ?: errorWithFirSpecificEntries("Class was not found", coneType = coneType)
        }

    override val typeArguments: List<KaTypeProjection> get() = entryPointImplementation { qualifiers.last().typeArguments }

    override val qualifiers: List<KaResolvedClassTypeQualifier>
        get() = entryPointImplementation {
            UsualClassTypeQualifierBuilder.buildQualifiers(coneType)
        }

    override val annotations: KaAnnotationList
        get() = entryPointImplementation {
            KaFirAnnotationListForType.create(coneType)
        }

    override val abbreviation: KaUsualClassType?
        get() = entryPointImplementation {
            buildAbbreviatedType(coneType)
        }

    override val isSuspend: Boolean
        get() = entryPointImplementation { coneType.isSuspendOrKSuspendFunctionType(analysisSession.firSession) }

    override val isReflectType: Boolean
        get() = entryPointImplementation { coneType.functionTypeKind(analysisSession.firSession)?.isReflectType == true }

    @Deprecated("Use `parameters.size` instead. See KT-80545", replaceWith = ReplaceWith("parameters.size"))
    override val arity: Int get() = entryPointImplementation { parameterTypes.size }

    override val contextParameterTypes: List<KaType>
        get() = entryPointImplementation {
            coneType.contextParameterTypes(analysisSession.firSession).map { it.buildKtType() }
        }

    @Deprecated("Context receivers in function types are deprecated. Use `contextParameterTypes` instead.")
    @KaExperimentalApi
    override val contextReceivers: List<KaContextReceiver>
        get() = entryPointImplementation {
            // Context receivers in function types may not have labels, hence the `null` label.
            contextParameterTypes.map { KaFirContextReceiver(it, label = null, this.token) }
        }

    override val receiverType: KaType?
        get() = entryPointImplementation {
            coneType.receiverType(analysisSession.firSession)?.buildKtType()
        }

    override val hasReceiver: Boolean get() = entryPointImplementation { receiverType != null }

    override val parameterTypes: List<KaType>
        get() = entryPointImplementation {
            coneType.valueParameterTypesWithoutReceivers(analysisSession.firSession).map { it.buildKtType() }
        }

    override val parameters: List<KaFunctionValueParameter>
        get() = entryPointImplementation {
            parameterTypes.map { parameterType ->
                val parameterConeType = (parameterType as? KaFirType)?.coneType

                // Parameters have to be resolved to FirResolvePhase.ANNOTATION_ARGUMENTS
                // as parameter names can be explicitly provided via @ParameterName annotations.
                parameterConeType.ensureResolvedTypeDeclaration(analysisSession.firSession, FirResolvePhase.ANNOTATION_ARGUMENTS)
                val name = parameterConeType?.valueParameterName(analysisSession.firSession)
                KaFirFunctionValueParameter(name, parameterType)

            }
        }

    override val returnType: KaType
        get() = entryPointImplementation {
            coneType.returnType(analysisSession.firSession).buildKtType()
        }

    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
    override fun toString() = coneType.renderForDebugging()

    private fun ConeKotlinType.buildKtType(): KaType = analysisSession.firSymbolBuilder.typeBuilder.buildKtType(this)

    @KaExperimentalApi
    override fun createPointer(): KaTypePointer<KaFunctionType> = entryPointImplementation {
        return KaFirFunctionalClassTypePointer(coneType, analysisSession)
    }
}

private class KaFirFunctionalClassTypePointer(
    coneType: ConeClassLikeTypeImpl,
    analysisSession: KaFirSession,
) : KaTypePointer<KaFunctionType> {
    private val coneTypePointer = coneType.createPointer(ConeTypeRecursionGuard(), analysisSession = analysisSession)

    @KaImplementationDetail
    override fun restore(session: KaSession): KaFunctionType? {
        requireIsInstance<KaFirSession>(session)

        val coneType = coneTypePointer.restore(analysisSession = session, guard = ConeTypeRecursionGuard()) ?: return null
        if (!coneType.isSomeFunctionType(session.resolutionFacade.useSiteFirSession)) {
            return null
        }

        return KaFirFunctionType(coneType, session)
    }
}
