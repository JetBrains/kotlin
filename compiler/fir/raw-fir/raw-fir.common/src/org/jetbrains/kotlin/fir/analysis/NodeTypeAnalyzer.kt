/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilder.ForbiddenLabelKind
import org.jetbrains.kotlin.fir.builder.DestructuringKind
import org.jetbrains.kotlin.fir.builder.escapedStringToCharacter
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirErrorPropertySymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.OperatorNameConventions

// May be merged with AbstractRawFirBuilder (was needed as a separate entity when it was an interface and used by delegation)
abstract class NodeTypeAnalyzer<Node : Any, Type : Any> {
    abstract val implicitType: FirImplicitTypeRef
    abstract val implicitUnitType: FirImplicitBuiltinTypeRef
    abstract val implicitAnyType: FirImplicitBuiltinTypeRef
    abstract val implicitEnumType: FirImplicitBuiltinTypeRef
    abstract val implicitAnnotationType: FirImplicitBuiltinTypeRef

    abstract fun Node.toFirSourceElement(kind: KtFakeSourceElementKind? = null): KtSourceElement
    abstract val Node.elementType: Type
    abstract val Node.asText: String

    abstract fun Node.isStringInterpolationPrefixOrQuote(): Boolean
    abstract fun Node.isLiteralStringTemplateEntry(): Boolean
    abstract fun Node.isEscapeStringTemplateEntry(): Boolean
    abstract fun Node.isShortOrLongStringTemplateEntry(): Boolean

    abstract fun Node.getLabelName(): String?
    abstract fun FirLoopJumpBuilder.bindLabel(expression: Node): FirLoopJumpBuilder

    fun buildLabel(rawName: String, source: KtSourceElement): FirLabel {
        val firLabel = org.jetbrains.kotlin.fir.builder.buildLabel {
            name = KtPsiUtil.unquoteIdentifier(rawName)
            this.source = source
        }

        return firLabel
    }

    abstract fun callableIdForName(name: Name): CallableId
    abstract fun callableIdForClassConstructor(): CallableId

    abstract fun destructuringKindOf(hasSquareBrackets: Boolean, isFullForm: Boolean): DestructuringKind
    abstract fun registerSelfType(selfType: FirResolvedTypeRef)

    fun Node.toDelegatedSelfType(firClass: FirRegularClassBuilder): FirResolvedTypeRef =
        toDelegatedSelfType(firClass.typeParameters, firClass.symbol)

    fun Node.toDelegatedSelfType(firObject: FirAnonymousObjectBuilder): FirResolvedTypeRef =
        toDelegatedSelfType(firObject.typeParameters, firObject.symbol)

    fun Node.toDelegatedSelfType(typeParameters: List<FirTypeParameterRef>, symbol: FirClassLikeSymbol<*>): FirResolvedTypeRef {
        return buildResolvedTypeRef {
            source = this@toDelegatedSelfType.toFirSourceElement(KtFakeSourceElementKind.ClassSelfTypeRef)
            coneType = ConeClassLikeTypeImpl(
                symbol.toLookupTag(),
                typeParameters.map { ConeTypeParameterType(it.symbol.toLookupTag(), false) }.toTypedArray(),
                false
            )
        }
    }

    fun isImplicitlyActual(status: FirDeclarationStatus, classKind: ClassKind): Boolean {
        return status.isActual && (status.isInline || status.isValue || classKind == ClassKind.ANNOTATION_CLASS)
    }

    fun configureScriptDestructuringDeclarationEntry(declaration: FirVariable, container: FirVariable) {
        (declaration as FirProperty).destructuringDeclarationContainerVariable = container.symbol
    }

    fun createNoTypeForParameterTypeRef(parameterSource: KtSourceElement): FirErrorTypeRef {
        return buildErrorTypeRef {
            source = parameterSource
            diagnostic = ConeSimpleDiagnostic("No type for parameter", DiagnosticKind.ValueParameterWithNoTypeAnnotation)
        }
    }

    fun convertValueParameterName(
        safeName: Name,
        valueParameterDeclaration: ValueParameterDeclaration,
        rawName: () -> String?,
    ): Name {
        return when (valueParameterDeclaration) {
            ValueParameterDeclaration.LAMBDA if (rawName() == "_")
                -> SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
            ValueParameterDeclaration.CATCH, ValueParameterDeclaration.CONTEXT_PARAMETER
                -> if (safeName.asString() == "_") SpecialNames.UNDERSCORE_FOR_UNUSED_VAR else safeName
            else -> safeName
        }
    }

