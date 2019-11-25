/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.*


abstract class ResolutionStage {
    abstract suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo)
}

abstract class CheckerStage : ResolutionStage()

internal object CheckExplicitReceiverConsistency : ResolutionStage() {
    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val receiverKind = candidate.explicitReceiverKind
        val explicitReceiver = callInfo.explicitReceiver
        // TODO: add invoke cases
        when (receiverKind) {
            NO_EXPLICIT_RECEIVER -> {
                if (explicitReceiver != null && explicitReceiver !is FirResolvedQualifier)
                    return sink.yieldApplicability(CandidateApplicability.WRONG_RECEIVER)
            }
            EXTENSION_RECEIVER, DISPATCH_RECEIVER -> {
                if (explicitReceiver == null) return sink.yieldApplicability(CandidateApplicability.WRONG_RECEIVER)
            }
            BOTH_RECEIVERS -> {
                if (explicitReceiver == null) return sink.yieldApplicability(CandidateApplicability.WRONG_RECEIVER)
                // Here we should also check additional invoke receiver
            }
        }
    }
}

internal sealed class CheckReceivers : ResolutionStage() {
    object Dispatch : CheckReceivers() {
        override fun ExplicitReceiverKind.shouldBeCheckedAgainstImplicit(): Boolean {
            return this == EXTENSION_RECEIVER // For NO_EXPLICIT_RECEIVER we can check extension receiver only
        }

        override fun ExplicitReceiverKind.shouldBeCheckedAgainstExplicit(): Boolean {
            return this == DISPATCH_RECEIVER || this == BOTH_RECEIVERS
        }

        override fun Candidate.getReceiverType(): ConeKotlinType? {
            return dispatchReceiverValue?.type
        }
    }

    object Extension : CheckReceivers() {
        override fun ExplicitReceiverKind.shouldBeCheckedAgainstImplicit(): Boolean {
            return this == DISPATCH_RECEIVER || this == NO_EXPLICIT_RECEIVER
        }

        override fun ExplicitReceiverKind.shouldBeCheckedAgainstExplicit(): Boolean {
            return this == EXTENSION_RECEIVER || this == BOTH_RECEIVERS
        }

        override fun Candidate.getReceiverType(): ConeKotlinType? {
            val callableSymbol = symbol as? FirCallableSymbol<*> ?: return null
            val callable = callableSymbol.fir
            return (callable.receiverTypeRef as FirResolvedTypeRef?)?.type
        }
    }

    abstract fun Candidate.getReceiverType(): ConeKotlinType?

    abstract fun ExplicitReceiverKind.shouldBeCheckedAgainstExplicit(): Boolean

    abstract fun ExplicitReceiverKind.shouldBeCheckedAgainstImplicit(): Boolean

    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val expectedReceiverType = candidate.getReceiverType()
        val explicitReceiverExpression = callInfo.explicitReceiver
        val explicitReceiverKind = candidate.explicitReceiverKind

        if (expectedReceiverType != null) {
            if (explicitReceiverExpression != null && explicitReceiverKind.shouldBeCheckedAgainstExplicit()) {
                candidate.resolveArgumentExpression(
                    candidate.csBuilder,
                    argument = explicitReceiverExpression,
                    expectedType = candidate.substitutor.substituteOrSelf(expectedReceiverType),
                    expectedTypeRef = explicitReceiverExpression.typeRef,
                    sink = sink,
                    isReceiver = true,
                    isDispatch = this is Dispatch,
                    isSafeCall = callInfo.isSafeCall,
                    typeProvider = callInfo.typeProvider
                )
                sink.yield()
            } else {
                val argumentExtensionReceiverValue = candidate.implicitExtensionReceiverValue
                if (argumentExtensionReceiverValue != null && explicitReceiverKind.shouldBeCheckedAgainstImplicit()) {
                    resolvePlainArgumentType(
                        candidate.csBuilder,
                        argumentType = argumentExtensionReceiverValue.type,
                        expectedType = candidate.substitutor.substituteOrSelf(expectedReceiverType.type),
                        sink = sink,
                        isReceiver = true,
                        isDispatch = this is Dispatch,
                        isSafeCall = callInfo.isSafeCall
                    )
                    sink.yield()
                }
            }
        }
    }
}

internal object MapArguments : ResolutionStage() {
    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val symbol = candidate.symbol as? FirFunctionSymbol<*> ?: return sink.reportApplicability(CandidateApplicability.HIDDEN)
        val function = symbol.fir
        val processor = FirCallArgumentsProcessor(function, callInfo.arguments)
        val mappingResult = processor.process()
        candidate.argumentMapping = mappingResult.argumentMapping
        if (!mappingResult.isSuccess) {
            return sink.yieldApplicability(CandidateApplicability.PARAMETER_MAPPING_ERROR)
        }
    }
}

