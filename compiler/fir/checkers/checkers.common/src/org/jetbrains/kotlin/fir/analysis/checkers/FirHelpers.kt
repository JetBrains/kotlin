/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import com.intellij.lang.LighterASTNode
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategy
import org.jetbrains.kotlin.diagnostics.isExpression
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.RecursionType.Plain
import org.jetbrains.kotlin.fir.analysis.checkers.RecursionType.ViaTypeParameters
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.BlockExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FinallyBlockExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.JumpNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.multipleDelegatesWithTheSameSignature
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.symbols.resolvedControlFlowGraphReference
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeCheckerProviderContext
import org.jetbrains.kotlin.types.model.isNullableType
import org.jetbrains.kotlin.util.ImplementationStatus
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.getChildren
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

private val INLINE_ONLY_ANNOTATION_CLASS_ID: ClassId = ClassId.topLevel(FqName("kotlin.internal.InlineOnly"))

/**
 * JEP 390 marks JDK classes whose identity must not be relied upon, and JEP 401 migrates all of them to value classes.
 * It is the JVM counterpart of [StandardClassIds.Annotations.WillBecomeValue].
 */
val JDK_INTERNAL_VALUE_BASED_ANNOTATION_CLASS_ID: ClassId = ClassId.fromString("jdk/internal/ValueBased")

context(context: CheckerContext)
fun FirClass.unsubstitutedScope(): FirTypeScope =
    this.unsubstitutedScope(
        withForcedTypeCalculator = true,
        memberRequiredPhase = FirResolvePhase.STATUS,
    )

context(context: CheckerContext)
fun FirClassSymbol<*>.declaredMemberScope(): FirContainingNamesAwareScope =
    this.declaredMemberScope(
        context.sessionHolder.session,
        memberRequiredPhase = FirResolvePhase.STATUS,
    )

/**
 * Returns true if this is a supertype of other.
 */
fun FirClassSymbol<*>.isSupertypeOf(other: FirClassSymbol<*>, session: FirSession): Boolean {
    /**
     * Hides additional parameters.
     */
    fun FirClassSymbol<*>.isSupertypeOf(other: FirClassSymbol<*>, exclude: MutableSet<FirClassSymbol<*>>): Boolean {
        for (it in other.resolvedSuperTypeRefs) {
            val candidate = it.toClassLikeSymbol(session)?.fullyExpandedClass(session) ?: continue

            if (candidate in exclude) {
                continue
            }

            exclude.add(candidate)

            if (candidate == this) {
                return true
            }

            if (this.isSupertypeOf(candidate, exclude)) {
                return true
            }
        }

        return false
    }

    return isSupertypeOf(other, mutableSetOf())
}

private fun ConeKotlinType.isKotlinValueClass(session: FirSession): Boolean {
    // Value classes have `inline` or `value` modifier in FIR
    return toRegularClassSymbol(session)?.isInlineOrValue == true
}

/**
 * Whether this type is a value class type: a Kotlin inline or full value class, or a value class type of the platform
 * (see [isPlatformValueClassType]).
 */
fun ConeKotlinType.isValueClass(session: FirSession): Boolean =
    isKotlinValueClass(session) || isPlatformValueClassType(session)

/**
 * Whether this type is a value class type of the platform, like a Java value class. Like javac, this includes type parameters, captured
 * types and intersection types bounded by such a class. A Kotlin class whose platform class is a value class, like `kotlin.Number` for
 * `java.lang.Number` or `kotlin.Int` for `java.lang.Integer`, is one too.
 *
 * Unlike [FirRegularClassSymbol.isPlatformValueClass], which checks only the class itself.
 */
private fun ConeKotlinType.isPlatformValueClassType(
    session: FirSession,
    visited: MutableSet<FirTypeParameterSymbol> = mutableSetOf(),
): Boolean = unwrapToSimpleTypeUsingLowerBound().isPlatformValueClassType(session, visited)

private fun ConeSimpleKotlinType.isPlatformValueClassType(session: FirSession, visited: MutableSet<FirTypeParameterSymbol>): Boolean =
    when (this) {
        is ConeIntersectionType -> intersectedTypes.any { it.isPlatformValueClassType(session, visited) }
        is ConeTypeParameterType ->
            visited.add(lookupTag.symbol) &&
                    lookupTag.symbol.resolvedBounds.any { it.coneType.isPlatformValueClassType(session, visited) }
        is ConeCapturedType -> constructor.supertypes.orEmpty().any { it.isPlatformValueClassType(session, visited) }
        is ConeClassLikeType -> toRegularClassSymbol(session)?.let {
            it.isPlatformValueClass(session) || it.isMappedToPlatformValueClass(session)
        } == true
        is ConeUnionType, is ConeTypeVariableType, is ConeStubTypeForTypeVariableInSubtyping, is ConeIntegerLiteralType,
            -> false
    }

fun ConeKotlinType.isFlexiblePrimitive(): Boolean {
    return this is ConeFlexibleType && lowerBound.isPrimitiveOrNullablePrimitive && upperBound.isPrimitiveOrNullablePrimitive
}

fun FirRegularClassSymbol.isMappedToPlatformValueClass(session: FirSession): Boolean {
    val platformClassId = session.platformClassMapper.getCorrespondingPlatformClass(classId) ?: return false
    return (platformClassId.toSymbol(session) as? FirRegularClassSymbol)?.isPlatformValueClass(session) == true
}

fun ConeKotlinType.isInlineClass(session: FirSession): Boolean =
    with(session.typeContext) { typeConstructor().isInlineClass() }