    enum class ValueParameterDeclaration(val shouldExplicitParameterTypeBePresent: Boolean, val isAnnotationOwner: Boolean) {
        FUNCTION(shouldExplicitParameterTypeBePresent = true, isAnnotationOwner = true),
        CATCH(shouldExplicitParameterTypeBePresent = true, isAnnotationOwner = false),
        PRIMARY_CONSTRUCTOR(shouldExplicitParameterTypeBePresent = true, isAnnotationOwner = false),
        SETTER(shouldExplicitParameterTypeBePresent = false, isAnnotationOwner = false),
        LAMBDA(shouldExplicitParameterTypeBePresent = false, isAnnotationOwner = false),
        FOR_LOOP(shouldExplicitParameterTypeBePresent = false, isAnnotationOwner = false),
        CONTEXT_PARAMETER(shouldExplicitParameterTypeBePresent = true, isAnnotationOwner = true),
    }

    abstract fun convertScriptOrSnippets(declaration: Node, sourceFile: KtSourceFile, fileBuilder: FirFileBuilder?): FirDeclaration

    abstract val Node?.receiverExpression: Node?
    abstract val Node?.selectorExpression: Node?
    abstract val Node?.indexExpressions: List<Node>?
    abstract fun KtSourceElement.isChildInParentheses(): Boolean

    abstract fun FirQualifiedAccessExpression.pullUpSafeCallIfNecessary(): FirExpression

    abstract fun FirCallableDeclaration.initContainingClassAttr()
    abstract fun FirClassLikeDeclaration.initContainingClassForLocalAttr()
    abstract fun FirRegularClass.initContainingScriptOrReplAttr()
    fun FirRegularClassBuilder.initCompanionObjectSymbolAttr() {
        companionObjectSymbol = (declarations.firstOrNull { it is FirRegularClass && it.isCompanion } as FirRegularClass?)?.symbol
    }

    fun constructorTypeParametersFromConstructedClass(ownerTypeParameters: List<FirTypeParameterRef>): List<FirTypeParameterRef> {
        return ownerTypeParameters.mapNotNull {
            val declaredTypeParameter = (it as? FirTypeParameter) ?: return@mapNotNull null
            buildConstructedClassTypeParameterRef {
                source = declaredTypeParameter.symbol.source?.fakeElement(KtFakeSourceElementKind.ConstructorTypeParameter)
                symbol = declaredTypeParameter.symbol
            }
        }
    }

    fun createErrorConstructorBuilder(diagnostic: ConeDiagnostic): FirErrorPrimaryConstructorBuilder =
        FirErrorPrimaryConstructorBuilder().apply { this.diagnostic = diagnostic }

    fun buildErrorNonLocalDestructuringDeclaration(
        source: KtSourceElement,
        initializer: FirExpression?,
        baseModuleData: FirModuleData,
    ): FirErrorProperty = buildErrorProperty {
        this.source = source
        moduleData = baseModuleData
        origin = FirDeclarationOrigin.Source
        name = Name.special("<destructuring>")
        diagnostic = ConeDestructuringDeclarationsOnTopLevel
        symbol = FirErrorPropertySymbol(diagnostic)
        this.initializer = initializer ?: buildErrorExpression {
            this.source = source
            diagnostic = ConeSyntaxDiagnostic("Initializer required for destructuring declaration")
        }
    }

    fun buildExpressionHandlingLabelErrors(
        element: FirElement?,
        elementSource: KtSourceElement,
        forbiddenLabelKind: ForbiddenLabelKind?,
        forbiddenLabelSource: KtSourceElement?,
    ): FirElement {
        if (element == null) return buildErrorExpression(
            elementSource,
            ConeSyntaxDiagnostic("Empty label")
        )
        if (forbiddenLabelKind == null) return element

        require(forbiddenLabelSource != null)
        return buildErrorExpression {
            this.source = element.source
            this.expression = element as? FirExpression
            this.nonExpressionElement = element.takeUnless { it is FirExpression }
            diagnostic = when (forbiddenLabelKind) {
                ForbiddenLabelKind.UNDERSCORE_IS_RESERVED -> ConeUnderscoreIsReserved(forbiddenLabelSource)
                ForbiddenLabelKind.MULTIPLE_LABEL -> ConeMultipleLabelsAreForbidden(forbiddenLabelSource)
            }
        }
    }

    fun convertUnaryPlusMinusCallOnIntegerLiteralIfNecessary(
        source: Node,
        receiver: FirExpression,
        operationName: Name?,
    ): FirExpression? {
        if (receiver !is FirLiteralExpression) return null
        if (receiver.kind != ConstantValueKind.IntegerLiteral) return null

        val convertedValue = when (operationName) {
            OperatorNameConventions.UNARY_MINUS -> -(receiver.value as Long)
            OperatorNameConventions.UNARY_PLUS -> receiver.value as Long
            else -> return null
        }

        return buildLiteralExpression(
            source.toFirSourceElement(),
            ConstantValueKind.IntegerLiteral,
            convertedValue,
            setType = false
        )
    }

