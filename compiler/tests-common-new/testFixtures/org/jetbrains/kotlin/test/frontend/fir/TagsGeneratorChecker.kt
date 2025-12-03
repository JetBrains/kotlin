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
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.description.ConeCallsEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeConditionalEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeConditionalReturnsDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeHoldsInEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeReturnsEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasExplicitBackingField
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.frontend.fir.TagsGeneratorChecker.FirTags
import org.jetbrains.kotlin.test.frontend.fir.TagsGeneratorChecker.FirTags.MAX_LINE_LENGTH
import org.jetbrains.kotlin.test.frontend.fir.TagsGeneratorChecker.FirTags.TAG_PREFIX
import org.jetbrains.kotlin.test.frontend.fir.TagsGeneratorChecker.FirTags.TAG_SUFFIX
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirAnalysisHandler
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.*
import org.jetbrains.kotlin.test.utils.FirIdenticalCheckerHelper.Companion.isTeamCityBuild
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.Variance
import java.io.File

class TagsGeneratorChecker(testServices: TestServices) : FirAnalysisHandler(testServices) {
    private val tagsFromAllModules: MutableSet<String> = mutableSetOf()
    private val testDataFiles: List<File> by lazy {
        testServices.moduleStructure.originalTestDataFiles.first().let { originalFile ->
            listOf(
                originalFile.originalTestDataFile,
                originalFile.firTestDataFile,
                originalFile.llFirTestDataFile,
                originalFile.reversedTestDataFile,
                originalFile.latestLVTestDataFile
            ).filter { it.exists() }
        }
    }

    private val shouldSkip: Boolean by lazy { testDataFiles.any { it.readText().contains(TAG_PREFIX) } }