fun ConeKotlinType.getValueClassTypeRecursionType(session: FirSession): RecursionType? =
    getValueClassTypeRecursionType(hashSetOf(), session)

enum class RecursionType { Plain, ViaTypeParameters }

private fun ConeKotlinType.getValueClassTypeRecursionType(
    visited: HashSet<ConeKotlinType>, session: FirSession
): RecursionType? = context(session.typeContext) {
    val plainRegularClass = toRegularClassSymbol(session)
    val expectedRecursionType = if (plainRegularClass != null) Plain else ViaTypeParameters

    val asRegularClass = plainRegularClass ?: leastUpperBound(session).toRegularClassSymbol(session) ?: return null
    val primaryConstructor = asRegularClass.primaryConstructorIfAny(session) ?: return null
    val valueClassRepresentation = asRegularClass.valueClassRepresentation
    val isOrWillBeFullValueClass = valueClassRepresentation is FullValueClassRepresentation ||
            valueClassRepresentation == null && asRegularClass.willBecomeKotlinValueClass(session)
    // A nullable full value class is a reference, so recursion through it is allowed (e.g. `value class Nat(val previous: Nat?)`).
    // An inline class is represented by its underlying type, so recursion through it is forbidden even if it is nullable,
    // though there is no need to forbid it for a nullable one either (e.g. `inline value class Nat(val previous: Nat?)`), see KT-86498.
    val isSubjectForCheck = when {
        valueClassRepresentation is InlineClassRepresentation -> true
        valueClassRepresentation is FullValueClassRepresentation && valueClassRepresentation.belongsToAbstractOrSealedValueClass -> false
        else -> isOrWillBeFullValueClass && !isNullableType()
    }
    if (!isSubjectForCheck) return null

    if (!visited.add(this)) return expectedRecursionType
    // A nullable inline class is represented by its underlying type made nullable, e.g. `I?` of `inline value class I(val a: A)` as `A?`,
    // which is not recursive if `A` is a full value class.
    val isNullableInlineClass = valueClassRepresentation is InlineClassRepresentation && isNullableType()
    val hasRecursionInParameters = primaryConstructor.valueParameterSymbols.any {
        val type = it.resolvedReturnType
        val representedType = if (isNullableInlineClass) type.withNullability(nullable = true, session.typeContext) else type
        representedType.getValueClassTypeRecursionType(visited, session) != null
    }
    return (if (hasRecursionInParameters) expectedRecursionType else null).also { visited.remove(this) }
}

context(context: CheckerContext)
fun FirClassLikeSymbol<*>.outerClassSymbol(): FirClassLikeSymbol<*>? {
    if (this !is FirClassSymbol<*>) return null
    return getContainingDeclaration(context.session)
}

context(context: CheckerContext)
fun FirNamedFunctionSymbol.overriddenFunctions(
    containingClass: FirClassSymbol<*>,
): Collection<FirFunctionSymbol<*>> {
    return overriddenFunctions(containingClass, context.session, context.scopeSession)
}

fun FirClass.collectSupertypesWithDelegates(): Map<FirTypeRef, FirFieldSymbol?> {
    val fieldsMap = delegateFieldsMap ?: emptyMap()
    return superTypeRefs.mapIndexed { index, it -> it to fieldsMap[index] }.toMap()
}

/**
 * Returns the modality of the class
 */
fun FirClass.modality(): Modality? {
    return when (this) {
        is FirRegularClass -> modality
        else -> Modality.FINAL
    }
}

@OptIn(SymbolInternals::class)
fun FirClassSymbol<*>.modality(): Modality? {
    lazyResolveToPhase(FirResolvePhase.STATUS)
    return fir.modality()
}

/**
 * Returns a set of [Modality] modifiers which are redundant for the given [FirMemberDeclaration]. If a modality modifier is redundant, the
 * declaration's modality won't be changed by the modifier.
 */
context(context: CheckerContext)
fun FirMemberDeclaration.redundantModalities(defaultModality: Modality): Set<Modality> {
    if (this is FirRegularClass) {
        return when (classKind) {
            ClassKind.INTERFACE -> setOf(Modality.ABSTRACT, Modality.OPEN)
            else -> setOf(defaultModality)
        }
    }

    val containingClass = context.findClosestClassOrObject() ?: return setOf(defaultModality)

    return when {
        isOverride && !containingClass.isFinal -> setOf(Modality.OPEN)
        containingClass.isInterface -> when {
            hasBody() -> setOf(Modality.OPEN)
            else -> setOf(Modality.ABSTRACT, Modality.OPEN)
        }
        else -> setOf(defaultModality)
    }
}

private fun FirDeclaration.hasBody(): Boolean = when (this) {
    is FirNamedFunction -> this.body != null && this.body !is FirEmptyExpressionBlock
    is FirProperty -> this.setter?.body !is FirEmptyExpressionBlock? || this.getter?.body !is FirEmptyExpressionBlock?
    else -> false
}

/**
 * Finds any non-interface supertype and returns it
 * or null if couldn't find any.
 */
context(context: CheckerContext)
fun FirClass.findNonInterfaceSupertype(): FirTypeRef? {
    for (superTypeRef in superTypeRefs) {
        val lookupTag = (superTypeRef.coneType as? ConeClassLikeType)?.lookupTag ?: continue

        val symbol = lookupTag.toClassSymbol() ?: continue

        if (symbol.classKind != ClassKind.INTERFACE) {
            return superTypeRef
        }
    }

    return null
}

val FirFunctionCall.isIterator: Boolean
    get() = this.calleeReference.name == SpecialNames.ITERATOR

fun ConeKotlinType.isSubtypeOfThrowable(session: FirSession): Boolean =
    session.builtinTypes.throwableType.coneType.isSupertypeOf(session.typeContext, this.fullyExpandedType(session))

