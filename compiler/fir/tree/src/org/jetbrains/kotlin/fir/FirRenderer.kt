/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.builtins.functions.BuiltInFictitiousFunctionClassFactory
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.contracts.*
import org.jetbrains.kotlin.fir.contracts.description.ConeContractRenderer
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun FirElement.renderWithType(mode: FirRenderer.RenderMode = FirRenderer.RenderMode.Normal): String = buildString {
    append(this@renderWithType)
    append(": ")
    this@renderWithType.accept(FirRenderer(this, mode))
}

fun FirElement.render(mode: FirRenderer.RenderMode = FirRenderer.RenderMode.Normal): String =
    buildString { this@render.accept(FirRenderer(this, mode)) }

class FirRenderer(builder: StringBuilder, private val mode: RenderMode = RenderMode.Normal) : FirVisitorVoid() {
    companion object {
        private val visibilitiesToRenderEffectiveSet = setOf(
            Visibilities.PRIVATE, Visibilities.PRIVATE_TO_THIS, Visibilities.INTERNAL,
            Visibilities.PROTECTED, Visibilities.PUBLIC, Visibilities.LOCAL
        )
    }

    abstract class RenderMode(
        val renderLambdaBodies: Boolean,
        val renderCallArguments: Boolean,
        val renderCallableFqNames: Boolean
    ) {
        object Normal : RenderMode(renderLambdaBodies = true, renderCallArguments = true, renderCallableFqNames = false)

        object WithFqNames: RenderMode(renderLambdaBodies = true, renderCallArguments = true, renderCallableFqNames = true)
    }

    private val printer = Printer(builder)

    private var lineBeginning = true

    private fun print(vararg objects: Any) {
        if (lineBeginning) {
            lineBeginning = false
            printer.print(*objects)
        } else {
            printer.printWithNoIndent(*objects)
        }
    }

    private fun println(vararg objects: Any) {
        print(*objects)
        printer.printlnWithNoIndent()
        lineBeginning = true
    }

    private fun pushIndent() {
        printer.pushIndent()
    }

    private fun popIndent() {
        printer.popIndent()
    }

    fun newLine() {
        println()
    }

    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    override fun visitFile(file: FirFile) {
        println("FILE: ${file.name}")
        pushIndent()
        visitElement(file)
        popIndent()
    }

    private fun List<FirElement>.renderSeparated() {
        for ((index, element) in this.withIndex()) {
            if (index > 0) {
                print(", ")
            }
            element.accept(this@FirRenderer)
        }
    }

    private fun List<FirElement>.renderSeparatedWithNewlines() {
        for ((index, element) in this.withIndex()) {
            if (index > 0) {
                print(",")
                newLine()
            }
            element.accept(this@FirRenderer)
        }
    }

    private fun List<ConeKotlinType>.renderTypesSeparated() {
        for ((index, element) in this.withIndex()) {
            if (index > 0) {
                print(", ")
            }
            print(element.render())
        }
    }


    private fun List<FirValueParameter>.renderParameters() {
        print("(")
        renderSeparated()
        print(")")
    }

    private fun List<FirAnnotationCall>.renderAnnotations() {
        for (annotation in this) {
            visitAnnotationCall(annotation)
        }
    }

    private fun Variance.renderVariance() {
        label.let {
            print(it)
            if (it.isNotEmpty()) {
                print(" ")
            }
        }
    }

    override fun <F : FirCallableDeclaration<F>> visitCallableDeclaration(callableDeclaration: FirCallableDeclaration<F>) {
        if (callableDeclaration is FirMemberDeclaration) {
            visitMemberDeclaration(callableDeclaration)
        } else {
            callableDeclaration.annotations.renderAnnotations()
            visitTypedDeclaration(callableDeclaration)
        }
        val receiverType = callableDeclaration.receiverTypeRef
        print(" ")
        if (receiverType != null) {
            receiverType.accept(this)
            print(".")
        }
        when (callableDeclaration) {
            is FirSimpleFunction -> {
                if (!mode.renderCallableFqNames) {
                    print(callableDeclaration.name)
                } else {
                    print(callableDeclaration.symbol.callableId)
                }
            }
            is FirVariable<*> -> {
                if (!mode.renderCallableFqNames) {
                    print(callableDeclaration.name)
                } else {
                    print(callableDeclaration.symbol.callableId)
                }
            }
        }

        if (callableDeclaration is FirFunction<*>) {
            callableDeclaration.valueParameters.renderParameters()
        }
        print(": ")
        callableDeclaration.returnTypeRef.accept(this)
        callableDeclaration.renderContractDescription()
    }

