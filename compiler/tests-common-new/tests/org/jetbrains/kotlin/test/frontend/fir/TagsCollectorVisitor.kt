/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.expressions.FirArrayLiteral
import org.jetbrains.kotlin.fir.expressions.FirBooleanOperatorExpression
import org.jetbrains.kotlin.fir.expressions.FirBreakExpression
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirComparisonExpression
import org.jetbrains.kotlin.fir.expressions.FirContinueExpression
import org.jetbrains.kotlin.fir.expressions.FirDoWhileLoop
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCallOrigin
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirSamConversionExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.FirWhileLoop
import org.jetbrains.kotlin.fir.expressions.UnresolvedExpressionTypeAccess
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeCapturedType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeDefinitelyNotNullType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ProjectionKind
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.test.frontend.fir.TagsGeneratorChecker.FirTags
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.Variance

@OptIn(UnresolvedExpressionTypeAccess::class)
class TagsCollectorVisitor(private val session: FirSession) : FirVisitorVoid() {
    val tags = mutableSetOf<String>()

    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    override fun visitRegularClass(regularClass: FirRegularClass) {
        visitElement(regularClass)
        tags += when (regularClass.classKind) {
            ClassKind.ANNOTATION_CLASS -> FirTags.ANNOTATION_CLASS
            ClassKind.INTERFACE -> FirTags.INTERFACE
            ClassKind.ENUM_CLASS -> FirTags.ENUM_CLASS
            ClassKind.OBJECT -> FirTags.OBJECT
            else -> FirTags.CLASS
        }

        for (superTypeRef in regularClass.superTypeRefs) {
            val superSource = superTypeRef.source ?: continue
            val parent = superSource.treeStructure.getParent(superSource.lighterASTNode) ?: continue
            if (parent.tokenType == KtNodeTypes.DELEGATED_SUPER_TYPE_ENTRY) {
                tags += FirTags.INHERITANCE_DELEGATION
            }
        }

        if (regularClass.symbol.isLocal) tags += FirTags.LOCAL_CLASS

        val containingSymbol = regularClass.getContainingClassSymbol()
        if (containingSymbol is FirClassSymbol<*> && !regularClass.status.isCompanion && !regularClass.status.isInner) {
            tags += FirTags.NESTED_CLASS
        }

        checkRegularClassStatus(regularClass.status)
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
        if (simpleFunction.source?.kind is KtFakeSourceElementKind) return

        visitElement(simpleFunction)
        tags += FirTags.FUNCTION

        if (simpleFunction.receiverParameter != null) tags += FirTags.FUN_WITH_EXTENSION_RECEIVER
        if (simpleFunction.contextParameters.isNotEmpty()) tags += FirTags.FUNCTION_WITH_CONTEXT
        if (simpleFunction.symbol.isLocal) tags += FirTags.LOCAL_FUNCTION

        checkSimpleFunctionStatus(simpleFunction.status)
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry) {
        visitElement(enumEntry)
        tags += FirTags.ENUM_ENTRY
    }

    override fun visitProperty(property: FirProperty) {
        if (property.source?.kind is KtFakeSourceElementKind) return
        visitElement(property)
        tags += FirTags.PROPERTY

        if (property.delegateFieldSymbol != null) tags += FirTags.PROPERTY_DELEGATE
        if (property.source?.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION) tags += FirTags.DESTRUCTURING_DECLARATION
        if (property.receiverParameter != null) tags += FirTags.PROPERTY_WITH_EXTENSION_RECEIVER
        if (property.getter != null && !property.getter!!.symbol.isDefault) tags += FirTags.GETTER
        if (property.setter != null && !property.setter!!.symbol.isDefault) tags += FirTags.SETTER
        if (property.contextParameters.isNotEmpty()) tags += FirTags.PROPERTY_WITH_CONTEXT
        if (property.isLocal) tags += FirTags.LOCAL_PROPERTY
        if (property.name == SpecialNames.UNDERSCORE_FOR_UNUSED_VAR) tags += FirTags.UNNAMED_LOCAL_VARIABLE

        checkPropertyStatus(property.status)
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias) {
        visitElement(typeAlias)
        tags += FirTags.TYPEALIAS
        if (typeAlias.status.isActual) tags += FirTags.ACTUAL
    }

    override fun visitValueParameter(valueParameter: FirValueParameter) {
        visitElement(valueParameter)

        if (valueParameter.isCrossinline) tags += FirTags.CROSSINLINE
        if (valueParameter.isNoinline) tags += FirTags.NOINLINE
        if (valueParameter.isVararg) tags += FirTags.VARARG
    }

