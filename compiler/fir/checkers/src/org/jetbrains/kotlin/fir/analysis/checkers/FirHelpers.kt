/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.builtins.StandardNames.HASHCODE_NAME
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.primaryConstructorSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.multipleDelegatesWithTheSameSignature
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParameter.VAL_VAR_TOKEN_SET
import org.jetbrains.kotlin.resolve.AnnotationTargetList
import org.jetbrains.kotlin.resolve.AnnotationTargetLists
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeCheckerProviderContext
import org.jetbrains.kotlin.util.ImplementationStatus
import org.jetbrains.kotlin.util.OperatorNameConventions

private val INLINE_ONLY_ANNOTATION_CLASS_ID: ClassId = ClassId.topLevel(FqName("kotlin.internal.InlineOnly"))

fun FirClass.unsubstitutedScope(context: CheckerContext): FirTypeScope =
    this.unsubstitutedScope(context.sessionHolder.session, context.sessionHolder.scopeSession, withForcedTypeCalculator = false)

fun FirClassSymbol<*>.unsubstitutedScope(context: CheckerContext): FirTypeScope =
    this.unsubstitutedScope(context.sessionHolder.session, context.sessionHolder.scopeSession, withForcedTypeCalculator = false)

fun FirTypeRef.toClassLikeSymbol(session: FirSession): FirClassLikeSymbol<*>? {
    return coneTypeSafe<ConeClassLikeType>()?.toSymbol(session)
}

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

fun ConeKotlinType.isValueClass(session: FirSession): Boolean {
    // Value classes have inline modifier in FIR
    return toRegularClassSymbol(session)?.isInline == true
}

fun ConeKotlinType.isSingleFieldValueClass(session: FirSession): Boolean = with(session.typeContext) {
    isRecursiveSingleFieldValueClassType(session) || typeConstructor().isInlineClass()
}

fun ConeKotlinType.isRecursiveSingleFieldValueClassType(session: FirSession) =
    isRecursiveValueClassType(hashSetOf(), session, onlyInline = true)

fun ConeKotlinType.isRecursiveValueClassType(session: FirSession) =
    isRecursiveValueClassType(hashSetOf(), session, onlyInline = false)

private fun ConeKotlinType.isRecursiveValueClassType(visited: HashSet<ConeKotlinType>, session: FirSession, onlyInline: Boolean): Boolean {

    val asRegularClass = this.toRegularClassSymbol(session)?.takeIf { it.isInlineOrValueClass() } ?: return false
    val primaryConstructor = asRegularClass.declarationSymbols
        .firstOrNull { it is FirConstructorSymbol && it.isPrimary } as FirConstructorSymbol?
        ?: return false

    if (primaryConstructor.valueParameterSymbols.size > 1 && onlyInline) return false
    return !visited.add(this) || primaryConstructor.valueParameterSymbols.any {
        it.resolvedReturnTypeRef.coneType.isRecursiveValueClassType(visited, session, onlyInline)
    }.also { visited.remove(this) }
}

/**
 * Returns the FirRegularClass associated with this
 * or null of something goes wrong.
 */
fun FirTypeRef.toRegularClassSymbol(session: FirSession): FirRegularClassSymbol? {
    return coneType.toRegularClassSymbol(session)
}

/**
 * Returns the ClassLikeDeclaration where the Fir object has been defined
 * or null if no proper declaration has been found.
 */
fun FirBasedSymbol<*>.getContainingClassSymbol(session: FirSession): FirClassLikeSymbol<*>? = when (this) {
    is FirCallableSymbol<*> -> containingClassLookupTag()?.toSymbol(session)
    is FirClassLikeSymbol<*> -> getContainingClassLookupTag()?.toSymbol(session)
    is FirAnonymousInitializerSymbol -> dispatchReceiverType?.toSymbol(session) as? FirClassLikeSymbol<*>
    else -> null
}

fun FirDeclaration.getContainingClassSymbol(session: FirSession) = symbol.getContainingClassSymbol(session)

fun FirClassLikeSymbol<*>.outerClassSymbol(context: CheckerContext): FirClassLikeSymbol<*>? {
    if (this !is FirClassSymbol<*>) return null
    return getContainingDeclarationSymbol(context.session)
}

