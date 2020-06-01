/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.inferenceContext
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.inference.ResolvedCallableReferenceAtom
import org.jetbrains.kotlin.fir.resolve.inference.csBuilder
import org.jetbrains.kotlin.fir.resolve.inference.extractInputOutputTypesFromCallableReferenceExpectedType
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SyntheticSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemOperation
import org.jetbrains.kotlin.resolve.calls.inference.model.SimpleConstraintSystemConstraintPosition
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.*
import org.jetbrains.kotlin.utils.addToStdlib.min


abstract class ResolutionStage {
    abstract suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo)
}

abstract class CheckerStage : ResolutionStage()

internal fun FirExpression.isSuperReferenceExpression(): Boolean {
    return if (this is FirQualifiedAccessExpression) {
        val calleeReference = calleeReference
        calleeReference is FirSuperReference
    } else false
}

internal object CheckExplicitReceiverConsistency : ResolutionStage() {
    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val receiverKind = candidate.explicitReceiverKind
        val explicitReceiver = callInfo.explicitReceiver
        // TODO: add invoke cases
        when (receiverKind) {
            NO_EXPLICIT_RECEIVER -> {
                if (explicitReceiver != null && explicitReceiver !is FirResolvedQualifier && !explicitReceiver.isSuperReferenceExpression()) {
                    return sink.yieldApplicability(CandidateApplicability.WRONG_RECEIVER)
                }
            }
            EXTENSION_RECEIVER, DISPATCH_RECEIVER -> {
                if (explicitReceiver == null) {
                    return sink.yieldApplicability(CandidateApplicability.WRONG_RECEIVER)
                }
            }
            BOTH_RECEIVERS -> {
                if (explicitReceiver == null) {
                    return sink.yieldApplicability(CandidateApplicability.WRONG_RECEIVER)
                }
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
            val receiverType = (callable.receiverTypeRef as FirResolvedTypeRef?)?.type
            if (receiverType != null) return receiverType
            val returnTypeRef = callable.returnTypeRef as? FirResolvedTypeRef ?: return null
            if (!returnTypeRef.isExtensionFunctionType(bodyResolveComponents.session)) return null
            return (returnTypeRef.type.typeArguments.firstOrNull() as? ConeKotlinTypeProjection)?.type
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
            if (explicitReceiverExpression != null &&
                explicitReceiverKind.shouldBeCheckedAgainstExplicit() &&
                !explicitReceiverExpression.isSuperReferenceExpression()
            ) {
                candidate.resolveArgumentExpression(
                    candidate.csBuilder,
                    argument = explicitReceiverExpression,
                    expectedType = candidate.substitutor.substituteOrSelf(expectedReceiverType),
                    expectedTypeRef = explicitReceiverExpression.typeRef,
                    sink = sink,
                    isReceiver = true,
                    isDispatch = this is Dispatch
                )
                sink.yieldIfNeed()
            } else {
                val argumentExtensionReceiverValue = candidate.implicitExtensionReceiverValue
                if (argumentExtensionReceiverValue != null && explicitReceiverKind.shouldBeCheckedAgainstImplicit()) {
                    candidate.resolvePlainArgumentType(
                        candidate.csBuilder,
                        argumentType = argumentExtensionReceiverValue.type,
                        expectedType = candidate.substitutor.substituteOrSelf(expectedReceiverType.type),
                        sink = sink,
                        isReceiver = true,
                        isDispatch = this is Dispatch
                    )
                    sink.yieldIfNeed()
                }
            }
        }
    }
}

internal object MapArguments : ResolutionStage() {
    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val symbol = candidate.symbol as? FirFunctionSymbol<*> ?: return sink.reportApplicability(CandidateApplicability.HIDDEN)
        val function = symbol.fir

        val mapping = mapArguments(callInfo.arguments, function)
        val argumentToParameterMapping = mutableMapOf<FirExpression, FirValueParameter>()
        mapping.parameterToCallArgumentMap.forEach { (valueParameter, resolvedArgument) ->
            when (resolvedArgument) {
                is ResolvedCallArgument.SimpleArgument -> argumentToParameterMapping[resolvedArgument.callArgument] = valueParameter
                is ResolvedCallArgument.VarargArgument -> resolvedArgument.arguments.forEach {
                    argumentToParameterMapping[it] = valueParameter
                }
            }
        }
        candidate.argumentMapping = argumentToParameterMapping