fun KotlinTypeMarker.isSupertypeOf(context: TypeCheckerProviderContext, type: KotlinTypeMarker?): Boolean =
    type != null && AbstractTypeChecker.isSubtypeOf(context, type, this)

fun FirCallableDeclaration.isInlineOnly(session: FirSession): Boolean = symbol.isInlineOnly(session)

fun FirCallableSymbol<*>.isInlineOnly(session: FirSession): Boolean =
    isInline && hasAnnotation(INLINE_ONLY_ANNOTATION_CLASS_ID, session)

fun isSubtypeForTypeMismatch(context: ConeInferenceContext, subtype: ConeKotlinType, supertype: ConeKotlinType): Boolean {
    val subtypeFullyExpanded = subtype.fullyExpandedType(context.session)
    val supertypeFullyExpanded = supertype.fullyExpandedType(context.session)
    return AbstractTypeChecker.isSubtypeOf(
        context.newTypeCheckerState(
            errorTypesEqualToAnything = true,
            stubTypesEqualToAnything = false,
            dnnTypesEqualToFlexible = false
        ),
        subtypeFullyExpanded,
        supertypeFullyExpanded
    )
}


/**
 * Get the [ImplementationStatus] for this member.
 * The containing symbol is resolved using the declaration-site session.
 *
 * @param parentClassSymbol the contextual class for this query.
 */
context(sessionHolder: SessionAndScopeSessionHolder) @OptIn(ScopeFunctionRequiresPrewarm::class)
fun FirCallableSymbol<*>.getImplementationStatus(
    parentClassSymbol: FirClassSymbol<*>
): ImplementationStatus {
    val containingClassSymbol = getContainingClassSymbol()
    val symbol = this

    if (this.multipleDelegatesWithTheSameSignature == true && containingClassSymbol == parentClassSymbol) {
        return ImplementationStatus.AMBIGUOUSLY_INHERITED
    }

    if (symbol is FirIntersectionCallableSymbol) {
        val dispatchReceiverScope = symbol.dispatchReceiverScope()
        val memberWithBaseScope = MemberWithBaseScope(symbol, dispatchReceiverScope)
        val nonSubsumed = memberWithBaseScope.getNonSubsumedOverriddenSymbols()

        if (containingClassSymbol === parentClassSymbol && !memberWithBaseScope.isTrivialIntersection() && nonSubsumed.subjectToManyNotImplemented()) {
            return ImplementationStatus.AMBIGUOUSLY_INHERITED
        }

        var hasAbstractFromClass = false
        var hasInterfaceDelegation = false
        var hasAbstractVar = false
        var hasImplementation = false
        var hasImplementationVar = false

        for (intersection in nonSubsumed) {
            val unwrapped = intersection.unwrapFakeOverrides()
            val isVar = unwrapped is FirPropertySymbol && unwrapped.isVar
            val isFromClass = unwrapped.getContainingClassSymbol()?.classKind == ClassKind.CLASS

            if (intersection.isAbstract) {
                if (isFromClass) {
                    hasAbstractFromClass = true
                }
                if (isVar) {
                    hasAbstractVar = true
                }
            } else {
                if (intersection.origin == FirDeclarationOrigin.Delegated) {
                    hasInterfaceDelegation = true
                }
                if (isFromClass) {
                    hasImplementation = true
                    if (isVar) {
                        hasImplementationVar = true
                    }
                }
            }
        }

        // In Java 8, non-abstract intersection overrides having abstract symbol from base class
        // still should be implemented in current class (even when they have default interface implementation)
        // Exception to the rule above: interface implementation via delegation
        if (hasAbstractFromClass && !hasInterfaceDelegation) {
            return ImplementationStatus.NOT_IMPLEMENTED
        }
        if (hasAbstractVar && hasImplementation && !hasImplementationVar) {
            return ImplementationStatus.VAR_IMPLEMENTED_BY_VAL
        }
    }

    when (symbol) {
        is FirNamedFunctionSymbol -> {
            if (
                parentClassSymbol is FirRegularClassSymbol &&
                parentClassSymbol.isData &&
                symbol.isMethodOfAny
            ) {
                return ImplementationStatus.INHERITED_OR_SYNTHESIZED
            }
        }
        is FirFieldSymbol -> if (symbol.isJavaOrEnhancement) return ImplementationStatus.CANNOT_BE_IMPLEMENTED
    }

    return when {
        isFinal -> ImplementationStatus.CANNOT_BE_IMPLEMENTED
        containingClassSymbol === parentClassSymbol && (origin == FirDeclarationOrigin.Source || origin == FirDeclarationOrigin.Precompiled) ->
            ImplementationStatus.ALREADY_IMPLEMENTED
        isAbstract -> ImplementationStatus.NOT_IMPLEMENTED
        else -> ImplementationStatus.INHERITED_OR_SYNTHESIZED
    }
}

private fun List<FirCallableSymbol<*>>.subjectToManyNotImplemented(): Boolean {
    var nonAbstractCountInClass = 0
    var nonAbstractCountInInterface = 0
    var abstractCountInInterface = 0
    for (intersectionSymbol in this) {
        val containingClassSymbol = intersectionSymbol.getContainingClassSymbol() as? FirRegularClassSymbol
        val hasInterfaceContainer = containingClassSymbol?.classKind == ClassKind.INTERFACE
        if (intersectionSymbol.modality != Modality.ABSTRACT) {
            if (hasInterfaceContainer) {
                nonAbstractCountInInterface++
            } else {
                nonAbstractCountInClass++
            }
        } else if (hasInterfaceContainer) {
            abstractCountInInterface++
        }
        if (nonAbstractCountInClass + nonAbstractCountInInterface > 1) {
            return true
        }
        if (nonAbstractCountInInterface > 0 && abstractCountInInterface > 0) {
            return true
        }
    }
    return false
}