    override fun processModule(module: TestModule, info: FirOutputArtifact) {
        if (FirDiagnosticsDirectives.DISABLE_GENERATED_FIR_TAGS in module.directives || shouldSkip) return
        for (file in info.allFirFilesByTestFile.values) {
            val session = file.moduleData.session
            val visitor = TagsCollectorVisitor(session)
            file.accept(visitor)
            tagsFromAllModules += visitor.tags
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (shouldSkip) return
        if (tagsFromAllModules.isEmpty()) return
        for (testDataFile in testDataFiles) {
            val testDataFileContent = testDataFile.readText()
            if (testDataFileContent.contains(TAG_PREFIX)) return

            if (isTeamCityBuild) {
                testServices.assertions.fail {
                    """GENERATED_FIR_TAGS are missing for file ${testDataFile.name}.
                        |Please rerun the test locally to generate tags.""".trimMargin()
                }
            }

            val wrappedTagComment = formatTagsAsMultilineComment()

            testDataFile.writer().use {
                it.append(testDataFileContent.trim())
                it.append("\n\n")
                it.appendLine(wrappedTagComment)
            }
        }
    }

    private fun formatTagsAsMultilineComment(): String {
        val result = StringBuilder(TAG_PREFIX)
        var lineLength = TAG_PREFIX.length

        for (tag in tagsFromAllModules.sorted()) {
            val toAppend = if (result.endsWith(": ") || result.endsWith("\n")) tag else ", $tag"
            if (lineLength + toAppend.length > MAX_LINE_LENGTH) {
                result.appendLine(",")
                result.append(tag)
                lineLength = tag.length
            } else {
                result.append(toAppend)
                lineLength += toAppend.length
            }
        }

        result.append(TAG_SUFFIX)
        return result.toString()
    }

    object FirTags {
        const val TAG_PREFIX = "/* GENERATED_FIR_TAGS: "
        const val TAG_SUFFIX = " */"
        const val MAX_LINE_LENGTH = 120
        const val FUNCTION = "functionDeclaration"
        const val PROPERTY = "propertyDeclaration"
        const val CLASS = "classDeclaration"
        const val TYPEALIAS = "typeAliasDeclaration"
        const val TYPEALIAS_WITH_TYPE_PARAMETER = "typeAliasDeclarationWithTypeParameter"
        const val OBJECT = "objectDeclaration"
        const val INTERFACE = "interfaceDeclaration"
        const val ANNOTATION_CLASS = "annotationDeclaration"
        const val ENUM_CLASS = "enumDeclaration"
        const val ENUM_ENTRY = "enumEntry"
        const val SEALED = "sealed"
        const val EXPECT = "expect"
        const val ACTUAL = "actual"
        const val VALUE = "value"
        const val INNER = "inner"
        const val DATA = "data"
        const val TAILREC = "tailrec"
        const val OPERATOR = "operator"
        const val INFIX = "infix"
        const val INLINE = "inline"
        const val EXTERNAL = "external"
        const val SUSPEND = "suspend"
        const val CONST = "const"
        const val LATEINIT = "lateinit"
        const val OVERRIDE = "override"
        const val COMPANION = "companionObject"
        const val VARARG = "vararg"
        const val NOINLINE = "noinline"
        const val CROSSINLINE = "crossinline"
        const val REIFIED = "reified"
        const val OUT = "out"
        const val IN = "in"
        const val PRIMARY_CONSTRUCTOR = "primaryConstructor"
        const val SECONDARY_CONSTRUCTOR = "secondaryConstructor"
        const val INIT_BLOCK = "init"
        const val PROPERTY_DELEGATE = "propertyDelegate"
        const val DESTRUCTURING_DECLARATION = "destructuringDeclaration"
        const val INHERITANCE_DELEGATION = "inheritanceDelegation"
        const val FUN_WITH_EXTENSION_RECEIVER = "funWithExtensionReceiver"
        const val PROPERTY_WITH_EXTENSION_RECEIVER = "propertyWithExtensionReceiver"
        const val IN_PROJECTION = "inProjection"
        const val OUT_PROJECTION = "outProjection"
        const val STAR_PROJECTION = "starProjection"
        const val GETTER = "getter"
        const val SETTER = "setter"
        const val FUNCTIONAL_TYPE = "functionalType"
        const val NULLABLE_TYPE = "nullableType"
        const val DNN_TYPE = "dnnType"
        const val WHILE_LOOP = "whileLoop"
        const val DO_WHILE_LOOP = "doWhileLoop"
        const val FOR_LOOP = "forLoop"
        const val BREAK = "break"
        const val CONTINUE = "continue"
        const val ASSIGNMENT = "assignment"
        const val TYPE_PARAMETER = "typeParameter"
        const val TYPE_CONSTRAINT = "typeConstraint"
        const val COMPARISON = "comparisonExpression"
        const val ANONYMOUS_OBJECT = "anonymousObjectExpression"
        const val ANONYMOUS_FUNCTION = "anonymousFunction"
        const val LAMBDA_LITERAL = "lambdaLiteral"
        const val ANNOTATION_USE_SITE_TARGET_FIELD = "annotationUseSiteTargetField"
        const val ANNOTATION_USE_SITE_TARGET_ALL = "annotationUseSiteTargetAll"
        const val ANNOTATION_USE_SITE_TARGET_FILE = "annotationUseSiteTargetFile"
        const val ANNOTATION_USE_SITE_TARGET_PROPERTY = "annotationUseSiteTargetProperty"
        const val ANNOTATION_USE_SITE_TARGET_PROPERTY_GETTER = "annotationUseSiteTargetPropertyGetter"
        const val ANNOTATION_USE_SITE_TARGET_PROPERTY_SETTER = "annotationUseSiteTargetPropertySetter"
        const val ANNOTATION_USE_SITE_TARGET_SETTER_PARAMETER = "annotationUseSiteTargetSetterParameter"
        const val ANNOTATION_USE_SITE_TARGET_RECEIVER = "annotationUseSiteTargetReceiver"
        const val ANNOTATION_USE_SITE_TARGET_PARAM = "annotationUseSiteTargetParam"
        const val ANNOTATION_USE_SITE_TARGET_FIELD_DELEGATE = "annotationUseSiteTargetFieldDelegate"
        const val TRY_EXPRESSION = "tryExpression"
        const val ELVIS_EXPRESSION = "elvisExpression"
        const val SUPER_EXPRESSION = "superExpression"
        const val THIS_EXPRESSION = "thisExpression"
        const val WHEN_EXPRESSION = "whenExpression"
        const val AND_EXPRESSION = "andExpression"
        const val OR_EXPRESSION = "disjunctionExpression"
        const val EQUALITY_EXPRESSION = "equalityExpression"
        const val RANGE_EXPRESSION = "rangeExpression"
        const val AS_EXPRESSION = "asExpression"
        const val IS_EXPRESSION = "isExpression"
        const val STRING_LITERAL = "stringLiteral"
        const val UNSIGNED_LITERAL = "unsignedLiteral"
        const val MULTILINE_STRING_LITERAL = "multilineStringLiteral"
        const val INTEGER_LITERAL = "integerLiteral"
        const val GUARD_CONDITION = "guardCondition"
        const val CALLABLE_REFERENCE = "callableReference"
        const val COLLECTION_LITERAL = "collectionLiteral"
        const val IF_EXPRESSION = "ifExpression"
        const val FUNCTION_WITH_CONTEXT = "functionDeclarationWithContext"
        const val PROPERTY_WITH_CONTEXT = "propertyDeclarationWithContext"
        const val FUNCTIONAL_TYPE_WITH_CONTEXT = "typeWithContext"
        const val FUNCTIONAL_TYPE_WITH_EXTENSION = "typeWithExtension"
        const val FUN_INTERFACE = "funInterface"
        const val SAM_CONVERSION = "samConversion"
        const val SMARTCAST = "smartcast"
        const val SAFE_CALL = "safeCall"
        const val LOCAL_CLASS = "localClass"
        const val LOCAL_FUNCTION = "localFunction"
        const val LOCAL_PROPERTY = "localProperty"
        const val FLEXIBLE_TYPE = "flexibleType"
        const val CAPTURED_TYPE = "capturedType"
        const val INTERSECTION_TYPE = "intersectionType"
        const val WHEN_WITH_SUBJECT = "whenWithSubject"
        const val UNNAMED_LOCAL_VARIABLE = "unnamedLocalVariable"
        const val CLASS_REFERENCE = "classReference"
        const val ADDITIVE_EXPRESSION = "additiveExpression"
        const val MULTIPLICATIVE_EXPRESSION = "multiplicativeExpression"
        const val UNARY_EXPRESSION = "unaryExpression"
        const val INCREMENT_DECREMENT_EXPRESSION = "incrementDecrementExpression"
        const val JAVA_FUNCTION = "javaFunction"
        const val JAVA_PROPERTY = "javaProperty"
        const val JAVA_TYPE = "javaType"
        const val JAVA_CALLABLE_REFERENCE = "javaCallableReference"
        const val CHECK_NOT_NULL_CALL = "checkNotNullCall"
        const val NESTED_CLASS = "nestedClass"
        const val CONTRACTS = "contracts"
        const val CONTRACT_RETURNS_EFFECT = "contractReturnsEffect"
        const val CONTRACT_CALLS_EFFECT = "contractCallsEffect"
        const val CONTRACT_CONDITIONAL_EFFECT = "contractConditionalEffect"
        const val CONTRACT_HOLDSIN_EFFECT = "contractHoldsInEffect"
        const val CONTRACT_IMPLIES_RETURN_EFFECT = "contractImpliesReturnEffect"
        const val EXPLICIT_BACKING_FIELD = "explicitBackingField"
        const val test = "testTest"
    }
}

@OptIn(UnresolvedExpressionTypeAccess::class)
private class TagsCollectorVisitor(private val session: FirSession) : FirVisitorVoid() {
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