internal object CheckArguments : CheckerStage() {
    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val argumentMapping =
            candidate.argumentMapping ?: throw IllegalStateException("Argument should be already mapped while checking arguments!")
        for ((argument, parameter) in argumentMapping) {
            candidate.resolveArgument(
                argument,
                parameter,
                isReceiver = false,
                isSafeCall = false,
                typeProvider = callInfo.typeProvider,
                sink = sink
            )
            if (candidate.system.hasContradiction) {
                sink.yieldApplicability(CandidateApplicability.INAPPLICABLE)
            }
            sink.yield()
        }
    }
}

internal object EagerResolveOfCallableReferences : CheckerStage() {
    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        for (atom in candidate.postponedAtoms.filterIsInstance<ResolvedCallableReferenceAtom>()) {
            if (!candidate.bodyResolveComponents.callResolver.resolveCallableReference(candidate.csBuilder, atom)) {
                sink.yieldApplicability(CandidateApplicability.INAPPLICABLE)
            }
        }
    }
}

internal object CheckCallableReferenceExpectedType : CheckerStage() {
    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val outerCsBuilder = callInfo.outerCSBuilder ?: return
        val expectedType = callInfo.expectedType
        val candidateSymbol = candidate.symbol as? FirCallableSymbol<*> ?: return

        val resultingReceiverType = when (callInfo.lhs) {
            is DoubleColonLHS.Type -> callInfo.lhs.type.takeIf { callInfo.explicitReceiver !is FirResolvedQualifier }
            else -> null
        }

        val fir = with(candidate.bodyResolveComponents) {
            candidateSymbol.phasedFir
        }

        val returnTypeRef = candidate.bodyResolveComponents.returnTypeCalculator.tryCalculateReturnType(fir)

        val resultingType: ConeKotlinType = when (fir) {
            is FirFunction -> createKFunctionType(fir, resultingReceiverType, returnTypeRef)
            is FirProperty -> createKPropertyType(fir, resultingReceiverType, returnTypeRef)
            else -> ConeKotlinErrorType("Unknown callable kind: ${fir::class}")
        }.let(candidate.substitutor::substituteOrSelf)

        candidate.resultingTypeForCallableReference = resultingType
        candidate.outerConstraintBuilderEffect = fun ConstraintSystemOperation.() {
            addOtherSystem(candidate.system.asReadOnlyStorage())

            val position = SimpleConstraintSystemConstraintPosition //TODO

            if (expectedType != null) {
                addSubtypeConstraint(resultingType, expectedType, position)
            }

            val declarationReceiverType: ConeKotlinType? =
                (fir as? FirCallableMemberDeclaration<*>)?.receiverTypeRef?.coneTypeSafe<ConeKotlinType>()
                    ?.let(candidate.substitutor::substituteOrSelf)

            if (resultingReceiverType != null && declarationReceiverType != null) {
                addSubtypeConstraint(resultingReceiverType, declarationReceiverType, position)
            }
        }

        var isApplicable = true

        outerCsBuilder.runTransaction {
            candidate.outerConstraintBuilderEffect!!(this)

            isApplicable = !hasContradiction

            false
        }

        if (!isApplicable) {
            sink.yieldApplicability(CandidateApplicability.INAPPLICABLE)
        }
    }
}

private fun createKPropertyType(
    property: FirProperty,
    receiverType: ConeKotlinType?,
    returnTypeRef: FirResolvedTypeRef
): ConeKotlinType {
    val propertyType = returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: ConeKotlinErrorType("No type for of $property")
    return createKPropertyType(
        receiverType, propertyType, isMutable = property.isVar
    )
}

private fun createKFunctionType(
    function: FirFunction<*>,
    receiverType: ConeKotlinType?,
    returnTypeRef: FirResolvedTypeRef
): ConeKotlinType {
    val parameterTypes = function.valueParameters.map {
        it.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: ConeKotlinErrorType("No type for parameter $it")
    }

    return createFunctionalType(
        parameterTypes, receiverType = receiverType,
        rawReturnType = returnTypeRef.coneTypeSafe() ?: ConeKotlinErrorType("No type for return type of $function"),
        isKFunctionType = true
    )
}

internal object DiscriminateSynthetics : CheckerStage() {
    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        if (candidate.symbol is SyntheticSymbol) {
            sink.reportApplicability(CandidateApplicability.SYNTHETIC_RESOLVED)
        }
    }
}

internal object CheckVisibility : CheckerStage() {
    private fun AbstractFirBasedSymbol<*>.packageFqName(): FqName {
        return when (this) {
            is FirClassLikeSymbol<*> -> classId.packageFqName
            is FirCallableSymbol<*> -> callableId.packageName
            else -> error("No package fq name for $this")
        }
    }

    // 'local' isn't taken into account here
    private fun ClassId.isSame(other: ClassId): Boolean =
        packageFqName == other.packageFqName && relativeClassName == other.relativeClassName

    private fun ImplicitReceiverStack.canSeePrivateMemberOf(ownerId: ClassId): Boolean {
        for (implicitReceiverValue in receiversAsReversed()) {
            if (implicitReceiverValue !is ImplicitDispatchReceiverValue) continue
            val boundSymbol = implicitReceiverValue.boundSymbol
            if (boundSymbol.classId.isSame(ownerId)) {
                return true
            }
            if (boundSymbol is FirRegularClassSymbol && boundSymbol.fir.companionObject?.symbol?.classId?.isSame(ownerId) == true) {
                return true
            }
        }
        return false
    }