    fun convertFirSelector(
        firSelector: FirQualifiedAccessExpression,
        source: KtSourceElement?,
        receiver: FirExpression,
    ): FirQualifiedAccessExpression {
        return if (firSelector is FirImplicitInvokeCall) {
            buildImplicitInvokeCall {
                this.source = source
                annotations.addAll(firSelector.annotations)
                typeArguments.addAll(firSelector.typeArguments)
                explicitReceiver = firSelector.explicitReceiver
                argumentList = buildArgumentList {
                    arguments.add(receiver)
                    arguments.addAll(firSelector.arguments)
                }
                isCallWithExplicitReceiver = true
                calleeReference = firSelector.calleeReference
            }
        } else {
            firSelector.replaceExplicitReceiver(receiver)
            @OptIn(FirImplementationDetail::class)
            firSelector.replaceSource(source)
            firSelector
        }
    }

    fun List<Node?>.toInterpolatingCall(
        base: Node,
        convertTemplateEntry: Node?.(String) -> Collection<FirExpression>,
        prefix: () -> String,
    ): FirExpression {
        return buildStringConcatenationCall {
            val sb = StringBuilder()
            var hasExpressions = false
            argumentList = buildArgumentList {
                L@ for (entry in this@toInterpolatingCall) {
                    if (entry == null) continue
                    when {
                        entry.isStringInterpolationPrefixOrQuote() -> continue@L
                        entry.isLiteralStringTemplateEntry() -> {
                            sb.append(entry.asText)
                            arguments += buildLiteralExpression(
                                entry.toFirSourceElement(), ConstantValueKind.String, entry.asText, setType = false
                            )
                        }
                        entry.isEscapeStringTemplateEntry() -> {
                            val entryText = entry.asText
                            val characterWithDiagnostic = escapedStringToCharacter(entryText)
                            val unescapedCharacter = characterWithDiagnostic.value
                            if (unescapedCharacter != null) {
                                sb.append(unescapedCharacter)
                            }

                            arguments += buildConstOrErrorExpression(
                                entry.toFirSourceElement(),
                                ConstantValueKind.String,
                                unescapedCharacter?.toString(),
                                "character",
                                entryText,
                                characterWithDiagnostic.getDiagnostic() ?: DiagnosticKind.IllegalConstExpression
                            )
                        }
                        entry.isShortOrLongStringTemplateEntry() -> {
                            hasExpressions = true
                            val expressions = entry.convertTemplateEntry("Incorrect template argument")
                            if (expressions.isNotEmpty()) {
                                arguments += expressions
                            } else {
                                arguments += buildErrorExpression {
                                    source = entry.toFirSourceElement()
                                    diagnostic = ConeSyntaxDiagnostic("Incorrect template argument")
                                }
                            }
                        }
                        else -> {
                            hasExpressions = true
                            arguments += buildErrorExpression {
                                source = entry.toFirSourceElement()
                                diagnostic = ConeSyntaxDiagnostic("Incorrect template entry: ${entry.asText}")
                            }
                        }
                    }
                }
            }
            source = base.toFirSourceElement()
            interpolationPrefix = prefix()
            // Fast-pass if there is no errors and non-const string expressions
            if (!hasExpressions && !argumentList.arguments.any { it is FirErrorExpression })
                return buildLiteralExpression(
                    source,
                    ConstantValueKind.String,
                    sb.toString(),
                    setType = false,
                    prefix = interpolationPrefix.takeIf { it.isNotEmpty() }
                )
        }
    }

    abstract fun dispatchReceiverForInnerClassConstructor(): ConeClassLikeType?
    abstract fun FirLoopBuilder.prepareTarget(firLabelUser: Any): FirLoopTarget
    abstract fun FirLoopBuilder.configure(target: FirLoopTarget, generateBlock: () -> FirBlock): FirLoop

    abstract fun FirExpression.toReturn(
        baseSource: KtSourceElement? = source,
        labelName: String? = null,
        fromKtReturnExpression: Boolean = false,
    ): FirReturnExpression

    abstract fun Node?.generateAssignment(
        baseSource: KtSourceElement,
        arrayAccessSource: KtSourceElement?,
        rhsExpression: FirExpression,
        operation: FirOperation,
        annotations: List<FirAnnotation>,
        // Effectively `value = rhs?.convert()`, but at generateIndexedAccessAugmentedAssignment we need to recreate FIR for rhs
        // since there should be different nodes for desugaring as `.set(.., get().plus($rhs1))` and `.get(...).plusAssign($rhs2)`
        // Once KT-50861 is fixed, those two parameters shall be eliminated
        rhsAST: Node?,
        isLhsParenthesized: Boolean,
        convert: Node.() -> FirExpression,
    ): FirStatement

    abstract fun generateIncrementOrDecrementBlock(
        // Used to get source-element or text
        wholeExpression: Node,
        operationReference: Node?,
        receiver: Node?,
        callName: Name,
        prefix: Boolean,
        convert: Node.() -> FirExpression,
    ): FirExpression

    abstract fun Type.toConstantValueKind(): ConstantValueKind?
}