@OptIn(SymbolInternals::class)
fun FirClassSymbol<*>.getContainingDeclarationSymbol(session: FirSession): FirClassLikeSymbol<*>? {
    if (isLocal) {
        return (this as FirRegularClassSymbol).fir.containingClassForLocalAttr?.toFirRegularClassSymbol(session)
    } else {
        val parentId = classId.relativeClassName.parent()
        if (!parentId.isRoot) {
            val containingDeclarationId = ClassId(classId.packageFqName, parentId, false)
            return session.symbolProvider.getClassLikeSymbolByClassId(containingDeclarationId)
        }
    }

    return null
}

/**
 * Returns the closest to the end of context.containingDeclarations
 * item like FirRegularClass or FirAnonymousObject
 * or null if no such item could be found.
 */
fun CheckerContext.findClosestClassOrObject(): FirClass? {
    for (it in containingDeclarations.asReversed()) {
        if (
            it is FirRegularClass ||
            it is FirAnonymousObject
        ) {
            return it as FirClass
        }
    }

    return null
}

/**
 * Returns the list of functions that overridden by given
 */
fun FirSimpleFunction.overriddenFunctions(
    containingClass: FirClassSymbol<*>,
    context: CheckerContext
): List<FirFunctionSymbol<*>> {
    return symbol.overriddenFunctions(containingClass, context)
}