    private fun FirDeclaration.renderContractDescription() {
        val contractDescription = (this as? FirContractDescriptionOwner)?.contractDescription ?: return
        pushIndent()
        contractDescription.accept(this@FirRenderer)
        popIndent()
    }

    private fun Visibility.asString(effectiveVisibility: FirEffectiveVisibility? = null): String {
        val itself = when (this) {
            Visibilities.UNKNOWN -> return "public?"
            else -> toString()
        }
        if (effectiveVisibility == null || effectiveVisibility == FirEffectiveVisibility.Default) return itself
        val effectiveAsVisibility = effectiveVisibility.toVisibility()
        if (effectiveAsVisibility == this) return itself
        if (effectiveAsVisibility == Visibilities.PRIVATE && this == Visibilities.PRIVATE_TO_THIS) return itself
        if (this !in visibilitiesToRenderEffectiveSet) return itself
        return itself + "[${effectiveVisibility.name}]"
    }

    private fun FirMemberDeclaration.modalityAsString(): String {
        return modality?.name?.toLowerCase() ?: run {
            if (this is FirCallableMemberDeclaration<*> && this.isOverride) {
                "open?"
            } else {
                "final?"
            }
        }
    }

    private fun List<FirTypeParameterRef>.renderTypeParameters() {
        if (isNotEmpty()) {
            print("<")
            renderSeparated()
            print(">")
        }
    }

    private fun List<FirTypeProjection>.renderTypeArguments() {
        if (isNotEmpty()) {
            print("<")
            renderSeparated()
            print(">")
        }
    }

    override fun visitTypeParameterRef(typeParameterRef: FirTypeParameterRef) {
        typeParameterRef.symbol.fir.accept(this)
    }

    override fun visitMemberDeclaration(memberDeclaration: FirMemberDeclaration) {
        memberDeclaration.annotations.renderAnnotations()
        if (memberDeclaration !is FirProperty || !memberDeclaration.isLocal) {
            // we can't access session.effectiveVisibilityResolver from here!
            // print(memberDeclaration.visibility.asString(memberDeclaration.getEffectiveVisibility(...)) + " ")
            print(memberDeclaration.visibility.asString() + " ")
            print(memberDeclaration.modalityAsString() + " ")
        }
        if (memberDeclaration.isExpect) {
            print("expect ")
        }
        if (memberDeclaration.isActual) {
            print("actual ")
        }
        if (memberDeclaration is FirCallableMemberDeclaration<*>) {
            if (memberDeclaration.isOverride) {
                print("override ")
            }
            if (memberDeclaration.isStatic) {
                print("static ")
            }
        }
        if (memberDeclaration is FirRegularClass) {
            if (memberDeclaration.isInner) {
                print("inner ")
            }
            if (memberDeclaration.isCompanion) {
                print("companion ")
            }
            if (memberDeclaration.isData) {
                print("data ")
            }
            if (memberDeclaration.isInline) {
                print("inline ")
            }
        } else if (memberDeclaration is FirSimpleFunction) {
            if (memberDeclaration.isOperator) {
                print("operator ")
            }
            if (memberDeclaration.isInfix) {
                print("infix ")
            }
            if (memberDeclaration.isInline) {
                print("inline ")
            }
            if (memberDeclaration.isTailRec) {
                print("tailrec ")
            }
            if (memberDeclaration.isExternal) {
                print("external ")
            }
            if (memberDeclaration.isSuspend) {
                print("suspend ")
            }
        } else if (memberDeclaration is FirProperty) {
            if (memberDeclaration.isConst) {
                print("const ")
            }
            if (memberDeclaration.isLateInit) {
                print("lateinit ")
            }
        }

        visitDeclaration(memberDeclaration)
        when (memberDeclaration) {
            is FirClassLikeDeclaration<*> -> {
                if (memberDeclaration is FirRegularClass) {
                    print(" " + memberDeclaration.name)
                }
                if (memberDeclaration is FirTypeAlias) {
                    print(" " + memberDeclaration.name)
                }
                memberDeclaration.typeParameters.renderTypeParameters()
            }
            is FirCallableDeclaration<*> -> {
                // Name is handled by visitCallableDeclaration
                if (memberDeclaration.typeParameters.isNotEmpty()) {
                    print(" ")
                    memberDeclaration.typeParameters.renderTypeParameters()
                }
            }
        }
    }

