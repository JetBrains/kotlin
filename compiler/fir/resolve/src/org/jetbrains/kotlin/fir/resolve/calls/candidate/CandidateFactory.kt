/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.candidate

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildErrorFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildErrorProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunctionCopy
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.calls.ConeResolutionAtom.Companion.createRawAtom
import org.jetbrains.kotlin.fir.resolve.isIntegerLiteralOrOperatorCall
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.originalForWrappedIntegerOperator
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzerContext
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind

class CandidateFactory private constructor(
    val context: ResolutionContext,
    private val baseSystem: ConstraintStorage
) {
    @OptIn(FirExtensionApiInternals::class)
    private val callRefinementExtensions = context.session.extensionService.callRefinementExtensions.takeIf { it.isNotEmpty() }

    companion object {
        private fun buildBaseSystem(context: ResolutionContext, callInfo: CallInfo): ConstraintStorage {
            val system = context.inferenceComponents.createConstraintSystem()
            callInfo.argumentAtoms.forEach {
                system.addSubsystemFromAtom(it)
            }
            return system.asReadOnlyStorage()
        }
    }

    constructor(context: ResolutionContext, callInfo: CallInfo) : this(context, buildBaseSystem(context, callInfo))

    /**
     * [createCandidate] doesn't make any guarantees for inapplicable calls. Errors in the call or callee do not necessarily result in an
     * inapplicable [Candidate].
     */
    fun createCandidate(
        callInfo: CallInfo,
        symbol: FirBasedSymbol<*>,
        explicitReceiverKind: ExplicitReceiverKind,
        scope: FirScope?,
        dispatchReceiver: FirExpression? = null,
        givenExtensionReceiverOptions: List<FirExpression> = emptyList(),
        objectsByName: Boolean = false,
        isFromOriginalTypeInPresenceOfSmartCast: Boolean = false,
    ): Candidate {
        var pluginAmbiguity: AmbiguousInterceptedSymbol? = null

        @Suppress("NAME_SHADOWING")
        @OptIn(FirExtensionApiInternals::class)
        val symbol = if (
            callRefinementExtensions != null &&
            callInfo.callKind == CallKind.Function &&
            symbol is FirNamedFunctionSymbol
        ) {
            val result = symbol.replaceFromPluginsIfNeeded(callRefinementExtensions, callInfo)
            pluginAmbiguity = result.second
            result.first
        } else {
            symbol.unwrapIntegerOperatorSymbolIfNeeded(callInfo)
        }

        val result = Candidate(
            symbol,
            ConeResolutionAtom.createRawAtom(dispatchReceiver),
            givenExtensionReceiverOptions.map { ConeResolutionAtom.createRawAtom(it) },
            explicitReceiverKind,
            context.inferenceComponents.constraintSystemFactory,
            baseSystem,
            callInfo,
            scope,
            isFromCompanionObjectTypeScope = when (explicitReceiverKind) {
                ExplicitReceiverKind.EXTENSION_RECEIVER ->
                    givenExtensionReceiverOptions.singleOrNull().isCandidateFromCompanionObjectTypeScope(callInfo.session)
                ExplicitReceiverKind.DISPATCH_RECEIVER -> dispatchReceiver.isCandidateFromCompanionObjectTypeScope(callInfo.session)
                // The following cases are not applicable for companion objects.
                ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, ExplicitReceiverKind.BOTH_RECEIVERS -> false
            },
            isFromOriginalTypeInPresenceOfSmartCast,
            context.bodyResolveContext,
        )

        if (pluginAmbiguity != null) {
            result.addDiagnostic(pluginAmbiguity)
        }

        // The counterpart in FE 1.0 checks if the given descriptor is VariableDescriptor yet not PropertyDescriptor.
        // Here, we explicitly check if the referred declaration/symbol is value parameter, local variable, enum entry, or backing field.
        val callSite = callInfo.callSite
        if (callSite is FirCallableReferenceAccess) {
            when {
                symbol is FirValueParameterSymbol || symbol is FirPropertySymbol && symbol.isLocal || symbol is FirBackingFieldSymbol -> {
                    result.addDiagnostic(
                        Unsupported("References to variables aren't supported yet", callSite.calleeReference.source)
                    )
                }
                symbol is FirEnumEntrySymbol -> {
                    result.addDiagnostic(
                        Unsupported("References to enum entries aren't supported", callSite.calleeReference.source)
                    )
                }
            }
        } else if (objectsByName && symbol.isRegularClassWithoutCompanion(callInfo.session)) {
            result.addDiagnostic(NoCompanionObject)
        }
        if (callInfo.origin == FirFunctionCallOrigin.Operator) {
            val normalizedSymbol = when (symbol) {
                !is FirFunctionSymbol -> symbol
                else -> callInfo.candidateForCommonInvokeReceiver?.symbol?.takeIf { it !is FirFunctionSymbol }
            }
            // Flag all references that are resolved from an convention operator call.
            normalizedSymbol?.let { result.addDiagnostic(NotFunctionAsOperator(normalizedSymbol)) }
        }
        if (symbol is FirPropertySymbol &&
            !context.session.languageVersionSettings.supportsFeature(LanguageFeature.PrioritizedEnumEntries)
        ) {
            val containingClass = symbol.containingClassLookupTag()?.toRegularClassSymbol(context.session)?.fir
            if (containingClass != null && symbol.fir.isEnumEntries(containingClass)) {
                result.addDiagnostic(LowerPriorityToPreserveCompatibilityDiagnostic)
            }
        }
        return result
    }

    @OptIn(FirExtensionApiInternals::class)
    private fun FirNamedFunctionSymbol.replaceFromPluginsIfNeeded(
        callRefinementExtensions: List<FirFunctionCallRefinementExtension>,
        callInfo: CallInfo,
    ): Pair<FirBasedSymbol<*>, AmbiguousInterceptedSymbol?> {
        var pluginAmbiguity: AmbiguousInterceptedSymbol? = null
        fun process(
            result: FirFunctionCallRefinementExtension.CallReturnType,
            extension: FirFunctionCallRefinementExtension
        ): FirNamedFunctionSymbol {
            val newSymbol = FirNamedFunctionSymbol(callableId)
            val function = buildSimpleFunctionCopy(fir) {
                body = null
                this.symbol = newSymbol
                returnTypeRef = result.typeRef
            }
            function.originalCallDataForPluginRefinedCall = OriginalCallData(this, extension)
            result.callback?.invoke(newSymbol)
            return newSymbol
        }

        val variants = callRefinementExtensions.mapNotNull { extension ->
            val result = extension.intercept(callInfo, this)
            result?.let { it to extension }
        }
        val firBasedSymbol = when (variants.size) {
            0 -> {
                unwrapIntegerOperatorSymbolIfNeeded(callInfo)
            }
            1 -> {
                val (result, extension) = variants[0]
                process(result, extension)
            }
            else -> {
                pluginAmbiguity =
                    AmbiguousInterceptedSymbol(variants.map { it.second::class.qualifiedName ?: it.second.javaClass.name })
                unwrapIntegerOperatorSymbolIfNeeded(callInfo)
            }
        }
        return Pair(firBasedSymbol, pluginAmbiguity)
    }

    private fun FirBasedSymbol<*>.isRegularClassWithoutCompanion(session: FirSession): Boolean {
        val referencedClass = (this as? FirClassLikeSymbol<*>)?.fullyExpandedClass(session) ?: return false
        return referencedClass.classKind != ClassKind.OBJECT && referencedClass.companionObjectSymbol == null
    }

    private fun FirBasedSymbol<*>.unwrapIntegerOperatorSymbolIfNeeded(callInfo: CallInfo): FirBasedSymbol<*> {
        if (this !is FirNamedFunctionSymbol) return this
        // There is no need to unwrap unary operators
        if (fir.valueParameters.isEmpty()) return this
        val original = fir.originalForWrappedIntegerOperator ?: return this
        return if (callInfo.arguments.firstOrNull()?.isIntegerLiteralOrOperatorCall() == true) {
            this
        } else {
            original
        }
    }

    private fun FirExpression?.isCandidateFromCompanionObjectTypeScope(useSiteSession: FirSession): Boolean {
        val resolvedQualifier = this?.unwrapSmartcastExpression() as? FirResolvedQualifier ?: return false
        val originClassOfCandidate = this.resolvedType.classId ?: return false
        val companion = resolvedQualifier.symbol?.fullyExpandedClass(useSiteSession)?.fir?.companionObjectSymbol
        return companion?.classId == originClassOfCandidate
    }

    fun createErrorCandidate(callInfo: CallInfo, diagnostic: ConeDiagnostic): Candidate {
        val symbol: FirBasedSymbol<*> = when (callInfo.callKind) {
            is CallKind.VariableAccess -> createErrorPropertySymbol(diagnostic)
            is CallKind.Function,
            is CallKind.DelegatingConstructorCall,
            is CallKind.CallableReference
            -> createErrorFunctionSymbol(diagnostic)
            is CallKind.SyntheticSelect -> throw IllegalStateException()
            is CallKind.SyntheticIdForCallableReferencesResolution -> throw IllegalStateException()
            is CallKind.CustomForIde -> throw IllegalStateException()
        }
        return Candidate(
            symbol,
            dispatchReceiver = null,
            givenExtensionReceiverOptions = emptyList(),
            explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            context.inferenceComponents.constraintSystemFactory,
            baseSystem,
            callInfo,
            originScope = null,
            bodyResolveContext = context.bodyResolveContext,
        )
    }

    private fun createErrorFunctionSymbol(diagnostic: ConeDiagnostic): FirErrorFunctionSymbol {
        return FirErrorFunctionSymbol().also {
            buildErrorFunction {
                moduleData = context.session.moduleData
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                origin = FirDeclarationOrigin.Synthetic.Error
                this.diagnostic = diagnostic
                symbol = it
            }
        }
    }

    private fun createErrorPropertySymbol(diagnostic: ConeDiagnostic): FirErrorPropertySymbol {
        return FirErrorPropertySymbol(diagnostic).also {
            buildErrorProperty {
                moduleData = context.session.moduleData
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                origin = FirDeclarationOrigin.Synthetic.Error
                name = FirErrorPropertySymbol.NAME
                this.diagnostic = diagnostic
                symbol = it
            }
        }
    }
}

private fun processConstraintStorageFromAtom(
    atom: ConeResolutionAtom,
    processor: (ConstraintStorage) -> Unit,
): Boolean {
    return when (atom) {
        is ConeAtomWithCandidate -> {
            processor(atom.candidate.system.asReadOnlyStorage())
            true
        }
        is ConeResolutionAtomWithSingleChild -> {
            processConstraintStorageFromAtom(atom.subAtom ?: return false, processor)
        }
        else -> false
    }
}

fun PostponedArgumentsAnalyzerContext.addSubsystemFromAtom(atom: ConeResolutionAtom): Boolean {
    return processConstraintStorageFromAtom(atom) {
        // If a call inside a lambda uses outer CS,
        // it's already integrated into inference session via FirPCLAInferenceSession.processPartiallyResolvedCall
        if (!it.usesOuterCs) {
            addOtherSystem(it)
        }
    }
}

internal fun FirResolvable.candidate(): Candidate? {
    return when (val callee = this.calleeReference) {
        is FirNamedReferenceWithCandidate -> return callee.candidate
        else -> null
    }
}