    private fun FirRegularClassSymbol.canSeeProtectedMemberOf(
        ownerId: ClassId,
        session: FirSession,
        visited: MutableSet<ClassId>
    ): Boolean {
        if (classId in visited) return false
        visited += classId
        if (classId.isSame(ownerId)) return true
        val superTypes = fir.superConeTypes
        for (superType in superTypes) {
            val superTypeSymbol = superType.lookupTag.toSymbol(session) as? FirRegularClassSymbol ?: continue
            if (superTypeSymbol.canSeeProtectedMemberOf(ownerId, session, visited)) return true
        }
        return false
    }

    private fun ImplicitReceiverStack.canSeeProtectedMemberOf(ownerId: ClassId, session: FirSession): Boolean {
        if (canSeePrivateMemberOf(ownerId)) return true
        val visited = mutableSetOf<ClassId>()
        for (implicitReceiverValue in receiversAsReversed()) {
            if (implicitReceiverValue !is ImplicitDispatchReceiverValue) continue
            val boundSymbol = implicitReceiverValue.boundSymbol
            val superTypes = boundSymbol.fir.superConeTypes
            for (superType in superTypes) {
                val superTypeSymbol = superType.lookupTag.toSymbol(session) as? FirRegularClassSymbol ?: continue
                if (superTypeSymbol.canSeeProtectedMemberOf(ownerId, session, visited)) return true
            }
        }
        return false
    }

    private fun AbstractFirBasedSymbol<*>.getOwnerId(): ClassId? {
        return when (this) {
            is FirClassLikeSymbol<*> -> {
                val ownerId = classId.outerClassId
                if (classId.isLocal) {
                    ownerId?.asLocal() ?: classId
                } else {
                    ownerId
                }
            }
            is FirCallableSymbol<*> -> {
                callableId.classId
            }
            else -> {
                throw AssertionError("Unsupported owner search for ${fir.javaClass}: ${fir.render()}")
            }
        }
    }

    private fun ClassId.asLocal(): ClassId = ClassId(packageFqName, relativeClassName, true)

    private suspend fun checkVisibility(
        declaration: FirMemberDeclaration,
        symbol: AbstractFirBasedSymbol<*>,
        sink: CheckerSink,
        callInfo: CallInfo
    ): Boolean {
        val useSiteFile = callInfo.containingFile
        val implicitReceiverStack = callInfo.implicitReceiverStack
        val session = callInfo.session
        val provider = session.firProvider
        val candidateFile = when (symbol) {
            is FirClassLikeSymbol<*> -> provider.getFirClassifierContainerFileIfAny(symbol)
            is FirCallableSymbol<*> -> provider.getFirCallableContainerFile(symbol)
            else -> null
        }
        val ownerId = symbol.getOwnerId()
        val visible = when (declaration.visibility) {
            JavaVisibilities.PACKAGE_VISIBILITY -> {
                symbol.packageFqName() == useSiteFile.packageFqName
            }
            Visibilities.INTERNAL -> {
                declaration.session == callInfo.session
            }
            Visibilities.PRIVATE, Visibilities.PRIVATE_TO_THIS -> {
                if (declaration.session == callInfo.session) {
                    if (ownerId == null) {
                        // Top-level: visible in file
                        candidateFile == useSiteFile
                    } else {
                        // Member: visible inside parent class, including all its member classes
                        implicitReceiverStack.canSeePrivateMemberOf(ownerId)
                    }
                } else {
                    false
                }
            }
            Visibilities.PROTECTED -> {
                ownerId != null && implicitReceiverStack.canSeeProtectedMemberOf(ownerId, session)
            }
            JavaVisibilities.PROTECTED_AND_PACKAGE, JavaVisibilities.PROTECTED_STATIC_VISIBILITY -> {
                if (symbol.packageFqName() == useSiteFile.packageFqName) {
                    true
                } else {
                    ownerId != null && implicitReceiverStack.canSeeProtectedMemberOf(ownerId, session)
                }
            }
            else -> true
        }

        if (!visible) {
            sink.yieldApplicability(CandidateApplicability.HIDDEN)
            return false
        }
        return true
    }

    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val symbol = candidate.symbol
        val declaration = symbol.fir
        if (declaration is FirMemberDeclaration) {
            if (!checkVisibility(declaration, symbol, sink, callInfo)) {
                return
            }
        }

        if (declaration is FirConstructor) {
            val ownerClassId = declaration.symbol.callableId.classId!!
            val provider = declaration.session.firSymbolProvider
            val classSymbol = provider.getClassLikeSymbolByFqName(ownerClassId)

            if (classSymbol is FirRegularClassSymbol) {
                checkVisibility(classSymbol.fir, classSymbol, sink, callInfo)
            }
        }
    }
}