        var applicability = CandidateApplicability.RESOLVED
        mapping.diagnostics.forEach {
            candidate.diagnostics += it
            applicability = min(applicability, it.applicability)
        }
        if (applicability < CandidateApplicability.RESOLVED) {
            return sink.yieldApplicability(applicability)
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
                sink = sink
            )
            if (candidate.system.hasContradiction) {
                sink.yieldApplicability(CandidateApplicability.INAPPLICABLE)
            }
            sink.yieldIfNeed()
        }
    }
}

internal object EagerResolveOfCallableReferences : CheckerStage() {
    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        if (candidate.postponedAtoms.isEmpty()) return
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
            is FirFunction -> callInfo.session.createKFunctionType(
                fir, resultingReceiverType, returnTypeRef,
                expectedParameterNumberWithReceiver = expectedType?.let { it.typeArguments.size - 1 },
                isSuspend = (fir as? FirSimpleFunction)?.isSuspend == true,
                expectedReturnType = extractInputOutputTypesFromCallableReferenceExpectedType(expectedType, callInfo.session)?.outputType
            )
            is FirVariable<*> -> createKPropertyType(fir, resultingReceiverType, returnTypeRef)
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
    propertyOrField: FirVariable<*>,
    receiverType: ConeKotlinType?,
    returnTypeRef: FirResolvedTypeRef
): ConeKotlinType {
    val propertyType = returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: ConeKotlinErrorType("No type for of $propertyOrField")
    return createKPropertyType(
        receiverType, propertyType, isMutable = propertyOrField.isVar
    )
}