fun FirNamedFunctionSymbol.overriddenFunctions(
    containingClass: FirClassSymbol<*>,
    context: CheckerContext
): List<FirFunctionSymbol<*>> {
    val firTypeScope = containingClass.unsubstitutedScope(
        context.sessionHolder.session,
        context.sessionHolder.scopeSession,
        withForcedTypeCalculator = true
    )

    val overriddenFunctions = mutableListOf<FirFunctionSymbol<*>>()
    firTypeScope.processFunctionsByName(callableId.callableName) { }
    firTypeScope.processOverriddenFunctions(this) {
        overriddenFunctions.add(it)
        ProcessorAction.NEXT
    }

    return overriddenFunctions
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

/**
 * returns implicit modality by FirMemberDeclaration<*>
 */
fun FirMemberDeclaration.implicitModality(context: CheckerContext): Modality {
    if (this is FirRegularClass) {
        return when (classKind) {
            ClassKind.INTERFACE -> Modality.ABSTRACT
            else -> Modality.FINAL
        }
    }

    val containingClass = context.findClosestClassOrObject() ?: return Modality.FINAL

    return when {
        isOverride && !containingClass.isFinal -> Modality.OPEN
        containingClass.isInterface -> when {
            hasBody() -> Modality.OPEN
            else -> Modality.ABSTRACT
        }
        else -> Modality.FINAL
    }
}

private fun FirDeclaration.hasBody(): Boolean = when (this) {
    is FirSimpleFunction -> this.body != null && this.body !is FirEmptyExpressionBlock
    is FirProperty -> this.setter?.body !is FirEmptyExpressionBlock? || this.getter?.body !is FirEmptyExpressionBlock?
    else -> false
}

/**
 * Finds any non-interface supertype and returns it
 * or null if couldn't find any.
 */
fun FirClass.findNonInterfaceSupertype(context: CheckerContext): FirTypeRef? {
    for (superTypeRef in superTypeRefs) {
        val lookupTag = (superTypeRef.coneType as? ConeClassLikeType)?.lookupTag ?: continue

        val symbol = lookupTag.toSymbol(context.session) as? FirClassSymbol<*> ?: continue

        if (symbol.classKind != ClassKind.INTERFACE) {
            return superTypeRef
        }
    }

    return null
}

val FirFunctionCall.isIterator: Boolean
    get() = this.calleeReference.name == SpecialNames.ITERATOR

fun ConeKotlinType.isSubtypeOfThrowable(session: FirSession): Boolean =
    session.builtinTypes.throwableType.type.isSupertypeOf(session.typeContext, this.fullyExpandedType(session))

val FirValueParameter.hasValOrVar: Boolean
    get() {
        val source = this.source ?: return false
        return source.getChild(VAL_VAR_TOKEN_SET) != null
    }

fun KotlinTypeMarker.isSupertypeOf(context: TypeCheckerProviderContext, type: KotlinTypeMarker?): Boolean =
    type != null && AbstractTypeChecker.isSubtypeOf(context, type, this)

fun FirMemberDeclaration.isInlineOnly(session: FirSession): Boolean =
    isInline && hasAnnotation(INLINE_ONLY_ANNOTATION_CLASS_ID, session)

fun isSubtypeForTypeMismatch(context: ConeInferenceContext, subtype: ConeKotlinType, supertype: ConeKotlinType): Boolean {
    val subtypeFullyExpanded = subtype.fullyExpandedType(context.session)
    val supertypeFullyExpanded = supertype.fullyExpandedType(context.session)
    return AbstractTypeChecker.isSubtypeOf(context, subtypeFullyExpanded, supertypeFullyExpanded)
}

fun FirCallableDeclaration.isVisibleInClass(parentClass: FirClass): Boolean {
    return symbol.isVisibleInClass(parentClass.symbol)
}

fun FirCallableSymbol<*>.isVisibleInClass(parentClassSymbol: FirClassSymbol<*>): Boolean {
    val classPackage = parentClassSymbol.classId.packageFqName
    if (visibility == Visibilities.Private ||
        !visibility.visibleFromPackage(classPackage, callableId.packageName)
    ) return false
    if (
        visibility == Visibilities.Internal &&
        (moduleData != parentClassSymbol.moduleData || parentClassSymbol.moduleData in moduleData.friendDependencies)
    ) return false
    return true
}

/**
 * Get the [ImplementationStatus] for this member.
 *
 * @param parentClassSymbol the contextual class for this query.
 */
fun FirCallableSymbol<*>.getImplementationStatus(
    sessionHolder: SessionHolder,
    parentClassSymbol: FirClassSymbol<*>
): ImplementationStatus {
    val containingClassSymbol = getContainingClassSymbol(sessionHolder.session)
    val symbol = this

    if (this.multipleDelegatesWithTheSameSignature == true && containingClassSymbol == parentClassSymbol) {
        return ImplementationStatus.AMBIGUOUSLY_INHERITED
    }

    if (symbol is FirIntersectionCallableSymbol) {
        if (containingClassSymbol === parentClassSymbol && symbol.subjectToManyNotImplemented(sessionHolder)) {
            return ImplementationStatus.AMBIGUOUSLY_INHERITED
        }
        // In Java 8, non-abstract intersection overrides having abstract symbol from base class
        // still should be implemented in current class (even when they have default interface implementation)
        if (symbol.intersections.any {
                @OptIn(SymbolInternals::class)
                val fir = it.fir.unwrapFakeOverrides()
                fir.isAbstract && (fir.getContainingClassSymbol(sessionHolder.session) as? FirRegularClassSymbol)?.classKind == ClassKind.CLASS
            }
        ) {
            // Exception from the rule above: interface implementation via delegation
            if (symbol.intersections.none {
                    @OptIn(SymbolInternals::class)
                    val fir = it.fir
                    fir.origin == FirDeclarationOrigin.Delegated && !fir.isAbstract
                }
            ) {
                return ImplementationStatus.NOT_IMPLEMENTED
            }
        }
    }

    when (symbol) {
        is FirNamedFunctionSymbol -> {
            if (
                parentClassSymbol is FirRegularClassSymbol &&
                parentClassSymbol.isData &&
                symbol.matchesDataClassSyntheticMemberSignatures
            ) {
                return ImplementationStatus.INHERITED_OR_SYNTHESIZED
            }
            // TODO: suspend function overridden by a Java class in the middle is not properly regarded as an override
            if (isSuspend) {
                return ImplementationStatus.INHERITED_OR_SYNTHESIZED
            }
        }
        is FirFieldSymbol -> if (symbol.isJavaOrEnhancement) return ImplementationStatus.CANNOT_BE_IMPLEMENTED
    }

    return when {
        isFinal -> ImplementationStatus.CANNOT_BE_IMPLEMENTED
        containingClassSymbol === parentClassSymbol && (origin == FirDeclarationOrigin.Source || origin == FirDeclarationOrigin.Precompiled) ->
            ImplementationStatus.ALREADY_IMPLEMENTED
        containingClassSymbol is FirRegularClassSymbol && containingClassSymbol.isExpect -> ImplementationStatus.CANNOT_BE_IMPLEMENTED
        isAbstract -> ImplementationStatus.NOT_IMPLEMENTED
        else -> ImplementationStatus.INHERITED_OR_SYNTHESIZED
    }
}


private fun FirIntersectionCallableSymbol.subjectToManyNotImplemented(sessionHolder: SessionHolder): Boolean {
    var nonAbstractCountInClass = 0
    var nonAbstractCountInInterface = 0
    var abstractCountInInterface = 0
    for (intersectionSymbol in intersections) {
        val containingClassSymbol = intersectionSymbol.getContainingClassSymbol(sessionHolder.session) as? FirRegularClassSymbol
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

private val FirNamedFunctionSymbol.matchesDataClassSyntheticMemberSignatures: Boolean
    get() {
        val name = callableId.callableName
        return (name == OperatorNameConventions.EQUALS && matchesEqualsSignature) ||
                (name == HASHCODE_NAME && matchesHashCodeSignature) ||
                (name == OperatorNameConventions.TO_STRING && matchesToStringSignature)
    }

// NB: we intentionally do not check return types
private val FirNamedFunctionSymbol.matchesEqualsSignature: Boolean
    get() {
        val valueParameters = valueParameterSymbols
        return valueParameters.size == 1 && valueParameters[0].resolvedReturnTypeRef.coneType.isNullableAny
    }

private val FirNamedFunctionSymbol.matchesHashCodeSignature: Boolean
    get() = valueParameterSymbols.isEmpty()

private val FirNamedFunctionSymbol.matchesToStringSignature: Boolean
    get() = valueParameterSymbols.isEmpty()

val Name.isDelegated: Boolean get() = asString().startsWith("\$\$delegate_")

val ConeTypeProjection.isConflictingOrNotInvariant: Boolean get() = kind != ProjectionKind.INVARIANT || this is ConeKotlinTypeConflictingProjection

fun checkTypeMismatch(
    lValueOriginalType: ConeKotlinType,
    assignment: FirVariableAssignment?,
    rValue: FirExpression,
    context: CheckerContext,
    source: KtSourceElement,
    reporter: DiagnosticReporter,
    isInitializer: Boolean
) {
    var lValueType = lValueOriginalType
    var rValueType = rValue.typeRef.coneType
    if (source.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement) {
        if (!lValueType.isNullable && rValueType.isNullable) {
            val tempType = rValueType
            rValueType = lValueType
            lValueType = tempType
        }
    }

    val typeContext = context.session.typeContext

    if (!isSubtypeForTypeMismatch(typeContext, subtype = rValueType, supertype = lValueType)) {
        if (rValueType is ConeClassLikeType &&
            rValueType.lookupTag.classId == StandardClassIds.Int &&
            lValueType.fullyExpandedType(context.session).isIntegerTypeOrNullableIntegerTypeOfAnySize &&
            rValueType.nullability == ConeNullability.NOT_NULL
        ) {
            // val p: Byte = 42 or similar situation
            // TODO: remove after fix of KT-46047
            return
        }
        if (lValueType.isExtensionFunctionType || rValueType.isExtensionFunctionType) {
            // TODO: remove after fix of KT-45989
            return
        }
        val resolvedSymbol = assignment?.calleeReference?.toResolvedCallableSymbol() as? FirPropertySymbol
        when {
            resolvedSymbol != null && lValueType is ConeCapturedType && lValueType.constructor.projection.kind.let {
                it == ProjectionKind.STAR || it == ProjectionKind.OUT
            } -> {
                reporter.reportOn(assignment.source, FirErrors.SETTER_PROJECTED_OUT, resolvedSymbol, context)
            }
            rValue.isNullLiteral && lValueType.nullability == ConeNullability.NOT_NULL -> {
                reporter.reportOn(rValue.source, FirErrors.NULL_FOR_NONNULL_TYPE, context)
            }
            isInitializer -> {
                reporter.reportOn(
                    source,
                    FirErrors.INITIALIZER_TYPE_MISMATCH,
                    lValueType,
                    rValueType,
                    context.session.typeContext.isTypeMismatchDueToNullability(rValueType, lValueType),
                    context
                )
            }
            source.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement -> {
                if (!lValueType.isNullable && rValueType.isNullable) {
                    val tempType = rValueType
                    rValueType = lValueType
                    lValueType = tempType
                }
                if (rValueType.isUnit) {
                    reporter.reportOn(source, FirErrors.INC_DEC_SHOULD_NOT_RETURN_UNIT, context)
                } else {
                    reporter.reportOn(source, FirErrors.RESULT_TYPE_MISMATCH, lValueType, rValueType, context)
                }
            }
            else -> {
                reporter.reportOn(
                    source,
                    FirErrors.ASSIGNMENT_TYPE_MISMATCH,
                    lValueType,
                    rValueType,
                    context.session.typeContext.isTypeMismatchDueToNullability(rValueType, lValueType),
                    context
                )
            }
        }
    }
}

internal fun checkCondition(condition: FirExpression, context: CheckerContext, reporter: DiagnosticReporter) {
    val coneType = condition.typeRef.coneType.lowerBoundIfFlexible()
    if (coneType !is ConeErrorType &&
        !coneType.isSubtypeOf(context.session.typeContext, context.session.builtinTypes.booleanType.type)
    ) {
        reporter.reportOn(
            condition.source,
            FirErrors.CONDITION_TYPE_MISMATCH,
            coneType,
            coneType.isNullableBoolean,
            context
        )
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

data class FirTypeRefSource(val typeRef: FirTypeRef?, val source: KtSourceElement?)

val FirClassLikeSymbol<*>.classKind: ClassKind?
    get() = (this as? FirClassSymbol<*>)?.classKind

val FirBasedSymbol<*>.typeParameterSymbols: List<FirTypeParameterSymbol>?
    get() = when (this) {
        is FirCallableSymbol<*> -> typeParameterSymbols
        is FirClassLikeSymbol<*> -> typeParameterSymbols
        else -> null
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

    val returnType = resolvedReturnTypeRef.coneType

    if ((returnType.lowerBoundIfFlexible() as? ConeTypeParameterType)?.lookupTag != typeParameterSymbol.toLookupTag()) return false

    fun FirTypeRef.isBadType() =
        coneType.contains { (it.lowerBoundIfFlexible() as? ConeTypeParameterType)?.lookupTag == typeParameterSymbol.toLookupTag() }

    if (valueParameterSymbols.any { it.resolvedReturnTypeRef.isBadType() } || resolvedReceiverTypeRef?.isBadType() == true) return false

    return true
}

fun getActualTargetList(annotated: FirDeclaration): AnnotationTargetList {
    fun CallableId.isMember(): Boolean {
        return classId != null || isLocal // TODO: Replace with .containingClass (after fixing)
    }

    return when (annotated) {
        is FirRegularClass -> {
            AnnotationTargetList(
                KotlinTarget.classActualTargets(annotated.classKind, annotated.isInner, annotated.isCompanion, annotated.isLocal)
            )
        }
        is FirEnumEntry -> AnnotationTargetList(
            KotlinTarget.classActualTargets(ClassKind.ENUM_ENTRY, annotated.isInner, isCompanionObject = false, isLocalClass = false)
        )
        is FirProperty -> {
            when {
                annotated.isLocal ->
                    if (annotated.source?.kind == KtFakeSourceElementKind.DesugaredComponentFunctionCall) {
                        TargetLists.T_DESTRUCTURING_DECLARATION
                    } else {
                        TargetLists.T_LOCAL_VARIABLE
                    }
                annotated.symbol.callableId.isMember() ->
                    if (annotated.source?.kind == KtFakeSourceElementKind.PropertyFromParameter) {
                        TargetLists.T_VALUE_PARAMETER_WITH_VAL
                    } else {
                        TargetLists.T_MEMBER_PROPERTY(annotated.hasBackingField, annotated.delegate != null)
                    }
                else ->
                    TargetLists.T_TOP_LEVEL_PROPERTY(annotated.hasBackingField, annotated.delegate != null)
            }
        }
        is FirValueParameter -> {
            when {
                annotated.hasValOrVar -> TargetLists.T_VALUE_PARAMETER_WITH_VAL
                else -> TargetLists.T_VALUE_PARAMETER_WITHOUT_VAL
            }
        }
        is FirConstructor -> TargetLists.T_CONSTRUCTOR
        is FirAnonymousFunction -> {
            TargetLists.T_FUNCTION_EXPRESSION
        }
        is FirSimpleFunction -> {
            when {
                annotated.isLocal -> TargetLists.T_LOCAL_FUNCTION
                annotated.symbol.callableId.isMember() -> TargetLists.T_MEMBER_FUNCTION
                else -> TargetLists.T_TOP_LEVEL_FUNCTION
            }
        }
        is FirTypeAlias -> TargetLists.T_TYPEALIAS
        is FirPropertyAccessor -> if (annotated.isGetter) TargetLists.T_PROPERTY_GETTER else TargetLists.T_PROPERTY_SETTER
        is FirBackingField -> TargetLists.T_BACKING_FIELD
        is FirFile -> TargetLists.T_FILE
        is FirTypeParameter -> TargetLists.T_TYPE_PARAMETER
        is FirAnonymousInitializer -> TargetLists.T_INITIALIZER
        is FirAnonymousObject ->
            if (annotated.source?.kind == KtFakeSourceElementKind.EnumInitializer) {
                AnnotationTargetList(
                    KotlinTarget.classActualTargets(
                        ClassKind.ENUM_ENTRY,
                        isInnerClass = false,
                        isCompanionObject = false,
                        isLocalClass = false
                    )
                )
            } else {
                TargetLists.T_OBJECT_LITERAL
            }
//            TODO: properly implement those cases
//            is KtDestructuringDeclarationEntry -> TargetLists.T_LOCAL_VARIABLE
//            is KtDestructuringDeclaration -> TargetLists.T_DESTRUCTURING_DECLARATION
//            is KtLambdaExpression -> TargetLists.T_FUNCTION_LITERAL
        else -> TargetLists.EMPTY
    }
}

private typealias TargetLists = AnnotationTargetLists

fun FirQualifiedAccessExpression.explicitReceiverIsNotSuperReference(): Boolean {
    return (this.explicitReceiver as? FirQualifiedAccessExpression)?.calleeReference !is FirSuperReference
}


internal val KtSourceElement.defaultValueForParameter: KtSourceElement?
    get() = when (this) {
        is KtPsiSourceElement -> (psi as? KtParameter)?.defaultValue?.toKtPsiSourceElement()
        is KtLightSourceElement -> findDefaultValue(this)
    }

private fun findDefaultValue(source: KtLightSourceElement): KtLightSourceElement? {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    source.treeStructure.getChildren(source.lighterASTNode, childrenRef)

    var defaultValue: LighterASTNode? = null
    var defaultValueOffset = source.startOffset

    for (node in childrenRef.get()) {
        if (node == null) continue
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

fun ConeKotlinType.getInlineClassUnderlyingType(session: FirSession): ConeKotlinType {
    require(this.isSingleFieldValueClass(session))
    return toRegularClassSymbol(session)!!.primaryConstructorSymbol()!!.valueParameterSymbols[0].resolvedReturnTypeRef.coneType
}

fun FirNamedFunctionSymbol.directOverriddenFunctions(session: FirSession, scopeSession: ScopeSession): List<FirNamedFunctionSymbol> {
    val classSymbol = getContainingClassSymbol(session) as? FirClassSymbol ?: return emptyList()
    val scope = classSymbol.unsubstitutedScope(session, scopeSession, withForcedTypeCalculator = false)

    scope.processFunctionsByName(name) { }
    return scope.getDirectOverriddenFunctions(this, true)
}

fun FirNamedFunctionSymbol.directOverriddenFunctions(context: CheckerContext) =
    directOverriddenFunctions(context.session, context.sessionHolder.scopeSession)

val CheckerContext.closestNonLocal get() = containingDeclarations.takeWhile { it.isNonLocal }.lastOrNull()

fun CheckerContext.closestNonLocalWith(declaration: FirDeclaration) =
    (containingDeclarations + declaration).takeWhile { it.isNonLocal }.lastOrNull()

val CheckerContext.isTopLevel get() = containingDeclarations.lastOrNull() is FirFile

fun FirBasedSymbol<*>.hasAnnotationOrInsideAnnotatedClass(classId: ClassId, session: FirSession): Boolean {
    if (hasAnnotation(classId, session)) return true
    val container = getContainingClassSymbol(session) ?: return false
    return container.hasAnnotationOrInsideAnnotatedClass(classId, session)
}

fun FirDeclaration.hasAnnotationOrInsideAnnotatedClass(classId: ClassId, session: FirSession) =
    symbol.hasAnnotationOrInsideAnnotatedClass(classId, session)

fun FirBasedSymbol<*>.getAnnotationStringParameter(classId: ClassId, session: FirSession): String? {
    val annotation = getAnnotationByClassId(classId, session) as? FirAnnotationCall
    val expression = annotation?.argumentMapping?.mapping?.values?.firstOrNull() as? FirConstExpression<*>
    return expression?.value as? String
}

fun FirElement.isLhsOfAssignment(context: CheckerContext): Boolean {
    if (this !is FirQualifiedAccessExpression) return false
    val lastQualified = context.qualifiedAccessOrAssignmentsOrAnnotationCalls.lastOrNull { it != this } ?: return false
    return lastQualified is FirVariableAssignment && lastQualified.lValue == this
}
