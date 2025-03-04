/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.builtins.functions.AllowedToUsedOnlyInK1
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKindExtractor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirPackageDirective
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeContractRenderer
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.expressions.impl.FirLazyDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.isCatchParameter
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import java.util.*

@OptIn(DirectDeclarationsAccess::class)
class FirRenderer(
    builder: StringBuilder = StringBuilder(),
    override val annotationRenderer: FirAnnotationRenderer? = FirAnnotationRenderer(),
    override val bodyRenderer: FirBodyRenderer? = FirBodyRenderer(),
    override val callArgumentsRenderer: FirCallArgumentsRenderer? = FirCallArgumentsRenderer(),
    override val classMemberRenderer: FirClassMemberRenderer? = FirClassMemberRenderer(),
    override val contractRenderer: ConeContractRenderer? = ConeContractRenderer(),
    override val declarationRenderer: FirDeclarationRenderer? = FirDeclarationRenderer(),
    override val idRenderer: ConeIdRenderer = ConeIdRendererForDebugging(),
    override val modifierRenderer: FirModifierRenderer? = FirAllModifierRenderer(),
    override val packageDirectiveRenderer: FirPackageDirectiveRenderer? = null,
    override val propertyAccessorRenderer: FirPropertyAccessorRenderer? = FirPropertyAccessorRenderer(),
    override val resolvePhaseRenderer: FirResolvePhaseRenderer? = null,
    override val typeRenderer: ConeTypeRenderer = ConeTypeRendererForDebugging(),
    override val referencedSymbolRenderer: FirSymbolRenderer = FirSymbolRenderer(),
    override val callableSignatureRenderer: FirCallableSignatureRenderer? = FirCallableSignatureRenderer(),
    override val errorExpressionRenderer: FirErrorExpressionRenderer? = FirErrorExpressionOnlyErrorRenderer(),
    override val resolvedNamedReferenceRenderer: FirResolvedNamedReferenceRenderer = FirResolvedNamedReferenceRendererWithLabel(),
    override val resolvedQualifierRenderer: FirResolvedQualifierRenderer = FirResolvedQualifierRendererWithLabel(),
    override val getClassCallRenderer: FirGetClassCallRenderer = FirGetClassCallRendererForDebugging(),
    override val supertypeRenderer: FirSupertypeRenderer? = FirSupertypeRenderer(),
    private val lineBreakAfterContextParameters: Boolean = true,
    private val renderFieldAnnotationSeparately: Boolean = true,
    private val renderVarargTypes: Boolean = false,
) : FirRendererComponents {

    override val visitor: Visitor = Visitor()
    override val printer: FirPrinter = FirPrinter(builder)

    companion object {
        fun noAnnotationBodiesAccessorAndArguments(): FirRenderer = FirRenderer(
            annotationRenderer = null,
            bodyRenderer = null,
            propertyAccessorRenderer = null,
            callArgumentsRenderer = FirCallNoArgumentsRenderer(),
            lineBreakAfterContextParameters = false,
        )

        fun withResolvePhase(): FirRenderer = FirRenderer(
            resolvePhaseRenderer = FirResolvePhaseRenderer(),
        )

        fun withDeclarationAttributes(): FirRenderer = FirRenderer(
            declarationRenderer = FirDeclarationRendererWithAttributes(),
        )

        fun forReadability(): FirRenderer = FirRenderer(
            typeRenderer = ConeTypeRenderer(),
            idRenderer = ConeIdShortRenderer(),
            classMemberRenderer = FirNoClassMemberRenderer(),
            bodyRenderer = null,
            propertyAccessorRenderer = null,
            callArgumentsRenderer = FirCallNoArgumentsRenderer(),
            modifierRenderer = FirPartialModifierRenderer(),
            callableSignatureRenderer = FirCallableSignatureRendererForReadability(),
            declarationRenderer = FirDeclarationRenderer("local "),
        )
    }

    init {
        annotationRenderer?.components = this
        bodyRenderer?.components = this
        callArgumentsRenderer?.components = this
        classMemberRenderer?.components = this
        contractRenderer?.components = this
        declarationRenderer?.components = this
        idRenderer.builder = builder
        modifierRenderer?.components = this
        packageDirectiveRenderer?.components = this
        propertyAccessorRenderer?.components = this
        resolvePhaseRenderer?.components = this
        typeRenderer.builder = builder
        typeRenderer.idRenderer = idRenderer
        referencedSymbolRenderer.components = this
        callableSignatureRenderer?.components = this
        errorExpressionRenderer?.components = this
        resolvedNamedReferenceRenderer.components = this
        resolvedQualifierRenderer.components = this
        getClassCallRenderer.components = this
        supertypeRenderer?.components = this
    }

    fun renderElementAsString(element: FirElement, trim: Boolean = false): String {
        element.accept(visitor)
        return printer.toString().applyIf(trim, String::trim)
    }

    fun renderElementWithTypeAsString(element: FirElement): String {
        print(element)
        print(": ")
        return renderElementAsString(element)
    }

    fun renderAsCallableDeclarationString(callableDeclaration: FirCallableDeclaration): String {
        visitor.visitCallableDeclaration(callableDeclaration)
        return printer.toString()
    }

    fun renderMemberDeclarationClass(firClass: FirClass) {
        visitor.visitMemberDeclaration(firClass)
    }

    fun renderAnnotations(annotationContainer: FirAnnotationContainer) {
        annotationRenderer?.render(annotationContainer)
    }

    private fun Variance.renderVariance() {
        label.let {
            print(it)
            if (it.isNotEmpty()) {
                print(" ")
            }
        }
    }

    private fun renderContexts(contextParameters: List<FirValueParameter>, lineBreakAfter: Boolean = lineBreakAfterContextParameters) {
        if (contextParameters.isEmpty()) return
        print("context(")
        renderSeparated(contextParameters, visitor)
        print(")")

        if (lineBreakAfter) {
            printer.newLine()
        } else {
            print(" ")
        }
    }

    private fun List<FirTypeParameterRef>.renderTypeParameters() {
        if (isNotEmpty()) {
            print("<")
            renderSeparated(this, visitor)
            print(">")
        }
    }

    private fun List<FirTypeProjection>.renderTypeArguments() {
        if (isNotEmpty()) {
            print("<")
            renderSeparated(this, visitor)
            print(">")
        }
    }

    private fun print(s: Any) {
        printer.print(s)
    }

    private fun renderSeparated(elements: List<FirElement>, visitor: Visitor) {
        printer.renderSeparated(elements, visitor)
    }

    private fun renderType(type: ConeKotlinType?) {
        if (type == null) return
        print("R|")
        typeRenderer.render(type)
        print("|")
    }

    private fun renderPhaseAndAttributes(declaration: FirDeclaration) {
        declarationRenderer?.renderPhaseAndAttributes(declaration) ?: resolvePhaseRenderer?.render(declaration)
    }

    inner class Visitor internal constructor() : FirVisitorVoid() {

        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitFile(file: FirFile) {
            printer.print("FILE: ")
            renderPhaseAndAttributes(file)
            printer.println(file.name)

            printer.pushIndent()
            annotationRenderer?.render(file)
            visitPackageDirective(file.packageDirective)
            file.imports.forEach { it.accept(this) }
            file.declarations.forEach { it.accept(this) }
            printer.popIndent()
        }

        override fun visitScript(script: FirScript) {
            annotationRenderer?.render(script)
            printer.print("SCRIPT: ")
            renderPhaseAndAttributes(script)
            printer.println(script.name)

            printer.pushIndent()
            // following the function convention, although in a compiled script parameters are passed before receivers
            script.receivers.forEach {
                it.accept(this)
                printer.newLine()
            }
            script.parameters.forEach {
                it.accept(this)
                printer.newLine()
            }

            printer.newLine()

            script.declarations.forEach {
                it.accept(this)
                printer.newLine()
            }

            printer.popIndent()
        }

        override fun visitScriptReceiverParameter(scriptReceiverParameter: FirScriptReceiverParameter) {
            renderPhaseAndAttributes(scriptReceiverParameter)
            annotationRenderer?.render(scriptReceiverParameter)
            print("<script receiver parameter>: ")
            scriptReceiverParameter.typeRef.accept(this)
        }

        override fun visitCodeFragment(codeFragment: FirCodeFragment) {
            printer.print("CODE FRAGMENT:")
            bodyRenderer?.renderBody(codeFragment.block)
        }

        override fun visitAnnotation(annotation: FirAnnotation) {
            annotationRenderer?.renderAnnotation(annotation)
        }

        override fun visitAnnotationCall(annotationCall: FirAnnotationCall) {
            annotationRenderer?.renderAnnotation(annotationCall)
        }

        override fun visitCallableDeclaration(callableDeclaration: FirCallableDeclaration) {
            renderContexts(callableDeclaration.contextParameters)
            annotationRenderer?.render(callableDeclaration)
            if (callableDeclaration is FirProperty) {
                val backingField = callableDeclaration.backingField
                if (backingField?.annotations?.isNotEmpty() == true) {
                    if (renderFieldAnnotationSeparately) {
                        print("field:")
                    }
                    annotationRenderer?.render(backingField)
                }
            }
            visitMemberDeclaration(callableDeclaration)
            if (callableDeclaration !is FirProperty || callableDeclaration.isCatchParameter != true) {
                print(" ")
            }

            val receiverParameter = callableDeclaration.receiverParameter
            if (receiverParameter != null) {
                renderReceiverParameter(receiverParameter, isExplicit = false)
                print(".")
            }
            when (callableDeclaration) {
                is FirSimpleFunction -> {
                    idRenderer.renderCallableId(callableDeclaration.symbol.callableId)
                }
                is FirVariable -> {
                    idRenderer.renderCallableId(callableDeclaration.symbol.callableId)
                }
                else -> {}
            }

            if (callableDeclaration is FirFunction) {
                callableSignatureRenderer?.renderParameters(callableDeclaration.valueParameters)
            }
            callableSignatureRenderer?.renderReturnTypePrefix()
            callableSignatureRenderer?.renderCallableType(callableDeclaration)
            contractRenderer?.render(callableDeclaration)
        }

        override fun visitTypeParameterRef(typeParameterRef: FirTypeParameterRef) {
            typeParameterRef.symbol.fir.accept(this)
        }

        override fun visitOuterClassTypeParameterRef(outerClassTypeParameterRef: FirOuterClassTypeParameterRef) {
            renderTypeParameter(outerClassTypeParameterRef.symbol.fir, forOuterTypeRef = true)
        }

        override fun visitConstructedClassTypeParameterRef(constructedClassTypeParameterRef: FirConstructedClassTypeParameterRef) {
            visitTypeParameterRef(constructedClassTypeParameterRef)
        }

        override fun visitMemberDeclaration(memberDeclaration: FirMemberDeclaration) {
            modifierRenderer?.renderModifiers(memberDeclaration)
            declarationRenderer?.render(memberDeclaration)
            when (memberDeclaration) {
                is FirClassLikeDeclaration -> {
                    if (memberDeclaration is FirRegularClass) {
                        print(" " + memberDeclaration.name)
                    }
                    if (memberDeclaration is FirTypeAlias) {
                        print(" " + memberDeclaration.name)
                    }
                    memberDeclaration.typeParameters.renderTypeParameters()
                }
                is FirCallableDeclaration -> {
                    // Name is handled by visitCallableDeclaration
                    if (memberDeclaration.typeParameters.isNotEmpty()) {
                        print(" ")
                        memberDeclaration.typeParameters.renderTypeParameters()
                    }
                }
            }
        }

        override fun visitRegularClass(regularClass: FirRegularClass) {
            renderContexts(regularClass.contextParameters)
            annotationRenderer?.render(regularClass)
            visitMemberDeclaration(regularClass)
            supertypeRenderer?.renderSupertypes(regularClass)
            classMemberRenderer?.render(regularClass)
        }

        override fun visitEnumEntry(enumEntry: FirEnumEntry) {
            visitVariable(enumEntry)
        }

        override fun visitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression) {
            anonymousObjectExpression.anonymousObject.accept(this)
        }

        override fun visitAnonymousObject(anonymousObject: FirAnonymousObject) {
            annotationRenderer?.render(anonymousObject)
            print("object : ")
            renderSeparated(anonymousObject.superTypeRefs, visitor)
            classMemberRenderer?.render(anonymousObject.declarations)
        }

        override fun visitVariable(variable: FirVariable) {
            visitCallableDeclaration(variable)
            bodyRenderer?.render(variable)
        }

        override fun visitField(field: FirField) {
            visitVariable(field)
            printer.newLine()
        }

        override fun visitProperty(property: FirProperty) {
            visitVariable(property)
            if (property.isLocal) return
            propertyAccessorRenderer?.render(property)
        }

        override fun visitErrorProperty(errorProperty: FirErrorProperty) {
            visitProperty(errorProperty)
        }

        override fun visitErrorFunction(errorFunction: FirErrorFunction) {
            visitFunction(errorFunction)
        }

        override fun visitBackingField(backingField: FirBackingField) {
            visitVariable(backingField)
        }

        override fun visitReceiverParameter(receiverParameter: FirReceiverParameter) {
            renderReceiverParameter(receiverParameter, isExplicit = true)
        }

        fun renderReceiverParameter(receiverParameter: FirReceiverParameter, isExplicit: Boolean) {
            renderPhaseAndAttributes(receiverParameter)

            if (isExplicit) {
                annotationRenderer?.render(receiverParameter)
                print("<explicit receiver parameter>: ")
            } else {
                annotationRenderer?.render(receiverParameter, AnnotationUseSiteTarget.RECEIVER)
            }

            receiverParameter.typeRef.accept(this)
        }

        override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
            visitFunction(simpleFunction)
        }

        override fun visitConstructor(constructor: FirConstructor) {
            renderContexts(constructor.contextParameters)
            annotationRenderer?.render(constructor)
            modifierRenderer?.renderModifiers(constructor)
            declarationRenderer?.render(constructor)

            constructor.typeParameters.renderTypeParameters()
            callableSignatureRenderer?.renderParameters(constructor.valueParameters)
            print(": ")
            constructor.returnTypeRef.accept(this)
            val body = constructor.body
            val delegatedConstructor = constructor.delegatedConstructor
            if (body == null) {
                bodyRenderer?.renderDelegatedConstructor(delegatedConstructor)
            }
            bodyRenderer?.renderBody(body, listOfNotNull<FirStatement>(delegatedConstructor))
        }

        override fun visitErrorPrimaryConstructor(errorPrimaryConstructor: FirErrorPrimaryConstructor) {
            visitConstructor(errorPrimaryConstructor)
        }

        override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor) {
            annotationRenderer?.render(propertyAccessor)
            modifierRenderer?.renderModifiers(propertyAccessor)
            declarationRenderer?.render(propertyAccessor)
            callableSignatureRenderer?.renderParameters(propertyAccessor.valueParameters)
            print(": ")
            propertyAccessor.returnTypeRef.accept(this)
            contractRenderer?.render(propertyAccessor)
            bodyRenderer?.render(propertyAccessor)
        }

        override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression) {
            if (anonymousFunctionExpression.isTrailingLambda) print("<L> = ")
            visitAnonymousFunction(anonymousFunctionExpression.anonymousFunction)
        }

        override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
            renderContexts(anonymousFunction.contextParameters, lineBreakAfter = false)
            annotationRenderer?.render(anonymousFunction)
            modifierRenderer?.renderModifiers(anonymousFunction)
            declarationRenderer?.render(anonymousFunction)
            print(" ")
            val receiverParameter = anonymousFunction.receiverParameter
            if (receiverParameter != null) {
                renderReceiverParameter(receiverParameter, isExplicit = false)
                print(".")
            }
            print("<anonymous>")
            if (anonymousFunction.valueParameters.isEmpty() &&
                anonymousFunction.hasExplicitParameterList &&
                anonymousFunction.returnTypeRef is FirImplicitTypeRef
            ) {
                print("(<no-parameters>)")
            }
            callableSignatureRenderer?.renderParameters(anonymousFunction.valueParameters)
            print(": ")
            anonymousFunction.returnTypeRef.accept(this)
            print(" <inline=${anonymousFunction.inlineStatus}")
            if (anonymousFunction.invocationKind != null) {
                print(", kind=${anonymousFunction.invocationKind}")
            }
            print("> ")
            bodyRenderer?.render(anonymousFunction)
        }

        override fun visitFunction(function: FirFunction) {
            visitCallableDeclaration(function)
            bodyRenderer?.render(function)
            if (function.body == null) {
                printer.newLine()
            }
        }

        override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer) {
            renderPhaseAndAttributes(anonymousInitializer)
            annotationRenderer?.render(anonymousInitializer)
            print("init")
            bodyRenderer?.renderBody(anonymousInitializer.body)
        }

        override fun visitDanglingModifierList(danglingModifierList: FirDanglingModifierList) {
            renderPhaseAndAttributes(danglingModifierList)
            annotationRenderer?.render(danglingModifierList)
            print("<DANGLING MODIFIER: ${danglingModifierList.diagnostic.reason}>")
        }

        override fun visitBlock(block: FirBlock) {
            bodyRenderer?.renderBody(block)
        }

        override fun visitTypeAlias(typeAlias: FirTypeAlias) {
            annotationRenderer?.render(typeAlias)
            visitMemberDeclaration(typeAlias)
            supertypeRenderer?.renderTypeAliasExpansion(typeAlias)
            printer.newLine()
        }

        override fun visitTypeParameter(typeParameter: FirTypeParameter) {
            renderTypeParameter(typeParameter)
        }

        private fun renderTypeParameter(typeParameter: FirTypeParameter, forOuterTypeRef: Boolean = false) {
            annotationRenderer?.render(typeParameter)
            modifierRenderer?.renderModifiers(typeParameter)
            renderPhaseAndAttributes(typeParameter)
            typeParameter.variance.renderVariance()

            if (!forOuterTypeRef) {
                print(typeParameter.name)
            } else {
                print("Outer(${typeParameter.name})")
            }

            val meaningfulBounds = typeParameter.bounds.filter {
                if (it !is FirResolvedTypeRef) return@filter true
                if (!it.coneType.isMarkedOrFlexiblyNullable) return@filter true
                val type = it.coneType as? ConeLookupTagBasedType ?: return@filter true
                (type.lookupTag as? ConeClassLikeLookupTag)?.classId != StandardClassIds.Any
            }

            if (meaningfulBounds.isNotEmpty()) {
                print(" : ")
                renderSeparated(meaningfulBounds, visitor)
            }
        }

        override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression) {
            safeCallExpression.receiver.accept(this)
            print("?.{ ")
            safeCallExpression.selector.accept(this)
            print(" }")
        }

        override fun visitCheckedSafeCallSubject(checkedSafeCallSubject: FirCheckedSafeCallSubject) {
            print("\$subj\$")
        }

        override fun visitValueParameter(valueParameter: FirValueParameter) {
            callableSignatureRenderer?.renderParameter(valueParameter)
        }

        override fun visitImport(import: FirImport) {
            visitElement(import)
        }

        override fun visitStatement(statement: FirStatement) {
            visitElement(statement)
        }

        override fun visitReturnExpression(returnExpression: FirReturnExpression) {
            annotationRenderer?.render(returnExpression)
            print("^")
            val target = returnExpression.target
            val labeledElement = target.labeledElement
            if (labeledElement is FirSimpleFunction) {
                print("${labeledElement.name}")
            } else {
                val labelName = target.labelName
                if (labelName != null) {
                    print("@$labelName")
                }
            }
            print(" ")
            returnExpression.result.accept(this)
        }

        override fun visitWhenBranch(whenBranch: FirWhenBranch) {
            val condition = whenBranch.condition
            if (condition is FirElseIfTrueCondition) {
                print("else")
            } else {
                condition.accept(this)
            }
            print(" -> ")
            whenBranch.result.accept(this)
        }

        override fun visitWhenExpression(whenExpression: FirWhenExpression) {
            annotationRenderer?.render(whenExpression)
            print("when (")
            val subjectVariable = whenExpression.subjectVariable
            if (subjectVariable != null) {
                subjectVariable.accept(this)
            } else {
                whenExpression.subjectVariable?.initializer?.accept(this)
            }
            printer.println(") {")
            printer.pushIndent()
            for (branch in whenExpression.branches) {
                branch.accept(this)
            }
            printer.popIndent()
            printer.println("}")
        }

        override fun visitWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression) {
            print("\$subj\$")
        }

        override fun visitTryExpression(tryExpression: FirTryExpression) {
            annotationRenderer?.render(tryExpression)
            print("try")
            tryExpression.tryBlock.accept(this)
            for (catchClause in tryExpression.catches) {
                print("catch (")
                catchClause.parameter.accept(this)
                print(")")
                catchClause.block.accept(this)
            }
            val finallyBlock = tryExpression.finallyBlock ?: return
            print("finally")
            finallyBlock.accept(this)
        }

        override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop) {
            val label = doWhileLoop.label
            if (label != null) {
                print("${label.name}@")
            }
            print("do")
            doWhileLoop.block.accept(this)
            print("while(")
            doWhileLoop.condition.accept(this)
            print(")")
        }

        override fun visitWhileLoop(whileLoop: FirWhileLoop) {
            val label = whileLoop.label
            if (label != null) {
                print("${label.name}@")
            }
            print("while(")
            whileLoop.condition.accept(this)
            print(")")
            whileLoop.block.accept(this)
        }

        private val loopJumpStack = Stack<FirLoopJump>()

        override fun visitLoopJump(loopJump: FirLoopJump) {
            if (loopJumpStack.contains(loopJump)) {
                // For example,
                //   do {
                //     ...
                //   } while(
                //       when (...) {
                //         ... -> break
                //       }
                //   )
                // That `break` condition is `when` expression, and while visiting its branch result, we will see the same `break` again.
                return
            }
            loopJumpStack.push(loopJump)
            val target = loopJump.target
            val labeledElement = target.labeledElement
            print("@@@[")
            labeledElement.condition.accept(this)
            print("] ")
            loopJumpStack.pop()
        }

        override fun visitBreakExpression(breakExpression: FirBreakExpression) {
            annotationRenderer?.render(breakExpression)
            print("break")
            visitLoopJump(breakExpression)
        }

        override fun visitContinueExpression(continueExpression: FirContinueExpression) {
            annotationRenderer?.render(continueExpression)
            print("continue")
            visitLoopJump(continueExpression)
        }

        override fun visitExpression(expression: FirExpression) {
            annotationRenderer?.render(expression)
            print(
                when (expression) {
                    is FirExpressionStub -> "STUB"
                    is FirUnitExpression -> "Unit"
                    is FirElseIfTrueCondition -> "else"
                    else -> "??? ${expression.javaClass}"
                }
            )
        }

        override fun visitLazyExpression(lazyExpression: FirLazyExpression) {
            print("LAZY_EXPRESSION")
        }

        override fun visitLiteralExpression(literalExpression: FirLiteralExpression) {
            annotationRenderer?.render(literalExpression)
            val kind = literalExpression.kind
            val value = literalExpression.value
            print("$kind(")
            if (value !is Char) {
                print(value.toString())
            } else {
                if (value.code in 32..127) {
                    print(value)
                } else {
                    print(value.code)
                }
            }
            print(")")
        }

        override fun visitWrappedDelegateExpression(wrappedDelegateExpression: FirWrappedDelegateExpression) {
            wrappedDelegateExpression.expression.accept(this)
        }

        override fun visitEnumEntryDeserializedAccessExpression(enumEntryDeserializedAccessExpression: FirEnumEntryDeserializedAccessExpression) {
            with(enumEntryDeserializedAccessExpression) {
                print("$enumClassId.$enumEntryName")
            }
        }

        override fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression) {
            print(namedArgumentExpression.name)
            print(" = ")
            if (namedArgumentExpression.isSpread) {
                print("*")
            }
            namedArgumentExpression.expression.accept(this)
        }

        override fun visitSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression) {
            if (spreadArgumentExpression.isSpread) {
                print("*")
            }
            spreadArgumentExpression.expression.accept(this)
        }

        override fun visitVarargArgumentsExpression(varargArgumentsExpression: FirVarargArgumentsExpression) {
            print("vararg(")
            renderSeparated(varargArgumentsExpression.arguments, visitor)

            if (renderVarargTypes) {
                if (varargArgumentsExpression.arguments.isNotEmpty()) {
                    print("; ")
                }
                print("type = ")
                typeRenderer.render(varargArgumentsExpression.resolvedType)
                print(", elementType = ")
                typeRenderer.render(varargArgumentsExpression.coneElementTypeOrNull!!)
            }

            print(")")
        }

        override fun visitSamConversionExpression(samConversionExpression: FirSamConversionExpression) {
            val expression = samConversionExpression.expression

            if (expression is FirAnonymousFunctionExpression && expression.isTrailingLambda) {
                print("<L> = SAM(")
                expression.anonymousFunction.accept(this)
                print(")")
                return
            }

            print("SAM(")
            expression.accept(this)
            print(")")
        }

        override fun visitCall(call: FirCall) {
            callArgumentsRenderer?.renderArguments(call.arguments)
        }

        override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall) {
            print("<strcat>")
            visitCall(stringConcatenationCall)
        }

        override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall) {
            print("(")
            typeOperatorCall.argument.accept(this)
            print(" ")
            print(typeOperatorCall.operation.operator)
            print(" ")
            typeOperatorCall.conversionTypeRef.accept(this)
            print(")")
        }

        override fun visitDelegatedConstructorCall(delegatedConstructorCall: FirDelegatedConstructorCall) {
            if (delegatedConstructorCall !is FirLazyDelegatedConstructorCall) {
                val dispatchReceiver = delegatedConstructorCall.dispatchReceiver
                if (dispatchReceiver != null) {
                    dispatchReceiver.accept(this)
                    print(".")
                }
            }
            if (delegatedConstructorCall is FirLazyDelegatedConstructorCall) {
                print("LAZY_")
            }
            if (delegatedConstructorCall.isSuper) {
                print("super<")
            } else if (delegatedConstructorCall.isThis) {
                print("this<")
            }
            delegatedConstructorCall.constructedTypeRef.accept(this)
            print(">")
            if (delegatedConstructorCall !is FirLazyDelegatedConstructorCall) {
                visitCall(delegatedConstructorCall)
            }
        }

        override fun visitMultiDelegatedConstructorCall(multiDelegatedConstructorCall: FirMultiDelegatedConstructorCall) {
            var first = true
            for (delegatedConstructorCall in multiDelegatedConstructorCall.delegatedConstructorCalls) {
                if (first) {
                    first = false
                } else {
                    printer.println()
                }
                visitDelegatedConstructorCall(delegatedConstructorCall)
            }
        }

        override fun visitTypeRef(typeRef: FirTypeRef) {
            annotationRenderer?.render(typeRef)
            if (typeRef.customRenderer) {
                print(typeRef.toString())
                return
            }
            visitElement(typeRef)
        }

        override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef) {
            annotationRenderer?.render(errorTypeRef)
            print("<ERROR TYPE REF: ${errorTypeRef.diagnostic.reason}>")
        }

        override fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef) {
            print("<implicit>")
        }

        override fun visitUnresolvedTypeRef(unresolvedTypeRef: FirUnresolvedTypeRef) {
            if (unresolvedTypeRef.isMarkedNullable) {
                print("?")
            }
        }

        override fun visitDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef) {
            annotationRenderer?.render(dynamicTypeRef)
            print("<dynamic>")
            visitUnresolvedTypeRef(dynamicTypeRef)
        }

        override fun visitFunctionTypeRef(functionTypeRef: FirFunctionTypeRef) {
            if (functionTypeRef.contextParameterTypeRefs.isNotEmpty()) {
                print("context(")
                renderSeparated(functionTypeRef.contextParameterTypeRefs, visitor)
                print(")")
            }

            annotationRenderer?.renderAnnotations(functionTypeRef.annotations.dropExtensionFunctionAnnotation())
            print("( ")
            modifierRenderer?.renderModifiers(functionTypeRef)
            functionTypeRef.receiverTypeRef?.let {
                it.accept(this)
                print(".")
            }

            printer.print("(")
            for ((index, parameter) in functionTypeRef.parameters.withIndex()) {
                if (index > 0) {
                    printer.print(", ")
                }
                parameter.name?.let { name ->
                    printer.print(name.asString())
                    printer.print(": ")
                }
                parameter.returnTypeRef.accept(visitor)
            }
            printer.print(")")

            print(" -> ")
            functionTypeRef.returnTypeRef.accept(this)
            print(" )")
            visitUnresolvedTypeRef(functionTypeRef)
        }

        @OptIn(AllowedToUsedOnlyInK1::class)
        override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
            typeRenderer.renderAsPossibleFunctionType(
                resolvedTypeRef.coneType,
                l@{
                    if (it is ConeFlexibleType) return@l null
                    val classId = it.classId ?: return@l null
                    FunctionTypeKindExtractor.Default.getFunctionalClassKind(classId.packageFqName, classId.shortClassName.asString())
                }
            )
        }

        override fun visitUserTypeRef(userTypeRef: FirUserTypeRef) {
            annotationRenderer?.render(userTypeRef)
            for ((index, qualifier) in userTypeRef.qualifier.withIndex()) {
                if (index != 0) {
                    print(".")
                }
                print(qualifier.name)
                if (qualifier.typeArgumentList.typeArguments.isNotEmpty()) {
                    print("<")
                    renderSeparated(qualifier.typeArgumentList.typeArguments, visitor)
                    print(">")
                }
            }
            visitUnresolvedTypeRef(userTypeRef)
        }

        override fun visitTypeProjection(typeProjection: FirTypeProjection) {
            visitElement(typeProjection)
        }

        override fun visitTypeProjectionWithVariance(typeProjectionWithVariance: FirTypeProjectionWithVariance) {
            typeProjectionWithVariance.variance.renderVariance()
            typeProjectionWithVariance.typeRef.accept(this)
        }

        override fun visitStarProjection(starProjection: FirStarProjection) {
            print("*")
        }

        override fun visitNamedReference(namedReference: FirNamedReference) {
            print("${namedReference.name}#")
        }

        override fun visitNamedReferenceWithCandidateBase(namedReferenceWithCandidateBase: FirNamedReferenceWithCandidateBase) {
            print("R?C|")
            referencedSymbolRenderer.printReference(namedReferenceWithCandidateBase.candidateSymbol)
            print("|")
        }

        override fun visitErrorNamedReference(errorNamedReference: FirErrorNamedReference) {
            print("<${errorNamedReference.diagnostic.reason}>#")
        }

        override fun visitBackingFieldReference(backingFieldReference: FirBackingFieldReference) {
            print("F|")
            print(backingFieldReference.resolvedSymbol.fir.propertySymbol.callableId)
            print("|")
        }

        override fun visitDelegateFieldReference(delegateFieldReference: FirDelegateFieldReference) {
            print("D|")
            print(delegateFieldReference.resolvedSymbol.callableId)
            print("|")
        }

        override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference) {
            resolvedNamedReferenceRenderer.render(resolvedNamedReference)
        }

        override fun visitResolvedErrorReference(resolvedErrorReference: FirResolvedErrorReference) {
            visitResolvedNamedReference(resolvedErrorReference)
        }

        override fun visitResolvedCallableReference(resolvedCallableReference: FirResolvedCallableReference) {
            visitResolvedNamedReference(resolvedCallableReference)
        }

        override fun visitThisReference(thisReference: FirThisReference) {
            print("this")
            val labelName = thisReference.labelName
            val symbol = thisReference.boundSymbol
            when {
                symbol != null -> {
                    print("@R|")
                    referencedSymbolRenderer.printReference(symbol as FirBasedSymbol<*>)
                    print("|")
                }
                labelName != null -> print("@$labelName#")
                else -> print("#")
            }
        }

        override fun visitSuperReference(superReference: FirSuperReference) {
            print("super<")
            superReference.superTypeRef.accept(this)
            print(">")
            superReference.labelName?.let {
                print("@$it#")
            }
        }

        private fun visitQualifiedAccessExpressionReceivers(qualifiedAccess: FirQualifiedAccessExpression) {
            val explicitReceiver = qualifiedAccess.explicitReceiver
            val dispatchReceiver = qualifiedAccess.dispatchReceiver
            val extensionReceiver = qualifiedAccess.extensionReceiver
            var hasSomeReceiver = true
            when {
                dispatchReceiver != null && extensionReceiver != null -> {
                    print("(")
                    dispatchReceiver.accept(this)
                    print(", ")
                    extensionReceiver.accept(this)
                    print(")")
                }
                dispatchReceiver != null -> {
                    dispatchReceiver.accept(this)
                }
                extensionReceiver != null -> {
                    extensionReceiver.accept(this)
                }
                explicitReceiver != null -> {
                    explicitReceiver.accept(this)
                }
                else -> {
                    hasSomeReceiver = false
                }
            }
            if (hasSomeReceiver) {
                print(".")
            }
        }

        override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall) {
            checkNotNullCall.argument.accept(this)
            print("!!")
        }

        override fun visitElvisExpression(elvisExpression: FirElvisExpression) {
            elvisExpression.lhs.accept(this)
            print(" ?: ")
            elvisExpression.rhs.accept(this)
        }

        override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess) {
            annotationRenderer?.render(callableReferenceAccess)
            callableReferenceAccess.explicitReceiver?.accept(this)
            if (callableReferenceAccess.hasQuestionMarkAtLHS && callableReferenceAccess.explicitReceiver !is FirResolvedQualifier) {
                print("?")
            }
            print("::")
            callableReferenceAccess.calleeReference.accept(this)
        }

        override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
            annotationRenderer?.render(qualifiedAccessExpression)
            visitQualifiedAccessExpressionReceivers(qualifiedAccessExpression)
            qualifiedAccessExpression.calleeReference.accept(this)
            qualifiedAccessExpression.typeArguments.renderTypeArguments()
        }

        override fun visitQualifiedErrorAccessExpression(qualifiedErrorAccessExpression: FirQualifiedErrorAccessExpression) {
            errorExpressionRenderer?.renderDiagnostic(qualifiedErrorAccessExpression.diagnostic)
            annotationRenderer?.render(qualifiedErrorAccessExpression)
            qualifiedErrorAccessExpression.receiver.accept(this)
            print('.')
            qualifiedErrorAccessExpression.selector.accept(this)
        }

        override fun visitSuperReceiverExpression(superReceiverExpression: FirSuperReceiverExpression) {
            visitQualifiedAccessExpression(superReceiverExpression)
        }

        override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
            visitQualifiedAccessExpression(propertyAccessExpression)
        }

        override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression) {
            visitQualifiedAccessExpression(thisReceiverExpression)
        }

        override fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression) {
            smartCastExpression.originalExpression.accept(this)
        }

        override fun visitDesugaredAssignmentValueReferenceExpression(desugaredAssignmentValueReferenceExpression: FirDesugaredAssignmentValueReferenceExpression) {
            desugaredAssignmentValueReferenceExpression.expressionRef.value.accept(this)
        }

        override fun visitVariableAssignment(variableAssignment: FirVariableAssignment) {
            annotationRenderer?.render(variableAssignment)
            variableAssignment.lValue.accept(this)
            print(" ")
            print(FirOperation.ASSIGN.operator)
            print(" ")
            variableAssignment.rValue.accept(visitor)
        }

        override fun visitIndexedAccessAugmentedAssignment(indexedAccessAugmentedAssignment: FirIndexedAccessAugmentedAssignment) {
            annotationRenderer?.render(indexedAccessAugmentedAssignment)
            print("ArraySet:[")
            indexedAccessAugmentedAssignment.lhsGetCall.accept(this)
            print(" ")
            print(indexedAccessAugmentedAssignment.operation.operator)
            print(" ")
            indexedAccessAugmentedAssignment.rhs.accept(this)
            print("]")
        }

        override fun visitFunctionCall(functionCall: FirFunctionCall) {
            annotationRenderer?.render(functionCall)
            visitQualifiedAccessExpressionReceivers(functionCall)
            functionCall.calleeReference.accept(this)
            functionCall.typeArguments.renderTypeArguments()
            visitCall(functionCall)
        }

        override fun visitIntegerLiteralOperatorCall(integerLiteralOperatorCall: FirIntegerLiteralOperatorCall) {
            visitFunctionCall(integerLiteralOperatorCall)
        }

        override fun visitImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall) {
            visitFunctionCall(implicitInvokeCall)
        }

        override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression) {
            print("CMP(${comparisonExpression.operation.operator}, ")
            comparisonExpression.compareToCall.accept(this)
            print(")")
        }

        override fun visitAugmentedAssignment(augmentedAssignment: FirAugmentedAssignment) {
            annotationRenderer?.render(augmentedAssignment)
            print(augmentedAssignment.operation.operator)
            print("(")
            augmentedAssignment.leftArgument.accept(visitor)
            print(", ")
            augmentedAssignment.rightArgument.accept(visitor)
            print(")")
        }

        override fun visitIncrementDecrementExpression(incrementDecrementExpression: FirIncrementDecrementExpression) {
            annotationRenderer?.render(incrementDecrementExpression)
            val operator = if (incrementDecrementExpression.operationName == OperatorNameConventions.INC) "++" else "--"
            if (incrementDecrementExpression.isPrefix) print(operator)
            incrementDecrementExpression.expression.accept(visitor)
            if (!incrementDecrementExpression.isPrefix) print(operator)
        }

        override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall) {
            annotationRenderer?.render(equalityOperatorCall)
            print(equalityOperatorCall.operation.operator)
            visitCall(equalityOperatorCall)
        }

        override fun visitComponentCall(componentCall: FirComponentCall) {
            visitFunctionCall(componentCall)
        }

        override fun visitGetClassCall(getClassCall: FirGetClassCall) {
            getClassCallRenderer.render(getClassCall)
        }

        override fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression) {
            annotationRenderer?.render(classReferenceExpression)
            print("<getClass>")
            print("(")
            classReferenceExpression.classTypeRef.accept(this)
            print(")")
        }

        override fun visitArrayLiteral(arrayLiteral: FirArrayLiteral) {
            annotationRenderer?.render(arrayLiteral)
            print("<implicitArrayOf>")
            visitCall(arrayLiteral)
        }

        override fun visitThrowExpression(throwExpression: FirThrowExpression) {
            annotationRenderer?.render(throwExpression)
            print("throw ")
            throwExpression.exception.accept(this)
        }

        override fun visitErrorExpression(errorExpression: FirErrorExpression) {
            errorExpressionRenderer?.renderErrorExpression(errorExpression)
        }

        override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
            resolvedQualifierRenderer.render(resolvedQualifier)
            resolvedQualifier.typeArguments.renderTypeArguments()
        }

        override fun visitErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier) {
            visitResolvedQualifier(errorResolvedQualifier)
        }

        override fun visitBooleanOperatorExpression(booleanOperatorExpression: FirBooleanOperatorExpression) {
            booleanOperatorExpression.leftOperand.accept(this)
            print(" ${booleanOperatorExpression.kind.token} ")
            booleanOperatorExpression.rightOperand.accept(this)
        }

        override fun visitEffectDeclaration(effectDeclaration: FirEffectDeclaration) {
            contractRenderer?.render(effectDeclaration)
        }

        override fun visitContractDescription(contractDescription: FirContractDescription) {
            shouldNotBeCalled()
        }

        override fun visitPackageDirective(packageDirective: FirPackageDirective) {
            packageDirectiveRenderer?.render(packageDirective)
        }

        override fun visitResolvedReifiedParameterReference(resolvedReifiedParameterReference: FirResolvedReifiedParameterReference) {
            @OptIn(UnresolvedExpressionTypeAccess::class)
            renderType(resolvedReifiedParameterReference.coneTypeOrNull)
        }

        override fun visitInaccessibleReceiverExpression(inaccessibleReceiverExpression: FirInaccessibleReceiverExpression) {
            @OptIn(UnresolvedExpressionTypeAccess::class)
            renderType(inaccessibleReceiverExpression.coneTypeOrNull)
            visitElement(inaccessibleReceiverExpression)
        }
    }
}