        if (regularClass.symbol.isLocal) tags += FirTags.LOCAL_CLASS

        val containingSymbol = regularClass.getContainingClassSymbol()
        if (containingSymbol != null && !regularClass.status.isCompanion && !regularClass.status.isInner) {
            tags += FirTags.NESTED_CLASS
        }

        checkRegularClassStatus(regularClass.status)
    }

    override fun visitNamedFunction(namedFunction: FirNamedFunction) {
        if (skipSyntheticDeclaration(namedFunction.source)) return
        visitElement(namedFunction)
        tags += FirTags.FUNCTION

        if (namedFunction.receiverParameter != null) tags += FirTags.FUN_WITH_EXTENSION_RECEIVER
        if (namedFunction.contextParameters.isNotEmpty()) tags += FirTags.FUNCTION_WITH_CONTEXT
        if (namedFunction.symbol.rawStatus.visibility == Visibilities.Local) tags += FirTags.LOCAL_FUNCTION

        checkSimpleFunctionStatus(namedFunction.status)
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry) {
        visitElement(enumEntry)
        tags += FirTags.ENUM_ENTRY
    }

    override fun visitProperty(property: FirProperty) {
        if (skipSyntheticDeclaration(property.source)) return
        visitElement(property)
        tags += FirTags.PROPERTY

        if (property.delegateFieldSymbol != null) tags += FirTags.PROPERTY_DELEGATE
        if (property.source?.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION) tags += FirTags.DESTRUCTURING_DECLARATION
        if (property.receiverParameter != null) tags += FirTags.PROPERTY_WITH_EXTENSION_RECEIVER
        if (property.getter?.symbol?.isDefault == false && property.getter?.source?.kind != KtFakeSourceElementKind.DelegatedPropertyAccessor)
            tags += FirTags.GETTER
        if (property.setter?.symbol?.isDefault == false) tags += FirTags.SETTER
        if (property.contextParameters.isNotEmpty()) tags += FirTags.PROPERTY_WITH_CONTEXT
        if (property.symbol is FirLocalPropertySymbol) tags += FirTags.LOCAL_PROPERTY
        if (property.name == SpecialNames.UNDERSCORE_FOR_UNUSED_VAR) tags += FirTags.UNNAMED_LOCAL_VARIABLE
        if (property.hasExplicitBackingField) tags += FirTags.EXPLICIT_BACKING_FIELD
        checkPropertyStatus(property.status)
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias) {
        visitElement(typeAlias)
        tags += FirTags.TYPEALIAS
        if (typeAlias.status.isActual) tags += FirTags.ACTUAL
        if (typeAlias.typeParameters.isNotEmpty()) tags += FirTags.TYPEALIAS_WITH_TYPE_PARAMETER
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
        if (skipSyntheticDeclaration(delegatedTypeRef?.source)) return
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

        tags += if (whileLoop.source?.elementType == KtNodeTypes.FOR) FirTags.FOR_LOOP
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
            LogicOperationKind.AND -> FirTags.AND_EXPRESSION
            LogicOperationKind.OR -> FirTags.OR_EXPRESSION
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

        if (literalExpression.kind == ConstantValueKind.UnsignedByte ||
            literalExpression.kind == ConstantValueKind.UnsignedShort ||
            literalExpression.kind == ConstantValueKind.UnsignedInt ||
            literalExpression.kind == ConstantValueKind.UnsignedLong ||
            literalExpression.kind == ConstantValueKind.UnsignedIntegerLiteral
        ) tags += FirTags.UNSIGNED_LITERAL

        if (literalExpression.kind == ConstantValueKind.Int) tags += FirTags.INTEGER_LITERAL
    }