    override fun visitDeclaration(declaration: FirDeclaration) {
        print(
            when (declaration) {
                is FirRegularClass -> declaration.classKind.name.toLowerCase().replace("_", " ")
                is FirTypeAlias -> "typealias"
                is FirSimpleFunction -> "fun"
                is FirProperty -> {
                    val prefix = if (declaration.isLocal) "l" else ""
                    prefix + if (declaration.isVal) "val" else "var"
                }
                is FirField -> "field"
                is FirEnumEntry -> "enum entry"
                else -> "unknown"
            }
        )
    }

    private fun List<FirDeclaration>.renderDeclarations() {
        renderInBraces {
            for (declaration in this) {
                declaration.accept(this@FirRenderer)
                println()
            }
        }
    }

    fun renderInBraces(leftBrace: String = "{", rightBrace: String = "}", f: () -> Unit) {
        println(" $leftBrace")
        pushIndent()
        f()
        popIndent()
        println(rightBrace)
    }

    fun renderSupertypes(regularClass: FirRegularClass) {
        if (regularClass.superTypeRefs.isNotEmpty()) {
            print(" : ")
            regularClass.superTypeRefs.renderSeparated()
        }
    }

    override fun visitRegularClass(regularClass: FirRegularClass) {
        visitMemberDeclaration(regularClass)
        renderSupertypes(regularClass)
        regularClass.declarations.renderDeclarations()
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry) {
        visitCallableDeclaration(enumEntry)
        enumEntry.initializer?.let {
            print(" = ")
            it.accept(this)
        }
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject) {
        anonymousObject.annotations.renderAnnotations()
        print("object : ")
        anonymousObject.superTypeRefs.renderSeparated()
        anonymousObject.declarations.renderDeclarations()
    }

    override fun <F : FirVariable<F>> visitVariable(variable: FirVariable<F>) {
        visitCallableDeclaration(variable)
        variable.initializer?.let {
            print(" = ")
            it.accept(this)
        }
        variable.delegate?.let {
            print("by ")
            it.accept(this)
        }
    }

    override fun visitField(field: FirField) {
        visitVariable(field)
        println()
    }

    override fun visitProperty(property: FirProperty) {
        visitVariable(property)
        if (property.isLocal) return
        println()
        pushIndent()
        property.getter?.accept(this)
        if (property.getter?.body == null) {
            println()
        }
        if (property.isVar) {
            property.setter?.accept(this)
            if (property.setter?.body == null) {
                println()
            }
        }
        popIndent()
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
        visitCallableDeclaration(simpleFunction)
        simpleFunction.body?.renderBody()
        if (simpleFunction.body == null) {
            println()
        }
    }