    override fun visitTypeParameter(typeParameter: FirTypeParameter) {
        visitElement(typeParameter)
        tags += FirTags.TYPE_PARAMETER

        if (typeParameter.isReified) tags += FirTags.REIFIED

        when (typeParameter.symbol.variance) {
            Variance.INVARIANT -> {}
            Variance.IN_VARIANCE -> tags += FirTags.IN
            Variance.OUT_VARIANCE -> tags += FirTags.OUT
        }

        if (typeParameter.bounds.any { it.source?.kind == KtRealSourceElementKind }) tags += FirTags.TYPE_CONSTRAINT
    }

    override fun visitConstructor(constructor: FirConstructor) {
        visitElement(constructor)
        if (constructor.source?.kind != KtFakeSourceElementKind.ImplicitConstructor) {
            tags += if (constructor.isPrimary) FirTags.PRIMARY_CONSTRUCTOR
            else FirTags.SECONDARY_CONSTRUCTOR
        }
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        visitElement(resolvedTypeRef)

        val delegatedTypeRef = resolvedTypeRef.delegatedTypeRef
        if (delegatedTypeRef is FirFunctionTypeRef) {
            delegatedTypeRef.accept(this)
            tags += FirTags.FUNCTIONAL_TYPE
        }
        checkConeType(resolvedTypeRef.coneType)
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer) {
        visitElement(anonymousInitializer)
        tags += FirTags.INIT_BLOCK
    }

    override fun visitFunctionTypeRef(functionTypeRef: FirFunctionTypeRef) {
        visitElement(functionTypeRef)

        if (functionTypeRef.isSuspend) tags += FirTags.SUSPEND
        if (functionTypeRef.receiverTypeRef != null) tags += FirTags.FUNCTIONAL_TYPE_WITH_EXTENSION
        if (functionTypeRef.contextParameterTypeRefs.isNotEmpty()) tags += FirTags.FUNCTIONAL_TYPE_WITH_CONTEXT
    }

    override fun visitWhileLoop(whileLoop: FirWhileLoop) {
        visitElement(whileLoop)

        tags += if (whileLoop.source?.kind == KtFakeSourceElementKind.DesugaredForLoop) FirTags.FOR_LOOP
        else FirTags.WHILE_LOOP
    }

