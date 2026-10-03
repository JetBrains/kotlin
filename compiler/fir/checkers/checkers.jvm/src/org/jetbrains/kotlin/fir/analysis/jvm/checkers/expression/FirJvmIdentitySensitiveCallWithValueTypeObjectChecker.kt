/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallableReferenceAccessChecker
import org.jetbrains.kotlin.fir.analysis.checkers.finalApproximationOrSelf
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors.SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.enableWarningsForIdentitySensitiveOperationsOnValueClassesAndPrimitives
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionTypeConversionExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.expressions.unwrapArgument
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.scope
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.scopes.CallableCopyTypeCalculator.DoNothing
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionIn
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.fir.types.withArguments
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

private val operationsToCheckFirstArgCallableIds = setOf(
    CallableId(FqName("java.lang"), FqName("System"), Name.identifier("identityHashCode")),
    CallableId(FqName("java.lang.ref"), FqName("Cleaner"), Name.identifier("register")),
    CallableId(FqName("java.lang.ref"), FqName("PhantomReference"), Name.identifier("PhantomReference")),
    CallableId(FqName("java.lang.ref"), FqName("SoftReference"), Name.identifier("SoftReference")),
    CallableId(FqName("java.lang.ref"), FqName("WeakReference"), Name.identifier("WeakReference")),
)

private val operationsToCheckFirstTypeArgCallableIds = setOf(
    CallableId(FqName("java.lang.ref"), FqName("ReferenceQueue"), Name.identifier("ReferenceQueue")),
    CallableId(FqName("java.util"), FqName("IdentityHashMap"), Name.identifier("IdentityHashMap")),
    CallableId(FqName("java.util"), FqName("WeakHashMap"), Name.identifier("WeakHashMap")),
)

object FirJvmIdentitySensitiveCallWithValueTypeObjectChecker : FirCallChecker(MppCheckerKind.Common) {
    private val synchronizedCallableId = CallableId(FqName("kotlin"), Name.identifier("synchronized"))
    private val lockParameterName = Name.identifier("lock")