    override fun visitConstructor(constructor: FirConstructor) {
        constructor.annotations.renderAnnotations()
        // we can't access session.effectiveVisibilityResolver from here!
        // print(constructor.visibility.asString(constructor.getEffectiveVisibility(...)) + " ")
        print(constructor.visibility.asString() + " ")
        if (constructor.isExpect) {
            print("expect ")
        }
        if (constructor.isActual) {
            print("actual ")
        }
        print("constructor")
        constructor.typeParameters.renderTypeParameters()
        constructor.valueParameters.renderParameters()
        print(": ")
        constructor.returnTypeRef.accept(this)
        val body = constructor.body
        val delegatedConstructor = constructor.delegatedConstructor
        if (body == null) {
            if (delegatedConstructor != null) {
                renderInBraces {
                    delegatedConstructor.accept(this)
                    println()
                }
            } else {
                println()
            }
        }
        body?.renderBody(listOfNotNull<FirStatement>(delegatedConstructor))
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor) {
        propertyAccessor.annotations.renderAnnotations()
        print(propertyAccessor.visibility.asString() + " ")
        print(if (propertyAccessor.isGetter) "get" else "set")
        propertyAccessor.valueParameters.renderParameters()
        print(": ")
        propertyAccessor.returnTypeRef.accept(this)
        propertyAccessor.renderContractDescription()
        propertyAccessor.body?.renderBody()
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
        anonymousFunction.annotations.renderAnnotations()
        val label = anonymousFunction.label
        if (label != null) {
            print("${label.name}@")
        }
        print("fun ")
        val receiverType = anonymousFunction.receiverTypeRef
        if (receiverType != null) {
            receiverType.accept(this)
            print(".")
        }
        print("<anonymous>")
        anonymousFunction.valueParameters.renderParameters()
        print(": ")
        anonymousFunction.returnTypeRef.accept(this)
        if (anonymousFunction.invocationKind != null) {
            print(" <kind=${anonymousFunction.invocationKind}> ")
        }
        if (mode.renderLambdaBodies) {
            anonymousFunction.body?.renderBody()
        }
    }

    override fun <F : FirFunction<F>> visitFunction(function: FirFunction<F>) {
        function.valueParameters.renderParameters()
        visitDeclaration(function)
        function.body?.renderBody()
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer) {
        print("init")
        anonymousInitializer.body?.renderBody()
    }

    private fun FirBlock.renderBody(additionalStatements: List<FirStatement> = emptyList()) {
        renderInBraces {
            for (statement in additionalStatements + statements) {
                statement.accept(this@FirRenderer)
                println()
            }
        }
    }

    override fun visitBlock(block: FirBlock) {
        block.renderBody()
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias) {
        typeAlias.annotations.renderAnnotations()
        visitMemberDeclaration(typeAlias)
        print(" = ")
        typeAlias.expandedTypeRef.accept(this)
        println()
    }

    override fun visitTypeParameter(typeParameter: FirTypeParameter) {
        typeParameter.annotations.renderAnnotations()
        if (typeParameter.isReified) {
            print("reified ")
        }
        typeParameter.variance.renderVariance()
        print(typeParameter.name)

        val meaningfulBounds = typeParameter.bounds.filter {
            if (it !is FirResolvedTypeRef) return@filter true
            if (!it.type.isNullable) return@filter true
            val type = it.type as? ConeLookupTagBasedType ?: return@filter true
            type.lookupTag.safeAs<ConeClassLikeLookupTag>()?.classId != StandardClassIds.Any
        }

        if (meaningfulBounds.isNotEmpty()) {
            print(" : ")
            meaningfulBounds.renderSeparated()
        }
    }