private fun FirSession.createKFunctionType(
    function: FirFunction<*>,
    receiverType: ConeKotlinType?,
    returnTypeRef: FirResolvedTypeRef,
    expectedParameterNumberWithReceiver: Int?,
    isSuspend: Boolean,
    expectedReturnType: ConeKotlinType?
): ConeKotlinType {
    // The similar adaptations: defaults and coercion-to-unit happen at org.jetbrains.kotlin.resolve.calls.components.CallableReferencesCandidateFactory.getCallableReferenceAdaptation
    val parameterTypes = mutableListOf<ConeKotlinType>()
    val expectedParameterNumber = when {
        expectedParameterNumberWithReceiver == null -> null
        receiverType != null -> expectedParameterNumberWithReceiver - 1
        else -> expectedParameterNumberWithReceiver
    }
    for ((index, valueParameter) in function.valueParameters.withIndex()) {
        if (expectedParameterNumber == null || index < expectedParameterNumber || valueParameter.defaultValue == null) {
            parameterTypes += valueParameter.returnTypeRef.coneTypeSafe()
                ?: ConeKotlinErrorType("No type for parameter $valueParameter")
        }
    }

    val returnType =
        if (expectedReturnType != null && inferenceContext.run { expectedReturnType.isUnit() })
            expectedReturnType
        else
            returnTypeRef.coneTypeSafe() ?: ConeKotlinErrorType("No type for return type of $function")

    return createFunctionalType(
        parameterTypes, receiverType = receiverType,
        rawReturnType = returnType,
        isKFunctionType = true,
        isSuspend = isSuspend
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

    private fun canSeePrivateMemberOf(
        containingDeclarationOfUseSite: List<FirDeclaration>,
        ownerId: ClassId,
        session: FirSession
    ): Boolean {
        ownerId.ownerIfCompanion(session)?.let { companionOwnerClassId ->
            return canSeePrivateMemberOf(containingDeclarationOfUseSite, companionOwnerClassId, session)
        }

        for (declaration in containingDeclarationOfUseSite) {
            if (declaration !is FirClass<*>) continue
            val boundSymbol = declaration.symbol
            if (boundSymbol.classId.isSame(ownerId)) {
                return true
            }
        }

        return false
    }

    private fun ClassId.ownerIfCompanion(session: FirSession): ClassId? {
        if (outerClassId == null || isLocal) return null
        val ownerSymbol = session.firSymbolProvider.getClassLikeSymbolByFqName(this) as? FirRegularClassSymbol

        return outerClassId.takeIf { ownerSymbol?.fir?.isCompanion == true }
    }

    private fun FirClass<*>.isSubClass(ownerId: ClassId, session: FirSession): Boolean {
        if (classId.isSame(ownerId)) return true

        return lookupSuperTypes(this, lookupInterfaces = true, deep = true, session).any { superType ->
            (superType as? ConeClassLikeType)?.fullyExpandedType(session)?.lookupTag?.classId?.isSame(ownerId) == true
        }
    }

    private fun canSeeProtectedMemberOf(
        containingUseSiteClass: FirClass<*>,
        dispatchReceiver: ReceiverValue?,
        ownerId: ClassId, session: FirSession
    ): Boolean {
        dispatchReceiver?.ownerIfCompanion(session)?.let { companionOwnerClassId ->
            if (containingUseSiteClass.isSubClass(companionOwnerClassId, session)) return true
        }

        // TODO: Add check for receiver, see org.jetbrains.kotlin.descriptors.Visibility#doesReceiverFitForProtectedVisibility
        return containingUseSiteClass.isSubClass(ownerId, session)
    }

    private fun canSeeProtectedMemberOf(
        containingDeclarationOfUseSite: List<FirDeclaration>,
        dispatchReceiver: ReceiverValue?,
        ownerId: ClassId, session: FirSession
    ): Boolean {
        if (canSeePrivateMemberOf(containingDeclarationOfUseSite, ownerId, session)) return true

        for (containingDeclaration in containingDeclarationOfUseSite) {
            if (containingDeclaration !is FirClass<*>) continue
            val boundSymbol = containingDeclaration.symbol
            if (canSeeProtectedMemberOf(boundSymbol.fir, dispatchReceiver, ownerId, session)) return true
        }

        return false
    }

    private fun ReceiverValue?.ownerIfCompanion(session: FirSession): ClassId? =
        (this?.type as? ConeClassLikeType)?.lookupTag?.classId?.ownerIfCompanion(session)

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
        candidate: Candidate
    ): Boolean {
        val callInfo = candidate.callInfo
        val useSiteFile = callInfo.containingFile
        val containingDeclarations = callInfo.containingDeclarations
        val session = callInfo.session
        val provider = session.firProvider
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
                    if (ownerId == null || declaration is FirConstructor && declaration.isFromSealedClass) {
                        val candidateFile = when (symbol) {
                            is FirClassLikeSymbol<*> -> provider.getFirClassifierContainerFileIfAny(symbol)
                            is FirCallableSymbol<*> -> provider.getFirCallableContainerFile(symbol)
                            else -> null
                        }
                        // Top-level: visible in file
                        candidateFile == useSiteFile
                    } else {
                        // Member: visible inside parent class, including all its member classes
                        canSeePrivateMemberOf(containingDeclarations, ownerId, session)
                    }
                } else {
                    false
                }
            }
            Visibilities.PROTECTED -> {
                ownerId != null && canSeeProtectedMemberOf(containingDeclarations, candidate.dispatchReceiverValue, ownerId, session)
            }
            JavaVisibilities.PROTECTED_AND_PACKAGE, JavaVisibilities.PROTECTED_STATIC_VISIBILITY -> {
                if (symbol.packageFqName() == useSiteFile.packageFqName) {
                    true
                } else {
                    ownerId != null && canSeeProtectedMemberOf(containingDeclarations, candidate.dispatchReceiverValue, ownerId, session)
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
            if (!checkVisibility(declaration, symbol, sink, candidate)) {
                return
            }
        }

        if (declaration is FirConstructor) {
            val ownerClassId = declaration.symbol.callableId.classId!!
            val provider = declaration.session.firSymbolProvider
            val classSymbol = provider.getClassLikeSymbolByFqName(ownerClassId)

            if (classSymbol is FirRegularClassSymbol) {
                if (classSymbol.fir.classKind.isSingleton) {
                    sink.yieldApplicability(CandidateApplicability.HIDDEN)
                }
                checkVisibility(classSymbol.fir, classSymbol, sink, candidate)
            }
        }
    }
}

internal object CheckLowPriorityInOverloadResolution : CheckerStage() {
    private val LOW_PRIORITY_IN_OVERLOAD_RESOLUTION_CLASS_ID: ClassId =
        ClassId(FqName("kotlin.internal"), Name.identifier("LowPriorityInOverloadResolution"))

    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val annotations = when (val fir = candidate.symbol.fir) {
            is FirSimpleFunction -> fir.annotations
            is FirProperty -> fir.annotations
            else -> return
        }
        val hasLowPriorityAnnotation = annotations.any {
            val lookupTag = ((it.annotationTypeRef as? FirResolvedTypeRef)?.type as? ConeClassLikeType)?.lookupTag ?: return@any false
            lookupTag.classId == LOW_PRIORITY_IN_OVERLOAD_RESOLUTION_CLASS_ID
        }

        if (hasLowPriorityAnnotation) {
            sink.reportApplicability(CandidateApplicability.RESOLVED_LOW_PRIORITY)
        }
    }
}