    override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop) {
        visitElement(doWhileLoop)
        tags += FirTags.DO_WHILE_LOOP
    }

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment) {
        visitElement(variableAssignment)
        tags += FirTags.ASSIGNMENT
    }

    @OptIn(UnresolvedExpressionTypeAccess::class)
    override fun visitExpression(expression: FirExpression) {
        visitElement(expression)

        if (expression is FirUnitExpression && expression.source?.kind == KtFakeSourceElementKind.ImplicitUnit.IndexedAssignmentCoercion) {
            tags += FirTags.ASSIGNMENT
        }
        checkConeType(expression.coneTypeOrNull)
    }

    override fun visitBreakExpression(breakExpression: FirBreakExpression) {
        visitElement(breakExpression)
        tags += FirTags.BREAK
    }

    override fun visitContinueExpression(continueExpression: FirContinueExpression) {
        visitElement(continueExpression)
        tags += FirTags.CONTINUE
    }

    override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression) {
        visitElement(comparisonExpression)
        tags += FirTags.COMPARISON
    }

    override fun visitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression) {
        if (anonymousObjectExpression.source?.kind is KtFakeSourceElementKind) return
        visitElement(anonymousObjectExpression)
        tags += FirTags.ANONYMOUS_OBJECT
    }

    override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression) {
        visitElement(anonymousFunctionExpression)
        tags += if (anonymousFunctionExpression.anonymousFunction.isLambda) FirTags.LAMBDA_LITERAL
        else FirTags.ANONYMOUS_FUNCTION

        checkConeType(anonymousFunctionExpression.coneTypeOrNull)
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall) {
        visitElement(annotationCall)

        when (annotationCall.useSiteTarget) {
            AnnotationUseSiteTarget.ALL -> tags += FirTags.ANNOTATION_USE_SITE_TARGET_ALL
            AnnotationUseSiteTarget.FIELD -> tags += FirTags.ANNOTATION_USE_SITE_TARGET_FIELD
            AnnotationUseSiteTarget.FILE -> tags += FirTags.ANNOTATION_USE_SITE_TARGET_FILE
            AnnotationUseSiteTarget.PROPERTY -> tags += FirTags.ANNOTATION_USE_SITE_TARGET_PROPERTY
            AnnotationUseSiteTarget.PROPERTY_GETTER -> tags += FirTags.ANNOTATION_USE_SITE_TARGET_PROPERTY_GETTER
            AnnotationUseSiteTarget.PROPERTY_SETTER -> tags += FirTags.ANNOTATION_USE_SITE_TARGET_PROPERTY_SETTER
            AnnotationUseSiteTarget.RECEIVER -> tags += FirTags.ANNOTATION_USE_SITE_TARGET_RECEIVER
            AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER -> tags += FirTags.ANNOTATION_USE_SITE_TARGET_PARAM
            AnnotationUseSiteTarget.SETTER_PARAMETER -> tags += FirTags.ANNOTATION_USE_SITE_TARGET_SETTER_PARAMETER
            AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD -> tags += FirTags.ANNOTATION_USE_SITE_TARGET_FIELD_DELEGATE
            null -> {}
        }
    }

    override fun visitTryExpression(tryExpression: FirTryExpression) {
        visitElement(tryExpression)
        tags += FirTags.TRY_EXPRESSION

        checkConeType(tryExpression.coneTypeOrNull)
    }

    override fun visitElvisExpression(elvisExpression: FirElvisExpression) {
        visitExpression(elvisExpression)
        tags += FirTags.ELVIS_EXPRESSION
    }

    override fun visitSuperReference(superReference: FirSuperReference) {
        visitElement(superReference)
        tags += FirTags.SUPER_EXPRESSION
    }

    override fun visitThisReference(thisReference: FirThisReference) {
        visitElement(thisReference)
        if (!thisReference.isImplicit) tags += FirTags.THIS_EXPRESSION
    }

    override fun visitWhenExpression(whenExpression: FirWhenExpression) {
        visitElement(whenExpression)
        tags += if (whenExpression.source?.elementType == KtNodeTypes.IF) FirTags.IF_EXPRESSION
        else FirTags.WHEN_EXPRESSION
        if (whenExpression.subjectVariable != null) tags += FirTags.WHEN_WITH_SUBJECT
    }

    override fun visitBooleanOperatorExpression(booleanOperatorExpression: FirBooleanOperatorExpression) {
        visitElement(booleanOperatorExpression)
        tags += when (booleanOperatorExpression.kind) {
            LogicOperationKind.AND -> FirTags.CONJUNCTION_EXPRESSION
            LogicOperationKind.OR -> FirTags.DISJUNCTION_EXPRESSION
        }
    }

    override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall) {
        visitElement(equalityOperatorCall)
        tags += FirTags.EQUALITY_EXPRESSION
    }

    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall) {
        visitElement(typeOperatorCall)
        when (typeOperatorCall.operation) {
            FirOperation.IS, FirOperation.NOT_IS -> tags += FirTags.IS_EXPRESSION
            FirOperation.AS -> tags += FirTags.AS_EXPRESSION
            else -> {}
        }
    }

    override fun visitLiteralExpression(literalExpression: FirLiteralExpression) {
        visitElement(literalExpression)

        if (literalExpression.kind == ConstantValueKind.String) {
            tags += if (literalExpression.source?.lighterASTNode.toString().contains("\"\"\"")) FirTags.MULTILINE_STRING_LITERAL
            else FirTags.STRING_LITERAL
        }
    }

    override fun visitWhenBranch(whenBranch: FirWhenBranch) {
        visitElement(whenBranch)
        if (whenBranch.hasGuard) tags += FirTags.GUARD_CONDITION
    }

    override fun visitResolvedCallableReference(resolvedCallableReference: FirResolvedCallableReference) {
        visitElement(resolvedCallableReference)
        tags += FirTags.CALLABLE_REFERENCE
    }

    override fun visitArrayLiteral(arrayLiteral: FirArrayLiteral) {
        visitElement(arrayLiteral)
        tags += FirTags.COLLECTION_LITERAL
    }

    override fun visitSamConversionExpression(samConversionExpression: FirSamConversionExpression) {
        visitExpression(samConversionExpression)
        tags += FirTags.SAM_CONVERSION
    }

    override fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression) {
        visitExpression(smartCastExpression)
        tags += FirTags.SMARTCAST
    }

    override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression) {
        visitExpression(safeCallExpression)
        tags += FirTags.SAFE_CALL
    }

    override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
        visitElement(propertyAccessExpression)

        val declarationOrigin = propertyAccessExpression.calleeReference.symbol?.origin
        val source = propertyAccessExpression.source
        if (isDeclarationOriginJava(declarationOrigin, source)) tags += FirTags.JAVA_PROPERTY
        checkConeType(propertyAccessExpression.coneTypeOrNull)
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall) {
        visitElement(functionCall)

        for (argument in functionCall.arguments) checkConeType(argument.coneTypeOrNull)
        if (functionCall.origin == FirFunctionCallOrigin.Operator) checkOperatorName(functionCall.calleeReference.name.toString())

        val declarationOrigin = functionCall.calleeReference.symbol?.origin
        val source = functionCall.source
        if (isDeclarationOriginJava(declarationOrigin, source)) tags += FirTags.JAVA_FUNCTION
        checkConeType(functionCall.coneTypeOrNull)
    }

    override fun visitGetClassCall(getClassCall: FirGetClassCall) {
        visitElement(getClassCall)
        tags += FirTags.CLASS_REFERENCE
    }

    override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall) {
        visitElement(checkNotNullCall)
        tags += FirTags.CHECK_NOT_NULL_CALL
    }

    fun isDeclarationOriginJava(origin: FirDeclarationOrigin?, source: KtSourceElement?): Boolean {
        return when (origin) {
            is FirDeclarationOrigin.Java.Source -> true
            is FirDeclarationOrigin.Java.Library -> true
            FirDeclarationOrigin.Enhancement, FirDeclarationOrigin.RenamedForOverride -> when (source?.kind) {
                KtFakeSourceElementKind.EnumGeneratedDeclaration -> false
                else -> true
            }
            else -> false
        }
    }

    fun checkOperatorName(name: String) {
        when (name) {
            "plus", "minus" -> tags += FirTags.ADDITIVE_EXPRESSION
            "times", "div", "rem" -> tags += FirTags.MULTIPLICATIVE_EXPRESSION
            "rangeTo", "rangeUntil" -> tags += FirTags.RANGE_EXPRESSION
            "iterator", "next", "hasNext" -> tags += FirTags.PROGRESSION_EXPRESSION
            "unaryMinus", "unaryPlus" -> tags += FirTags.UNARY_EXPRESSION
            "inc", "dec" -> tags += FirTags.INCREMENT_DECREMENT_EXPRESSION
        }
    }

    fun checkConeType(coneKotlinType: ConeKotlinType?) {
        if (coneKotlinType == null) return
        when (coneKotlinType) {
            is ConeIntersectionType -> tags += FirTags.INTERSECTION_TYPE
            is ConeCapturedType -> tags += FirTags.CAPTURED_TYPE
            is ConeFlexibleType -> tags += FirTags.FLEXIBLE_TYPE
            is ConeDefinitelyNotNullType -> tags += FirTags.DEFINITELY_NOT_NULL
            else -> {}
        }
        for (typeArgument in coneKotlinType.typeArguments) {
            when (typeArgument.kind) {
                ProjectionKind.IN -> tags += FirTags.IN_PROJECTION
                ProjectionKind.OUT -> tags += FirTags.OUT_PROJECTION
                ProjectionKind.STAR -> tags += FirTags.STAR_PROJECTION
                ProjectionKind.INVARIANT -> {}
            }
            val innerConeType = (typeArgument as? ConeKotlinTypeProjection)?.type ?: continue
            val fakeTypeRef = buildResolvedTypeRef {
                source = null
                coneType = innerConeType
            }
            fakeTypeRef.accept(this)
        }
        if (coneKotlinType is ConeClassLikeType) {
            val symbol = coneKotlinType.lookupTag.toSymbol(session)
            if (symbol?.origin == FirDeclarationOrigin.Java.Source) tags += FirTags.JAVA_TYPE
        }
        if (coneKotlinType.isMarkedNullable) tags += FirTags.NULLABLE_TYPE
    }


    fun checkRegularClassStatus(status: FirDeclarationStatus) {
        if (status.modality == Modality.SEALED) tags += FirTags.SEALED
        if (status.isExpect) tags += FirTags.EXPECT
        if (status.isActual) tags += FirTags.ACTUAL
        if (status.isValue) tags += FirTags.VALUE
        if (status.isInner) tags += FirTags.INNER
        if (status.isData) tags += FirTags.DATA
        if (status.isCompanion) tags += FirTags.COMPANION
        if (status.isFun) tags += FirTags.FUN_INTERFACE
        if (status.isInline)  tags += FirTags.INLINE
    }

    fun checkSimpleFunctionStatus(status: FirDeclarationStatus) {
        if (status.isTailRec) tags += FirTags.TAILREC
        if (status.isOperator) tags += FirTags.OPERATOR
        if (status.isInfix) tags += FirTags.INFIX
        if (status.isInline) tags += FirTags.INLINE
        if (status.isExternal) tags += FirTags.EXTERNAL
        if (status.isSuspend) tags += FirTags.SUSPEND
        if (status.isActual) tags += FirTags.ACTUAL
        if (status.isExpect) tags += FirTags.EXPECT
        if (status.isOverride) tags += FirTags.OVERRIDE
    }

    fun checkPropertyStatus(status: FirDeclarationStatus) {
        if (status.isExpect) tags += FirTags.EXPECT
        if (status.isActual) tags += FirTags.ACTUAL
        if (status.isConst) tags += FirTags.CONST
        if (status.isLateInit) tags += FirTags.LATEINIT
        if (status.isOverride) tags += FirTags.OVERRIDE
    }
}