    override fun visitWhenBranch(whenBranch: FirWhenBranch) {
        visitElement(whenBranch)
        if (whenBranch.hasGuard) tags += FirTags.GUARD_CONDITION
    }

    override fun visitResolvedCallableReference(resolvedCallableReference: FirResolvedCallableReference) {
        visitElement(resolvedCallableReference)

        val declarationOrigin = resolvedCallableReference.resolvedSymbol.origin
        val source = resolvedCallableReference.source
        if (isDeclarationOriginJava(declarationOrigin, source)) {
            tags += FirTags.JAVA_CALLABLE_REFERENCE
            return
        }

        tags += FirTags.CALLABLE_REFERENCE
    }

    override fun visitCollectionLiteral(collectionLiteral: FirCollectionLiteral) {
        visitElement(collectionLiteral)
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

        for (argument in functionCall.arguments) {
            if (skipSyntheticDeclaration(argument.source)) continue
            checkConeType(argument.coneTypeOrNull)
        }
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

    override fun visitField(field: FirField) {
        visitElement(field)
        if (field.origin == FirDeclarationOrigin.Synthetic.DelegateField) tags += FirTags.INHERITANCE_DELEGATION
    }

    override fun visitResolvedContractDescription(resolvedContractDescription: FirResolvedContractDescription) {
        visitElement(resolvedContractDescription)
        tags += FirTags.CONTRACTS
    }

    override fun visitEffectDeclaration(effectDeclaration: FirEffectDeclaration) {
        visitElement(effectDeclaration)
        when (effectDeclaration.effect) {
            is ConeConditionalEffectDeclaration -> tags += FirTags.CONTRACT_CONDITIONAL_EFFECT
            is ConeCallsEffectDeclaration -> tags += FirTags.CONTRACT_CALLS_EFFECT
            is ConeReturnsEffectDeclaration -> tags += FirTags.CONTRACT_RETURNS_EFFECT
            is ConeHoldsInEffectDeclaration -> tags += FirTags.CONTRACT_HOLDSIN_EFFECT
            is ConeConditionalReturnsDeclaration -> tags += FirTags.CONTRACT_IMPLIES_RETURN_EFFECT
        }
    }

    fun isDeclarationOriginJava(origin: FirDeclarationOrigin?, source: KtSourceElement?): Boolean {
        return when (origin) {
            is FirDeclarationOrigin.Java.Source -> true
            is FirDeclarationOrigin.Java.Library -> true
            is FirDeclarationOrigin.Synthetic.JavaProperty -> true
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
            is ConeDefinitelyNotNullType -> tags += FirTags.DNN_TYPE
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

    fun skipSyntheticDeclaration(source: KtSourceElement?): Boolean {
        return when (source?.kind) {
            KtFakeSourceElementKind.EnumGeneratedDeclaration -> true
            KtFakeSourceElementKind.DataClassGeneratedMembers -> true
            KtFakeSourceElementKind.DesugaredPrefixInc -> true
            KtFakeSourceElementKind.DesugaredPrefixDec -> true
            KtFakeSourceElementKind.DesugaredPostfixInc -> true
            KtFakeSourceElementKind.DesugaredPostfixDec -> true
            KtFakeSourceElementKind.DesugaredPrefixIncSecondGetReference -> true
            KtFakeSourceElementKind.DesugaredPrefixDecSecondGetReference -> true
            KtFakeSourceElementKind.ArrayAccessNameReference -> true
            KtFakeSourceElementKind.ArrayIndexExpressionReference -> true
            KtFakeSourceElementKind.ArrayTypeFromVarargParameter -> true
            KtFakeSourceElementKind.VarargArgument -> true
            KtFakeSourceElementKind.WhenGeneratedSubject -> true
            else -> false
        }
    }
}