val Name.isDelegated: Boolean get() = asString().startsWith("\$\$delegate_")

val ConeTypeProjection.isConflictingOrNotInvariant: Boolean get() = kind != ProjectionKind.INVARIANT || this is ConeKotlinTypeConflictingProjection

val CheckerContext.secondToLastContainer: FirElement?
    get() = nthLastContainer(2)

fun CheckerContext.nthLastContainer(n: Int): FirElement? = containingElements.let { it.getOrNull(it.size - n) }

val CheckerContext.isInsideAnnotationCall: Boolean get() = callsOrAssignments.any { it is FirAnnotationCall }

context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkTypeMismatch(
    lValueOriginalType: ConeKotlinType,
    assignment: FirVariableAssignment?,
    rValue: FirExpression,
    source: KtSourceElement,
    isInitializer: Boolean
) {
    var lValueType = lValueOriginalType
    var rValueType = rValue.resolvedType
    if (source.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement) {
        if (!lValueType.isMarkedOrFlexiblyNullable && rValueType.isMarkedOrFlexiblyNullable) {
            val tempType = rValueType
            rValueType = lValueType
            lValueType = tempType
        }
    }

    val typeContext = context.session.typeContext

    // there is nothing to report if types are matching
    if (isSubtypeForTypeMismatch(typeContext, subtype = rValueType, supertype = lValueType)) return

    val resolvedSymbol = assignment?.calleeReference?.toResolvedCallableSymbol() as? FirPropertySymbol
    val receiverType = (assignment?.extensionReceiver ?: assignment?.dispatchReceiver)?.resolvedType

    when {
        resolvedSymbol != null &&
                receiverType != null &&
                lValueType is ConeCapturedType &&
                lValueType.constructor.projection.kind.let { it == ProjectionKind.STAR || it == ProjectionKind.OUT } -> {
            reporter.reportOn(
                assignment.source,
                FirErrors.SETTER_PROJECTED_OUT,
                receiverType,
                lValueType.projectionKindAsString(),
                resolvedSymbol
            )
        }
        rValue.isNullLiteral && !lValueType.isMarkedOrFlexiblyNullable -> {
            reporter.reportOn(rValue.source, FirErrors.NULL_FOR_NONNULL_TYPE, lValueType)
        }
        source.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement || assignment?.source?.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement -> {
            if (!lValueType.isMarkedOrFlexiblyNullable && rValueType.isMarkedOrFlexiblyNullable) {
                val tempType = rValueType
                rValueType = lValueType
                lValueType = tempType
            }
            if (rValueType.isUnit) {
                reporter.reportOn(source, FirErrors.INC_DEC_SHOULD_NOT_RETURN_UNIT)
            } else {
                reporter.reportOn(source, FirErrors.RESULT_TYPE_MISMATCH, lValueType, rValueType)
            }
        }
        else -> {
            if (reportReturnTypeMismatchInLambda(
                    lValueType = lValueType.fullyExpandedType(),
                    rValue = rValue,
                    rValueType = rValueType.fullyExpandedType(),
                )
            ) return

            val calleeSource = (rValue as? FirQualifiedAccessExpression)?.calleeReference?.source
            val factory = when {
                !isInitializer -> FirErrors.ASSIGNMENT_TYPE_MISMATCH
                source.elementType == KtNodeTypes.BACKING_FIELD -> FirErrors.FIELD_INITIALIZER_TYPE_MISMATCH
                calleeSource?.kind is KtFakeSourceElementKind.DesugaredForLoop -> FirErrors.TYPE_MISMATCH
                else -> FirErrors.INITIALIZER_TYPE_MISMATCH
            }

            reporter.reportOn(
                assignment?.source ?: source,
                factory,
                lValueType,
                rValueType,
                context.session.typeContext.isTypeMismatchDueToNullability(rValueType, lValueType)
            )
        }
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
/**
 * Instead of reporting type mismatch on the whole lambda, tries to report more granular type mismatch on the return expressions.
 */
private fun reportReturnTypeMismatchInLambda(
    lValueType: ConeKotlinType,
    rValue: FirExpression,
    rValueType: ConeKotlinType,
): Boolean {
    if (rValue !is FirAnonymousFunctionExpression) return false
    if (!lValueType.isSomeFunctionType(context.session) && lValueType.classId != StandardClassIds.Function) return false

    val expectedReturnType = lValueType.typeArguments.lastOrNull()?.type ?: return false

    val rValueTypeWithExpectedReturnType = rValueType.withArguments(
        rValueType.typeArguments.dropLast(1).plus(expectedReturnType).toTypedArray()
    )

    if (!isSubtypeForTypeMismatch(context.session.typeContext, rValueTypeWithExpectedReturnType, lValueType)) return false

    var reported = false

    for (expression in rValue.anonymousFunction.symbol.getReturnedExpressions()) {
        if (!isSubtypeForTypeMismatch(context.session.typeContext, expression.resolvedType, expectedReturnType)) {
            reported = true
            reporter.reportOn(
                expression.source,
                FirErrors.RETURN_TYPE_MISMATCH,
                expectedReturnType,
                expression.resolvedType,
                rValue.anonymousFunction,
                context.session.typeContext.isTypeMismatchDueToNullability(expression.resolvedType, expectedReturnType)
            )
        }
    }

    return reported
}

fun ConeCapturedType.projectionKindAsString(): String {
    return when (constructor.projection.kind) {
        ProjectionKind.OUT -> "out"
        ProjectionKind.IN -> "in"
        ProjectionKind.STAR -> "star"
        ProjectionKind.INVARIANT -> error("no projection")
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkCondition(condition: FirExpression) {
    val coneType = condition.resolvedType.fullyExpandedType().lowerBoundIfFlexible()
    if (coneType !is ConeErrorType &&
        !coneType.isSubtypeOf(context.session.typeContext, StandardTypes.Boolean)
    ) {
        if (condition is FirFunctionCall &&
            condition.origin == FirFunctionCallOrigin.Operator &&
            condition.calleeReference.name == OperatorNameConventions.HAS_NEXT
        ) {
            reporter.reportOn(condition.source, FirErrors.HAS_NEXT_FUNCTION_TYPE_MISMATCH, coneType)
        } else {
            reporter.reportOn(
                condition.source,
                FirErrors.CONDITION_TYPE_MISMATCH,
                coneType,
                coneType.isNullableBoolean
            )
        }
    }
}

fun extractArgumentsTypeRefAndSource(qualifier: FirResolvedQualifier): List<FirTypeRefSource> {
    return buildList {
        for (typeArgument in qualifier.typeArguments) {
            add(FirTypeRefSource((typeArgument as? FirTypeProjectionWithVariance)?.typeRef, typeArgument.source))
        }
    }
}

fun extractArgumentsTypeRefAndSource(typeRef: FirTypeRef?): List<FirTypeRefSource>? {
    if (typeRef !is FirResolvedTypeRef) return null
    val result = mutableListOf<FirTypeRefSource>()
    when (val delegatedTypeRef = typeRef.delegatedTypeRef) {
        is FirUserTypeRef -> {
            val qualifier = delegatedTypeRef.qualifier

            for (i in qualifier.size - 1 downTo 0) {
                for (typeArgument in qualifier[i].typeArgumentList.typeArguments) {
                    result.add(FirTypeRefSource((typeArgument as? FirTypeProjectionWithVariance)?.typeRef, typeArgument.source))
                }
            }
        }
        is FirFunctionTypeRef -> {
            val parameters = delegatedTypeRef.parameters

            for (contextParameter in delegatedTypeRef.contextParameterTypeRefs) {
                result.add(FirTypeRefSource(contextParameter, contextParameter.source))
            }
            delegatedTypeRef.receiverTypeRef?.let { result.add(FirTypeRefSource(it, it.source)) }
            for (valueParameter in parameters) {
                val valueParamTypeRef = valueParameter.returnTypeRef
                result.add(FirTypeRefSource(valueParamTypeRef, valueParamTypeRef.source))
            }
            val returnTypeRef = delegatedTypeRef.returnTypeRef
            result.add(FirTypeRefSource(returnTypeRef, returnTypeRef.source))
        }
        else -> return null
    }

    return result
}

/*
 * This is phase-safe version of similar function from FirCallCompleter
 *
 * Expect type is only being added to calls in a position of cast argument: foo() as R
 * And that call should be resolved to something materialize()-like: it returns its single generic parameter and doesn't have value parameters
 * fun <T> materialize(): T
 */
fun FirFunctionSymbol<*>.isFunctionForExpectTypeFromCastFeature(): Boolean {
    val typeParameterSymbol = typeParameterSymbols.singleOrNull() ?: return false

    val returnType = resolvedReturnType

    if ((returnType.lowerBoundIfFlexible() as? ConeTypeParameterType)?.lookupTag != typeParameterSymbol.toLookupTag()) return false

    fun FirTypeRef.isBadType() =
        coneType.contains { (it.lowerBoundIfFlexible() as? ConeTypeParameterType)?.lookupTag == typeParameterSymbol.toLookupTag() }

    return valueParameterSymbols.none { it.resolvedReturnTypeRef.isBadType() }
            && resolvedReceiverTypeRef?.isBadType() != true
            && contextParameterSymbols.none { it.resolvedReturnTypeRef.isBadType() }
}

fun FirQualifiedAccessExpression.explicitReceiverIsNotSuperReference(): Boolean {
    return this.explicitReceiver !is FirSuperReceiverExpression
}


internal val KtSourceElement.defaultValueForParameter: KtSourceElement?
    get() = when (this) {
        is KtPsiSourceElement -> (psi as? KtParameter)?.defaultValue?.toKtPsiSourceElement()
        is KtLightSourceElement -> findDefaultValue(this)
    }

private fun findDefaultValue(source: KtLightSourceElement): KtLightSourceElement? {
    var defaultValue: LighterASTNode? = null
    var defaultValueOffset = source.startOffset

    val nodes = source.lighterASTNode.getChildren(source.treeStructure)
    for (node in nodes) {
        if (node.isExpression()) {
            defaultValue = node
            break
        } else {
            defaultValueOffset += node.endOffset - node.startOffset
        }
    }
    if (defaultValue == null) return null

    return defaultValue.toKtLightSourceElement(
        source.treeStructure,
        startOffset = defaultValueOffset,
        endOffset = defaultValueOffset + defaultValue.textLength,
    )
}

val CheckerContext.closestNonLocal: FirBasedSymbol<*>?
    get() {
        for (symbol in containingDeclarations) {
            if (symbol is FirCallableSymbol || symbol is FirAnonymousInitializerSymbol) {
                return symbol
            }
        }
        return containingDeclarations.lastOrNull()
    }

fun CheckerContext.closestNonLocalWith(declaration: FirDeclaration): FirBasedSymbol<*>? {
    for (symbol in containingDeclarations + declaration.symbol) {
        if (symbol is FirCallableSymbol || symbol is FirAnonymousInitializerSymbol) {
            return symbol
        }
    }
    return declaration.symbol
}

val CheckerContext.isTopLevel: Boolean get() = containingDeclarations.lastOrNull().let { it is FirFileSymbol || it is FirScriptSymbol }

/**
 * The containing symbol is resolved using the declaration-site session.
 */
fun FirBasedSymbol<*>.hasAnnotationOrInsideAnnotatedClass(classId: ClassId, session: FirSession): Boolean {
    if (hasAnnotation(classId, session)) return true
    val container = getContainingClassSymbol() ?: return false
    return container.hasAnnotationOrInsideAnnotatedClass(classId, session)
}

fun FirDeclaration.hasAnnotationOrInsideAnnotatedClass(classId: ClassId, session: FirSession): Boolean =
    symbol.hasAnnotationOrInsideAnnotatedClass(classId, session)

fun FirBasedSymbol<*>.getAnnotationFirstArgument(classId: ClassId, session: FirSession): FirExpression? {
    val annotation = getAnnotationWithResolvedArgumentsByClassId(classId, session)
    return annotation?.argumentMapping?.mapping?.values?.firstOrNull()
}

fun FirBasedSymbol<*>.getAnnotationStringParameter(classId: ClassId, session: FirSession): String? {
    val expression = getAnnotationFirstArgument(classId, session) as? FirLiteralExpression
    return expression?.value as? String
}

fun FirBasedSymbol<*>.getAnnotationBooleanParameter(classId: ClassId, session: FirSession): Boolean? {
    val expression = getAnnotationFirstArgument(classId, session) as? FirLiteralExpression
    return expression?.value as? Boolean
}

context(context: CheckerContext)
fun FirElement.isLhsOfAssignment(): Boolean {
    if (this !is FirQualifiedAccessExpression) return false
    val lastQualified = context.callsOrAssignments.lastOrNull { it != this } ?: return false
    return lastQualified is FirVariableAssignment && lastQualified.lValue == this
}

fun ConeKotlinType.leastUpperBound(session: FirSession): ConeKotlinType {
    val upperBounds = collectUpperBounds(session.typeContext).takeIf { it.isNotEmpty() }
        ?: return StandardTypes.NullableAny
    return ConeTypeIntersector.intersectTypes(session.typeContext, upperBounds)
}

context(_: SessionHolder)
fun ConeKotlinType.forEachClassId(f: (ClassId) -> Unit) {
    when (this) {
        is ConeFlexibleType -> lowerBound.forEachClassId(f)
        is ConeDefinitelyNotNullType -> original.forEachClassId(f)
        is ConeCapturedType -> constructor.supertypes?.forEach { it.forEachClassId(f) }
        is ConeIntersectionType -> intersectedTypes.forEach { it.forEachClassId(f) }
        is ConeUnionType -> {
            primaryType.forEachClassId(f)
            richErrorTypes.forEach { it.forEachClassId(f) }
        }
        is ConeTypeParameterType -> lookupTag.symbol.resolvedBounds.forEach { it.coneType.forEachClassId(f) }
        is ConeClassLikeType -> fullyExpandedType().classId.let(f)
        is ConeStubTypeForTypeVariableInSubtyping,
        is ConeTypeVariableType,
        is ConeIntegerLiteralType -> {}
    }
}

context(context: CheckerContext)
fun ConeKotlinType.finalApproximationOrSelf(): ConeKotlinType {
    return context.session.typeApproximator.approximateToSuperType(
        this,
        TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference
    ) ?: this
}

context(context: CheckerContext)
fun FirResolvedQualifier.isStandalone(
): Boolean {
    val lastQualifiedAccess = context.callsOrAssignments.lastOrNull() as? FirQualifiedAccessExpression
    // Note: qualifier isn't standalone when it's in receiver (SomeClass.foo) or getClass (SomeClass::class) position
    if (lastQualifiedAccess?.explicitReceiver === this || lastQualifiedAccess?.dispatchReceiver === this || lastQualifiedAccess?.extensionReceiver == this) return false
    val lastGetClass = context.getClassCalls.lastOrNull()
    if (lastGetClass?.argument === this) return false
    if (isExplicitParentOfResolvedQualifier()) return false

    return true
}

/**
 * @return `true` for qualifiers that are explicit parents (receivers) for other qualifiers, e.g., for `Outer` in `Outer.Nested`
 */
context(context: CheckerContext)
fun FirResolvedQualifier.isExplicitParentOfResolvedQualifier(): Boolean {
    return context.secondToLastContainer.let { it is FirResolvedQualifier && it.explicitParent == this }
}

fun isExplicitTypeArgumentSource(source: KtSourceElement?): Boolean =
    source != null && source.kind !is KtFakeSourceElementKind.ImplicitTypeArgument

val FirTypeProjection.isExplicit: Boolean get() = isExplicitTypeArgumentSource(source)

fun FirAnonymousFunctionSymbol.getReturnedExpressions(): List<FirExpression> {
    val exitNode = resolvedControlFlowGraphReference?.controlFlowGraph?.exitNode ?: return emptyList()

    fun extractReturnedExpression(it: CFGNode<*>): FirExpression? {
        return when (it) {
            is JumpNode -> (it.fir as? FirReturnExpression)?.result
            is BlockExitNode -> (it.fir.statements.lastOrNull() as? FirReturnExpression)?.result
            is FinallyBlockExitNode -> {
                val finallyBlockEnterNode = it.enterNode
                finallyBlockEnterNode.previousNodes.firstOrNull { x -> finallyBlockEnterNode.edgeFrom(x) == exitNode.edgeFrom(it) }
                    ?.let(::extractReturnedExpression)
            }
            else -> null
        }
    }

    return exitNode.previousNodes.mapNotNull(::extractReturnedExpression).distinct()
}

context(context: CheckerContext)
fun ConeKotlinType.isMalformedExpandedType(allowNullableNothing: Boolean): Boolean {
    val expandedType = fullyExpandedType()
    if (expandedType.classId == StandardClassIds.Array) {
        val singleArgumentType = expandedType.typeArguments.singleOrNull()?.type?.fullyExpandedType()
        if (singleArgumentType != null &&
            (singleArgumentType.isNothing || (singleArgumentType.isNullableNothing && !allowNullableNothing))
        ) {
            return true
        }
    }
    return expandedType.containsMalformedArgument(allowNullableNothing)
}

context(context: CheckerContext)
private fun ConeKotlinType.containsMalformedArgument(allowNullableNothing: Boolean): Boolean =
    typeArguments.any {
        it.type?.fullyExpandedType()?.isMalformedExpandedType(allowNullableNothing) == true
    }

context(context: CheckerContext, reporter: DiagnosticReporter)
inline fun KtSourceElement?.requireFeatureSupport(
    feature: LanguageFeature,
    positioningStrategy: SourceElementPositioningStrategy? = null,
    ifSupported: () -> Unit = {},
) {
    if (feature.isDisabled()) {
        reporter.reportOn(this, FirErrors.UNSUPPORTED_FEATURE, feature to context.languageVersionSettings, positioningStrategy)
    } else {
        ifSupported()
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
inline fun FirElement.requireFeatureSupport(
    feature: LanguageFeature,
    positioningStrategy: SourceElementPositioningStrategy? = null,
    ifSupported: () -> Unit = {},
) {
    source.requireFeatureSupport(feature, positioningStrategy, ifSupported)
}

context(context: CheckerContext)
internal val ConeKotlinType.hasStableIdentityForAtomicOperations: Boolean
    get() = fullyExpandedType().unwrapToSimpleTypeUsingLowerBound().let {
        !it.isPrimitiveOrNullablePrimitive && !it.isValueClass(context.session)
    }

context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkAtomicCallReceiverForStableIdentity(
    type: ConeKotlinType,
    source: KtSourceElement?,
    atomicReferenceClassId: ClassId,
    appropriateCandidatesForArgument: Map<ClassId, ClassId>,
) {
    val expanded = type.fullyExpandedType()
    val argument = expanded.typeArguments.firstOrNull()?.type ?: return

    if (!argument.hasStableIdentityForAtomicOperations) {
        val candidate = appropriateCandidatesForArgument[argument.classId]
        reporter.reportOn(source, FirErrors.ATOMIC_REF_WITHOUT_CONSISTENT_IDENTITY, atomicReferenceClassId, argument, candidate)
    }
}

@OptIn(ExperimentalContracts::class)
fun FirBasedSymbol<*>?.isPrimaryConstructor(): Boolean {
    contract {
        returns(true) implies (this@isPrimaryConstructor is FirConstructorSymbol)
    }
    if (this !is FirConstructorSymbol) return false
    return isPrimary
}

fun FirBasedSymbol<*>?.isExpect(): Boolean {
    return when (this) {
        is FirCallableSymbol<*> -> isExpect
        is FirClassLikeSymbol -> isExpect
        else -> false
    }
}

fun FirResolvedQualifier.resolvedSymbolOrCompanionSymbol(): FirClassLikeSymbol<*>? {
    return if (resolvedToCompanionObject) accessedObjectSymbol else qualifierSymbol
}

context(context: CheckerContext)
fun FirExpression.isDispatchReceiver(): Boolean {
    val parentElement = context.containingElements.elementAtOrNull(context.containingElements.size - 2)
    return parentElement is FirQualifiedAccessExpression && parentElement.dispatchReceiver == this
}

/**
 * Determines if there is a potential ambiguity in interpreting the given expression as an integer literal or an integer literal operator call.
 *
 * The problem is the integer literal (or operator call) might have an ambiguous type like
 * ([Long] | [Int] | [Short] | [Byte]) or ([ULong] | [UInt] | [UShort] | [UByte]).
 * The only resolution disambiguates it to a particular type that sometimes depends on the context (but in most cases it's [Int] and [UInt]).
 * The `as Int`, `toInt`, `as Short`, `toShort` and other similar expressions
 * *always* narrow down the ambiguous type to a particular type (specified on RHS) that prevents the integer type ascription by resolution.
 *
 * Since checkers don't have full-fledged info about the resolution, the redundancy diagnostics on integer literals should not be reported.
 * The ambiguity check is only applicable to literals like `1` or `1U` but not to `1L` or `1UL`,
 * because the latter ones already have unambiguous types ([Long] and [ULong] respectively).
 * The literals for strict [Int], [UInt] types don't exist in Kotlin.
 * The [Short], [Byte], [UShort], [UByte] types aren't expressible via literals at all.
 *
 * @param this the left-hand side expression to be checked (`lhs.toInt()`, `lhs as Int`, `lhs as Short`, etc.)
 * @return `true` if it could cause ambiguity and neither [FirErrors.USELESS_CAST] nor [FirErrors.REDUNDANT_CALL_OF_CONVERSION_METHOD] should be reported.
 */
fun FirExpression.hasIntegerLiteralTypeAmbiguity(): Boolean {
    // The leftmost argument defines the type of the entire integer operation.
    // Moreover, the mixing of different integer types within a single integer operator call (for example, `2U * 4`) is prohibited.
    fun unwrapLeftmostLiteralExpression(expression: FirExpression?): FirLiteralExpression? {
        return when (expression) {
            is FirLiteralExpression -> expression
            is FirIntegerLiteralOperatorCall -> unwrapLeftmostLiteralExpression(expression.dispatchReceiver)
            else -> null
        }
    }

    val maybeIntegerLiteralExpression = unwrapLeftmostLiteralExpression(this)

    return maybeIntegerLiteralExpression?.kind?.let { it == ConstantValueKind.Int || it == ConstantValueKind.UnsignedInt } == true
}

context(context: CheckerContext)
fun canBeEvaluated(expression: FirExpression, allowErrors: Boolean = true): Boolean {
    @OptIn(PrivateConstantEvaluatorAPI::class)
    val evaluationResult = FirExpressionEvaluator.evaluateExpression(expression, context.session)
    return when (evaluationResult) {
        is FirEvaluatorResult.Evaluated -> true
        is FirEvaluatorResult.ResolutionError -> allowErrors
        else -> false
    }
}

/**
 * @return true if the symbol is the constructor of one of 9 array classes (`Array<T>`,
 * `IntArray`, `FloatArray`, ...) which takes the size and an initializer lambda as parameters.
 * Such constructors are marked as `inline` but they are not loaded as such because the `inline`
 * flag is not stored for constructors in the binary metadata. Therefore, we pretend that they
 * are inline.
 */
fun FirFunctionSymbol<*>.isArrayLambdaConstructor(): Boolean {
    return this is FirConstructorSymbol &&
            valueParameterSymbols.size == 2 &&
            resolvedReturnType.isArrayOrPrimitiveArray
}

/**
 * @return true if the symbol is generated `copy` function from a data-class
 */
fun FirFunction.isCopyMethod(): Boolean =
    origin == FirDeclarationOrigin.Synthetic.DataClassMember
            && nameOrSpecialName == StandardNames.DATA_CLASS_COPY
            && getContainingClassSymbol()?.isData == true

@OptIn(SymbolInternals::class)
context(context: CheckerContext)
fun FirBasedSymbol<*>.isExportedToJs(): Boolean {
    val declaration = fir
    val session = context.session

    if (declaration is FirMemberDeclaration) {
        val visibility = declaration.visibility
        if (visibility != Visibilities.Public && visibility != Visibilities.Protected) {
            return false
        }

        /**
         * We don't need to check anything except the `copy` method because
         * - `componentN` are not exported to JS at all. See [org.jetbrains.kotlin.ir.backend.js.lower.ExcludeSyntheticDeclarationsFromExportLowering]
         * - `toString`, `hashCode`, and `equals` are members of `Any` so they are exported anyway. See [org.jetbrains.kotlin.ir.backend.js.ir.shouldDeclarationBeExported]
         */
        if (
            declaration.origin == FirDeclarationOrigin.Synthetic.DataClassMember &&
            declaration.nameOrSpecialName != StandardNames.DATA_CLASS_COPY
        ) {
            return false
        }
    }

    if (hasAnnotationOrInsideAnnotatedClass(StandardClassIds.Annotations.jsExportIgnore, session)) {
        return false
    }

    if (
        hasAnnotationOrInsideAnnotatedClass(StandardClassIds.Annotations.jsExport, session) ||
        hasAnnotationOrInsideAnnotatedClass(StandardClassIds.Annotations.jsExportDefault, session) ||
        getAnnotationBooleanParameter(StandardClassIds.Annotations.jsImplicitExport, session) == true ||
        getContainingFile()?.symbol.hasAnnotation(StandardClassIds.Annotations.jsExport, session)
    ) {
        /**
         * The rules for exporting data class copy functions are inheriting rules for consistent `copy` visibility,
         * described in KT-11914. The migration process is exactly the same as for the visibility modifiers (described in details in the same ticket)
         *
         * For more context see [shouldExportDataClassCopy] from org.jetbrains.kotlin.ir.backend.js.ir package
         */
        if (declaration is FirFunction && declaration.isCopyMethod()) {
            val dataClass = getContainingClassSymbol()
            val primaryConstructor = dataClass?.getPrimaryConstructorSymbol(session, context.scopeSession) ?: return false

            return when {
                dataClass.hasAnnotation(StandardClassIds.Annotations.ExposedCopyVisibility, session) -> true
                primaryConstructor.isExportedToJs() -> true
                LanguageFeature.DataClassCopyRespectsConstructorVisibility.isEnabled() -> false
                else -> !dataClass.hasAnnotation(StandardClassIds.Annotations.ConsistentCopyVisibility, session)
            }
        }

        return true
    }

    return false
}

fun FirBasedSymbol<*>.getContainingFile(): FirFile? {
    return when (this) {
        is FirCallableSymbol<*> -> moduleData.session.firProvider.getFirCallableContainerFile(this)
        is FirClassLikeSymbol<*> -> moduleData.session.firProvider.getFirClassifierContainerFileIfAny(this)
        else -> null
    }
}

internal fun FirDeclaration.containsErrorTypes(): Boolean {
    var hasErrorType = false

    accept(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef) {
            hasErrorType = true
        }

        override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
            if (resolvedTypeRef.coneType.hasError()) {
                hasErrorType = true
            }
        }
    }, null)

    return hasErrorType
}

context(context: CheckerContext)
internal fun isInConstContext(): Boolean {
    if (context.findClosest<FirPropertySymbol> { it.isConst } != null) return true
    if (context.callsOrAssignments.any { it is FirAnnotation }) return true
    return false
}

val ConeKotlinType.isTypealiasToAny: Boolean
    get() = abbreviatedType != null && isAny