    override val platformSpecificCheckerEnabledInMetadataCompilation: Boolean
        get() = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCall) {
        val function = when (expression) {
            is FirFunctionCall -> expression.calleeReference.toResolvedCallableSymbol()
            is FirDelegatedConstructorCall -> expression.calleeReference.toResolvedCallableSymbol()
            else -> null
        } ?: return
        when (function.callableId) {
            synchronizedCallableId -> if (expression is FirFunctionCall) checkSynchronizedCall(expression)

            in operationsToCheckFirstArgCallableIds -> {
                val argument = expression.arguments.firstOrNull() ?: return
                checkType(argument.resolvedType, argument.source)
            }

            in operationsToCheckFirstTypeArgCallableIds -> when (expression) {
                is FirFunctionCall -> {
                    // A type alias may have other type arguments than the class it expands to.
                    val type = expression.resolvedType.fullyExpandedType().typeArguments.firstOrNull()?.type ?: return
                    val typeArgument = expression.typeArguments.firstOrNull() as? FirTypeProjectionWithVariance
                    val isTypeAliasConstructor = (function as? FirConstructorSymbol)?.typeAliasConstructorInfo != null
                    val source = if (isTypeAliasConstructor) expression.calleeReference.source else typeArgument?.source
                    checkType(type.lowerBoundIfFlexible(), source ?: expression.calleeReference.source)
                }
                is FirDelegatedConstructorCall -> {
                    val typeRef = expression.constructedTypeRef
                    val type = typeRef.coneType.typeArguments.firstOrNull()?.type ?: return
                    val userTypeRef = (typeRef as? FirResolvedTypeRef)?.delegatedTypeRef as? FirUserTypeRef
                    val typeArgumentSource = userTypeRef?.qualifier?.lastOrNull()?.typeArgumentList?.typeArguments?.firstOrNull()?.source
                    checkType(type.lowerBoundIfFlexible(), typeArgumentSource ?: typeRef.source)
                }
                else -> {}
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSynchronizedCall(
        expression: FirFunctionCall,
    ) {
        for ([argument, parameter] in expression.resolvedArgumentMapping?.entries ?: return) {
            if (parameter.name != lockParameterName) continue
            val type = argument.resolvedType.finalApproximationOrSelf()
            if (type.isValueClassOrPrimitive()) {
                reporter.reportOn(argument.source, SYNCHRONIZED_BLOCK_ON_VALUE_CLASS_OR_PRIMITIVE, type)
            } else if (type.isJavaValueBasedClassAndWarningsEnabled()) {
                reporter.reportOn(argument.source, SYNCHRONIZED_BLOCK_ON_JAVA_VALUE_BASED_CLASS, type)
            } else if (enableWarningsForIdentitySensitiveOperationsOnValueClassesAndPrimitives() && type.isFlexiblePrimitive()) {
                reporter.reportOn(argument.source, IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE, type)
            }
        }
    }
}

// A reference to an identity-sensitive function, like `::WeakReference`, takes instances of the type of its value parameter. When the
// parameter type is not a value type, like that of `System::identityHashCode`, the function type the reference is used as gives it.
object FirJvmIdentitySensitiveCallableReferenceChecker : FirCallableReferenceAccessChecker(MppCheckerKind.Common) {
    override val platformSpecificCheckerEnabledInMetadataCompilation: Boolean
        get() = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCallableReferenceAccess) {
        val function = expression.calleeReference.toResolvedCallableSymbol() as? FirFunctionSymbol<*> ?: return
        when (function.callableId) {
            in operationsToCheckFirstArgCallableIds -> {
                // An unbound reference to a member takes the receiver first.
                val parameterTypes = expression.resolvedType.functionTypeParameters()
                val index = (parameterTypes.size - function.valueParameterSymbols.size).coerceAtLeast(0)
                val type = parameterTypes.getOrNull(index)?.takeIf { it.isIdentityChecked() }
                    ?: expression.expectedType()?.invokedParameterTypes()?.getOrNull(index)
                    ?: return
                checkType(type, expression.source)
            }
            in operationsToCheckFirstTypeArgCallableIds -> {
                val constructedType = expression.resolvedType.fullyExpandedType().typeArguments.lastOrNull()?.type ?: return
                val type = constructedType.fullyExpandedType().typeArguments.firstOrNull()?.type ?: return
                checkType(type.lowerBoundIfFlexible(), expression.source)
            }
        }
    }
}

context(context: CheckerContext)
private fun ConeKotlinType.functionTypeParameters(): List<ConeKotlinType> =
    fullyExpandedType().typeArguments.dropLast(1).map { it.type?.lowerBoundIfFlexible() ?: return emptyList() }

// The parameter types of the function type, or of the abstract method of the fun interface type, that a reference is used as.
context(context: CheckerContext)
private fun ConeKotlinType.invokedParameterTypes(): List<ConeKotlinType>? {
    val type = lowerBoundIfFlexible().fullyExpandedType()
    if (type.isSomeFunctionType(context.session)) return type.functionTypeParameters()
    if (type !is ConeClassLikeType || type.toRegularClassSymbol()?.classKind != ClassKind.INTERFACE) return null
    // The abstract method of `ToIntFunction<in T>` takes instances of `T`.
    val samType = type.withArguments(type.typeArguments.map { (it as? ConeKotlinTypeProjectionIn)?.type ?: it }.toTypedArray())
    val scope = samType.scope(DoNothing, FirResolvePhase.STATUS) ?: return null
    val abstractFunction = scope.getCallableNames().flatMap { scope.getFunctions(it) }
        .singleOrNull { it.isAbstract && it.name !in anyMemberNames } ?: return null
    val receiverTypes = listOfNotNull(abstractFunction.resolvedReceiverType)
    return (receiverTypes + abstractFunction.valueParameterSymbols.map { it.resolvedReturnType }).map { it.lowerBoundIfFlexible() }
}

private val anyMemberNames = setOf(OperatorNameConventions.EQUALS, OperatorNameConventions.HASH_CODE, OperatorNameConventions.TO_STRING)

// The type that a reference is used as, known from where it is: an argument, a variable or a returned value.
context(context: CheckerContext)
private fun FirCallableReferenceAccess.expectedType(): ConeKotlinType? {
    var child: FirElement = this
    for (parent in context.containingElements.asReversed().dropWhile { it !== this }.drop(1)) {
        when (parent) {
            is FirArgumentList -> continue
            is FirWrappedArgumentExpression, is FirWhenExpression, is FirElvisExpression -> {}
            is FirBlock -> if (parent.statements.lastOrNull() !== child) return null
            is FirWhenBranch -> if (parent.result !== child) return null
            is FirVarargArgumentsExpression -> return parent.coneElementTypeOrNull
            is FirFunctionTypeConversionExpression -> return parent.resolvedType
            is FirCall -> return parent.parameterTypeOf(child)
            is FirValueParameter -> return parent.returnTypeRef.coneType.takeIf { parent.defaultValue === child }
            is FirVariable -> return parent.returnTypeRef.coneType.takeIf { parent.initializer === child }
            is FirVariableAssignment -> return parent.lValue.resolvedType.takeIf { parent.rValue === child }
            is FirReturnExpression -> return parent.target.labeledElement.returnTypeRef.coneType
            else -> return null
        }
        child = parent
    }
    return null
}

context(context: CheckerContext)
private fun FirCall.parameterTypeOf(argument: FirElement): ConeKotlinType? {
    val unwrappedArgument = (argument as? FirExpression)?.unwrapArgument() ?: return null
    val [function, typeArguments] = when (this) {
        is FirFunctionCall -> calleeReference.toResolvedCallableSymbol() to
                typeArguments.map { (it as? FirTypeProjectionWithVariance)?.typeRef?.coneType ?: return null }
        is FirDelegatedConstructorCall -> calleeReference.toResolvedCallableSymbol() to
                constructedTypeRef.coneType.fullyExpandedType().typeArguments.map { it.type ?: return null }
        else -> return null
    }
    if (function == null) return null
    val parameter = resolvedArgumentMapping?.entries?.firstOrNull { it.key.unwrapArgument() === unwrappedArgument }?.value ?: return null
    return substitutorByMap(function.typeParameterSymbols.zip(typeArguments).toMap(), context.session)
        .substituteOrSelf(parameter.returnTypeRef.coneType)
}

context(context: CheckerContext)
private fun ConeKotlinType.isIdentityChecked(): Boolean =
    finalApproximationOrSelf().let { it.isValueObjectAtRuntime() || it.isValueTypeAndWarningsEnabled() }

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkType(type: ConeKotlinType, source: KtSourceElement?) {
    // A captured type, like that of an element of `Array<out V>`, is reported as `V`.
    val renderedType = type.finalApproximationOrSelf()
    if (renderedType.isValueObjectAtRuntime()) {
        reporter.reportOn(source, FirJvmErrors.IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_OBJECT, renderedType)
    } else if (renderedType.isValueTypeAndWarningsEnabled()) {
        reporter.reportOn(source, FirJvmErrors.IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE, renderedType)
    }
}