    override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression) {
        safeCallExpression.receiver.accept(this)
        print("?.{ ")
        safeCallExpression.regularQualifiedAccess.accept(this)
        print(" }")
    }

    override fun visitCheckedSafeCallSubject(checkedSafeCallSubject: FirCheckedSafeCallSubject) {
        print("\$subj\$")
    }

    override fun visitTypedDeclaration(typedDeclaration: FirTypedDeclaration) {
        visitDeclaration(typedDeclaration)
    }

    override fun visitValueParameter(valueParameter: FirValueParameter) {
        valueParameter.annotations.renderAnnotations()
        if (valueParameter.isCrossinline) {
            print("crossinline ")
        }
        if (valueParameter.isNoinline) {
            print("noinline ")
        }
        if (valueParameter.isVararg) {
            print("vararg ")
        }
        if (valueParameter.name != SpecialNames.NO_NAME_PROVIDED) {
            print(valueParameter.name.toString() + ": ")
        }
        valueParameter.returnTypeRef.accept(this)
        valueParameter.defaultValue?.let {
            print(" = ")
            it.accept(this)
        }
    }

    override fun visitImport(import: FirImport) {
        visitElement(import)
    }

    override fun visitStatement(statement: FirStatement) {
        if (statement is FirStubStatement) {
            print("[StubStatement]")
        } else {
            visitElement(statement)
        }
    }

    override fun visitReturnExpression(returnExpression: FirReturnExpression) {
        returnExpression.annotations.renderAnnotations()
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
        whenExpression.annotations.renderAnnotations()
        print("when (")
        val subjectVariable = whenExpression.subjectVariable
        if (subjectVariable != null) {
            subjectVariable.accept(this)
        } else {
            whenExpression.subject?.accept(this)
        }
        println(") {")
        pushIndent()
        for (branch in whenExpression.branches) {
            branch.accept(this)
        }
        popIndent()
        println("}")
    }

    override fun visitWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression) {
        print("\$subj\$")
    }

    override fun visitTryExpression(tryExpression: FirTryExpression) {
        tryExpression.annotations.renderAnnotations()
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

    override fun visitLoopJump(loopJump: FirLoopJump) {
        val target = loopJump.target
        val labeledElement = target.labeledElement
        print("@@@[")
        labeledElement.condition.accept(this)
        print("] ")
    }

    override fun visitBreakExpression(breakExpression: FirBreakExpression) {
        breakExpression.annotations.renderAnnotations()
        print("break")
        visitLoopJump(breakExpression)
    }

    override fun visitContinueExpression(continueExpression: FirContinueExpression) {
        continueExpression.annotations.renderAnnotations()
        print("continue")
        visitLoopJump(continueExpression)
    }

    override fun visitExpression(expression: FirExpression) {
        expression.annotations.renderAnnotations()
        print(
            when (expression) {
                is FirExpressionStub -> "STUB"
                is FirUnitExpression -> "Unit"
                is FirElseIfTrueCondition -> "else"
                is FirNoReceiverExpression -> ""
                else -> "??? ${expression.javaClass}"
            }
        )
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>) {
        constExpression.annotations.renderAnnotations()
        val kind = constExpression.kind
        val value = constExpression.value
        print("$kind(")
        if (value !is Char) {
            print(value.toString())
        } else {
            if (value.toInt() in 32..127) {
                print(value)
            } else {
                print(value.toInt())
            }
        }
        print(")")
    }

    override fun visitWrappedDelegateExpression(wrappedDelegateExpression: FirWrappedDelegateExpression) {
        wrappedDelegateExpression.expression.accept(this)
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

    override fun visitLambdaArgumentExpression(lambdaArgumentExpression: FirLambdaArgumentExpression) {
        print("<L> = ")
        lambdaArgumentExpression.expression.accept(this)
    }

    override fun visitVarargArgumentsExpression(varargArgumentsExpression: FirVarargArgumentsExpression) {
        print("vararg(")
        varargArgumentsExpression.arguments.renderSeparated()
        print(")")
    }

    override fun visitCall(call: FirCall) {
        print("(")
        if (mode.renderCallArguments) {
            call.arguments.renderSeparated()
        } else {
            if (call.arguments.isNotEmpty()) {
                print("...")
            }
        }
        print(")")
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

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall) {
        print("@")
        annotationCall.useSiteTarget?.let {
            print(it.name)
            print(":")
        }
        annotationCall.annotationTypeRef.accept(this)
        visitCall(annotationCall)
        if (annotationCall.useSiteTarget == AnnotationUseSiteTarget.FILE) {
            println()
        } else {
            print(" ")
        }
    }

    override fun visitDelegatedConstructorCall(delegatedConstructorCall: FirDelegatedConstructorCall) {
        val dispatchReceiver = delegatedConstructorCall.dispatchReceiver
        if (dispatchReceiver !is FirNoReceiverExpression) {
            dispatchReceiver.accept(this)
            print(".")
        }
        if (delegatedConstructorCall.isSuper) {
            print("super<")
        } else if (delegatedConstructorCall.isThis) {
            print("this<")
        }
        delegatedConstructorCall.constructedTypeRef.accept(this)
        print(">")
        visitCall(delegatedConstructorCall)
    }

    override fun visitTypeRef(typeRef: FirTypeRef) {
        typeRef.annotations.renderAnnotations()
        visitElement(typeRef)
    }

    override fun visitDelegatedTypeRef(delegatedTypeRef: FirDelegatedTypeRef) {
        delegatedTypeRef.typeRef.accept(this)
        print(" by ")
        delegatedTypeRef.delegate?.accept(this)
    }

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef) {
        visitTypeRef(errorTypeRef)
        print("<ERROR TYPE REF: ${errorTypeRef.diagnostic.reason}>")
    }

    override fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef) {
        print("<implicit>")
    }

    override fun visitTypeRefWithNullability(typeRefWithNullability: FirTypeRefWithNullability) {
        if (typeRefWithNullability.isMarkedNullable) {
            print("?")
        }
    }

    override fun visitDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef) {
        dynamicTypeRef.annotations.renderAnnotations()
        print("<dynamic>")
        visitTypeRefWithNullability(dynamicTypeRef)
    }

    override fun visitFunctionTypeRef(functionTypeRef: FirFunctionTypeRef) {
        print("( ")
        functionTypeRef.receiverTypeRef?.let {
            it.accept(this)
            print(".")
        }
        functionTypeRef.valueParameters.renderParameters()
        print(" -> ")
        functionTypeRef.returnTypeRef.accept(this)
        print(" )")
        visitTypeRefWithNullability(functionTypeRef)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        val kind = resolvedTypeRef.functionTypeKind
        val annotations = if (kind.withPrettyRender()) {
            resolvedTypeRef.annotations.dropExtensionFunctionAnnotation()
        } else {
            resolvedTypeRef.annotations
        }
        annotations.renderAnnotations()
        print("R|")
        val coneType = resolvedTypeRef.type
        print(coneType.renderFunctionType(kind, resolvedTypeRef.annotations.any {
            it.isExtensionFunctionAnnotationCall
        }))
        print("|")
    }

    private val FirResolvedTypeRef.functionTypeKind: FunctionClassDescriptor.Kind?
        get() {
            val classId = (type as? ConeClassLikeType)?.lookupTag?.classId ?: return null
            return BuiltInFictitiousFunctionClassFactory.getFunctionalClassKind(
                classId.shortClassName.asString(), classId.packageFqName
            )
        }

    override fun visitUserTypeRef(userTypeRef: FirUserTypeRef) {
        userTypeRef.annotations.renderAnnotations()
        for ((index, qualifier) in userTypeRef.qualifier.withIndex()) {
            if (index != 0) {
                print(".")
            }
            print(qualifier.name)
            if (qualifier.typeArgumentList.typeArguments.isNotEmpty()) {
                print("<")
                qualifier.typeArgumentList.typeArguments.renderSeparated()
                print(">")
            }
        }
        visitTypeRefWithNullability(userTypeRef)
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

    private fun AbstractFirBasedSymbol<*>.render(): String {
        return when (this) {
            is FirCallableSymbol<*> -> callableId.toString()
            is FirClassLikeSymbol<*> -> classId.toString()
            else -> "?"
        }
    }

    override fun visitNamedReference(namedReference: FirNamedReference) {
        val symbol = namedReference.candidateSymbol
        when {
            symbol != null -> {
                print("R?C|${symbol.render()}|")
            }
            namedReference is FirErrorNamedReference -> print("<${namedReference.diagnostic.reason}>#")
            else -> print("${namedReference.name}#")
        }
    }

    override fun visitBackingFieldReference(backingFieldReference: FirBackingFieldReference) {
        print("F|")
        print(backingFieldReference.resolvedSymbol.callableId)
        print("|")
    }

    override fun visitDelegateFieldReference(delegateFieldReference: FirDelegateFieldReference) {
        print("D|")
        print(delegateFieldReference.resolvedSymbol.callableId)
        print("|")
    }

    override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference) {
        print("R|")
        val symbol = resolvedNamedReference.resolvedSymbol
        val isFakeOverride = when (symbol) {
            is FirNamedFunctionSymbol -> symbol.isFakeOverride
            is FirPropertySymbol -> symbol.isFakeOverride
            else -> false
        }

        if (isFakeOverride) {
            print("FakeOverride<")
        }
        print(symbol.render())


        if (resolvedNamedReference is FirResolvedCallableReference) {
            if (resolvedNamedReference.inferredTypeArguments.isNotEmpty()) {
                print("<")

                resolvedNamedReference.inferredTypeArguments.renderTypesSeparated()

                print(">")
            }
        }

        if (isFakeOverride) {
            when (symbol) {
                is FirNamedFunctionSymbol -> {
                    print(": ")
                    symbol.fir.returnTypeRef.accept(this)
                }
                is FirPropertySymbol -> {
                    print(": ")
                    symbol.fir.returnTypeRef.accept(this)
                }
            }
            print(">")
        }
        print("|")
    }

    override fun visitResolvedCallableReference(resolvedCallableReference: FirResolvedCallableReference) {
        visitResolvedNamedReference(resolvedCallableReference)
    }

    override fun visitThisReference(thisReference: FirThisReference) {
        print("this")
        val labelName = thisReference.labelName
        val symbol = thisReference.boundSymbol
        when {
            symbol != null -> print("@R|${symbol.render()}|")
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

    override fun visitQualifiedAccess(qualifiedAccess: FirQualifiedAccess) {
        val explicitReceiver = qualifiedAccess.explicitReceiver
        val dispatchReceiver = qualifiedAccess.dispatchReceiver
        val extensionReceiver = qualifiedAccess.extensionReceiver
        var hasSomeReceiver = true
        when {
            dispatchReceiver !is FirNoReceiverExpression && extensionReceiver !is FirNoReceiverExpression -> {
                print("(")
                dispatchReceiver.accept(this)
                print(", ")
                extensionReceiver.accept(this)
                print(")")
            }
            dispatchReceiver !is FirNoReceiverExpression -> {
                dispatchReceiver.accept(this)
            }
            extensionReceiver !is FirNoReceiverExpression -> {
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
        callableReferenceAccess.annotations.renderAnnotations()
        callableReferenceAccess.explicitReceiver?.accept(this)
        if (callableReferenceAccess.hasQuestionMarkAtLHS && callableReferenceAccess.explicitReceiver !is FirResolvedQualifier) {
            print("?")
        }
        print("::")
        callableReferenceAccess.calleeReference.accept(this)
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
        qualifiedAccessExpression.annotations.renderAnnotations()
        visitQualifiedAccess(qualifiedAccessExpression)
        qualifiedAccessExpression.calleeReference.accept(this)
        qualifiedAccessExpression.typeArguments.renderTypeArguments()
    }

    override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression) {
        visitQualifiedAccessExpression(thisReceiverExpression)
    }

    override fun visitExpressionWithSmartcast(expressionWithSmartcast: FirExpressionWithSmartcast) {
        visitQualifiedAccessExpression(expressionWithSmartcast)
    }

    private fun visitAssignment(operation: FirOperation, rValue: FirExpression) {
        print(operation.operator)
        print(" ")
        rValue.accept(this)
    }

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment) {
        variableAssignment.annotations.renderAnnotations()
        visitQualifiedAccess(variableAssignment)
        variableAssignment.lValue.accept(this)
        print(" ")
        visitAssignment(FirOperation.ASSIGN, variableAssignment.rValue)
    }

    override fun visitAugmentedArraySetCall(augmentedArraySetCall: FirAugmentedArraySetCall) {
        augmentedArraySetCall.annotations.renderAnnotations()
        print("ArraySet:[")
        augmentedArraySetCall.assignCall.accept(this)
        print("]")
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall) {
        functionCall.annotations.renderAnnotations()
        visitQualifiedAccess(functionCall)
        functionCall.calleeReference.accept(this)
        functionCall.typeArguments.renderTypeArguments()
        visitCall(functionCall)
    }

    override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression) {
        print("CMP(${comparisonExpression.operation.operator}, ")
        comparisonExpression.compareToCall.accept(this)
        print(")")
    }

    override fun visitAssignmentOperatorStatement(assignmentOperatorStatement: FirAssignmentOperatorStatement) {
        assignmentOperatorStatement.annotations.renderAnnotations()
        print(assignmentOperatorStatement.operation.operator)
        print("(")
        assignmentOperatorStatement.leftArgument.accept(this@FirRenderer)
        print(", ")
        assignmentOperatorStatement.rightArgument.accept(this@FirRenderer)
        print(")")
    }

    override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall) {
        equalityOperatorCall.annotations.renderAnnotations()
        print(equalityOperatorCall.operation.operator)
        visitCall(equalityOperatorCall)
    }

    override fun visitComponentCall(componentCall: FirComponentCall) {
        visitFunctionCall(componentCall)
    }

    override fun visitGetClassCall(getClassCall: FirGetClassCall) {
        getClassCall.annotations.renderAnnotations()
        print("<getClass>")
        visitCall(getClassCall)
    }

    override fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression) {
        classReferenceExpression.annotations.renderAnnotations()
        print("<getClass>")
        print("(")
        classReferenceExpression.classTypeRef.accept(this)
        print(")")
    }

    override fun visitArrayOfCall(arrayOfCall: FirArrayOfCall) {
        arrayOfCall.annotations.renderAnnotations()
        print("<implicitArrayOf>")
        visitCall(arrayOfCall)
    }

    override fun visitThrowExpression(throwExpression: FirThrowExpression) {
        throwExpression.annotations.renderAnnotations()
        print("throw ")
        throwExpression.exception.accept(this)
    }

    override fun visitErrorExpression(errorExpression: FirErrorExpression) {
        print("ERROR_EXPR(${errorExpression.diagnostic.reason})")
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
        print("Q|")
        val classId = resolvedQualifier.classId
        if (classId != null) {
            print(classId.asString())
        } else {
            print(resolvedQualifier.packageFqName.asString().replace(".", "/"))
        }
        if (resolvedQualifier.isNullableLHSForCallableReference) {
            print("?")
        }
        print("|")
    }

    override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression) {
        binaryLogicExpression.leftOperand.accept(this)
        print(" ${binaryLogicExpression.kind.token} ")
        binaryLogicExpression.rightOperand.accept(this)
    }

    override fun visitErrorNamedReference(errorNamedReference: FirErrorNamedReference) {
        visitNamedReference(errorNamedReference)
    }

    override fun visitRawContractDescription(rawContractDescription: FirRawContractDescription) {
        newLine()
        print("[Contract description]")
        renderInBraces("<", ">") {
            rawContractDescription.contractCall.accept(this)
            newLine()
        }
    }

    override fun visitEffectDeclaration(effectDeclaration: FirEffectDeclaration) {
        newLine()
        print("[Effect declaration]")
        renderInBraces("<", ">") {
            println(buildString { effectDeclaration.effect.accept(ConeContractRenderer(this), null) })
        }
    }

    override fun visitResolvedContractDescription(resolvedContractDescription: FirResolvedContractDescription) {
        newLine()
        println("[R|Contract description]")
        renderInBraces("<", ">") {
            resolvedContractDescription.effects
                .map { it.effect }
                .forEach {
                    println(buildString { it.accept(ConeContractRenderer(this), null) })
                }
        }
    }

    override fun visitContractDescription(contractDescription: FirContractDescription) {
        require(contractDescription is FirEmptyContractDescription)
    }
}
