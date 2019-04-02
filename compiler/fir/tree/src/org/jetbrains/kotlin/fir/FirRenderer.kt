/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.expressions.impl.FirExpressionStub
import org.jetbrains.kotlin.fir.expressions.impl.FirUnitExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirWhenSubjectExpression
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.Printer

fun FirElement.renderWithType(): String = buildString {
    append(this@renderWithType)
    append(": ")
    this@renderWithType.accept(FirRenderer(this))
}
fun FirElement.render(): String = buildString { this@render.accept(FirRenderer(this)) }


fun ConeKotlinType.render(): String {
    return when (this) {
        is ConeKotlinErrorType -> "error: $reason"
        is ConeClassErrorType -> "class error: $reason"
        is ConeCapturedType -> "captured type: lowerType = ${lowerType?.render()}"
        is ConeClassLikeType -> {
            val sb = StringBuilder()
            sb.append(lookupTag.classId.asString())
            if (typeArguments.isNotEmpty()) {
                sb.append(typeArguments.joinToString(prefix = "<", postfix = ">") {
                    when (it) {
                        ConeStarProjection -> "*"
                        is ConeKotlinTypeProjectionIn -> "in ${it.type.render()}"
                        is ConeKotlinTypeProjectionOut -> "out ${it.type.render()}"
                        is ConeKotlinType -> it.render()
                    }
                })
            }
            sb.toString()
        }
        is ConeTypeParameterType -> {
            lookupTag.name.asString()
        }
        is ConeFunctionType -> {
            buildString {
                receiverType?.let {
                    append(it.render())
                    append(".")
                }
                append("(")
                parameterTypes.joinTo(this) { it.render() }
                append(") -> ")
                append(returnType.render())
            }
        }
        is ConeFlexibleType -> {
            buildString {
                append("ft<")
                append(lowerBound.render())
                append(lowerBound.nullability.suffix)
                append(", ")
                append(upperBound.render())
                append(upperBound.nullability.suffix)
                append(">")
            }
        }
    }
}

class FirRenderer(builder: StringBuilder) : FirVisitorVoid() {
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
        super.visitFile(file)
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

    override fun visitCallableDeclaration(callableDeclaration: FirCallableDeclaration) {
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
        if (callableDeclaration is FirNamedDeclaration) {
            print(callableDeclaration.name)
        }

        if (callableDeclaration is FirFunction) {
            callableDeclaration.valueParameters.renderParameters()
        }
        print(": ")
        callableDeclaration.returnTypeRef.accept(this)
    }

    private fun Visibility.asString() =
        when (this) {
            Visibilities.UNKNOWN -> "public?"
            else -> toString()
        }

    private fun FirMemberDeclaration.modalityAsString(): String {
        return modality?.name?.toLowerCase() ?: run {
            if (this is FirCallableMemberDeclaration && this.isOverride) {
                "open?"
            } else {
                "final?"
            }
        }
    }

    override fun visitMemberDeclaration(memberDeclaration: FirMemberDeclaration) {
        memberDeclaration.annotations.renderAnnotations()
        if (memberDeclaration.typeParameters.isNotEmpty()) {
            print("<")
            memberDeclaration.typeParameters.renderSeparated()
            print("> ")
        }
        print(memberDeclaration.visibility.asString() + " " + memberDeclaration.modalityAsString() + " ")
        if (memberDeclaration.isExpect) {
            print("expect ")
        }
        if (memberDeclaration.isActual) {
            print("actual ")
        }
        if (memberDeclaration is FirCallableMemberDeclaration) {
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
        } else if (memberDeclaration is FirNamedFunction) {
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

        visitNamedDeclaration(memberDeclaration)
    }

    override fun visitNamedDeclaration(namedDeclaration: FirNamedDeclaration) {
        visitDeclaration(namedDeclaration)
        if (namedDeclaration !is FirCallableDeclaration) { // Handled by visitCallableDeclaration
            print(" " + namedDeclaration.name)
        }
    }

    override fun visitDeclaration(declaration: FirDeclaration) {
        print(
            when (declaration) {
                is FirRegularClass -> declaration.classKind.name.toLowerCase().replace("_", " ")
                is FirTypeAlias -> "typealias"
                is FirNamedFunction -> "fun"
                is FirProperty -> if (declaration.isVal) "val" else "var"
                is FirVariable -> if (declaration.isVal) "lval" else "lvar"
                else -> "unknown"
            }
        )
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry) {
        visitRegularClass(enumEntry)
    }

    private fun FirDeclarationContainer.renderDeclarations() {
        renderInBraces {
            for (declaration in declarations) {
                declaration.accept(this@FirRenderer)
                println()
            }
        }
    }

    fun renderInBraces(f: () -> Unit) {
        println(" {")
        pushIndent()
        f()
        popIndent()
        println("}")
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
        regularClass.renderDeclarations()
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject) {
        anonymousObject.annotations.renderAnnotations()
        print("object : ")
        anonymousObject.superTypeRefs.renderSeparated()
        anonymousObject.renderDeclarations()
    }

    override fun visitVariable(variable: FirVariable) {
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

    override fun visitProperty(property: FirProperty) {
        visitVariable(property)
        println()
        pushIndent()
        property.getter.accept(this)
        if (property.getter.body == null) {
            println()
        }
        if (property.isVar) {
            property.setter.accept(this)
            if (property.setter.body == null) {
                println()
            }
        }
        popIndent()
    }

    override fun visitNamedFunction(namedFunction: FirNamedFunction) {
        visitCallableDeclaration(namedFunction)
        namedFunction.body?.accept(this)
        if (namedFunction.body == null) {
            println()
        }
    }

    override fun visitConstructor(constructor: FirConstructor) {
        constructor.annotations.renderAnnotations()
        print(constructor.visibility.asString() + " constructor")
        constructor.valueParameters.renderParameters()
        constructor.delegatedConstructor?.accept(this)
        constructor.body?.accept(this)
        if (constructor.body == null) {
            println()
        }
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor) {
        propertyAccessor.annotations.renderAnnotations()
        print(propertyAccessor.visibility.asString() + " ")
        print(if (propertyAccessor.isGetter) "get" else "set")
        propertyAccessor.valueParameters.renderParameters()
        print(": ")
        propertyAccessor.returnTypeRef.accept(this)
        propertyAccessor.body?.accept(this)
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
        anonymousFunction.body?.accept(this)
    }

    override fun visitFunction(function: FirFunction) {
        function.valueParameters.renderParameters()
        visitDeclarationWithBody(function)
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer) {
        print("init")
        anonymousInitializer.body?.accept(this)
    }

    override fun visitDeclarationWithBody(declarationWithBody: FirDeclarationWithBody) {
        visitDeclaration(declarationWithBody)
        declarationWithBody.body?.accept(this)
    }

    override fun visitBlock(block: FirBlock) {
        renderInBraces {
            for (statement in block.statements) {
                statement.accept(this)
                println()
            }
        }
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
        if (typeParameter.bounds.isNotEmpty()) {
            print(" : ")
            typeParameter.bounds.renderSeparated()
        }
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
        visitElement(statement)
    }

    override fun visitReturnExpression(returnExpression: FirReturnExpression) {
        returnExpression.annotations.renderAnnotations()
        print("^")
        val target = returnExpression.target
        val labeledElement = target.labeledElement
        if (labeledElement is FirNamedFunction) {
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

    private fun visitLoopJump(jump: FirJump<FirLoop>) {
        val target = jump.target
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
                is FirWhenSubjectExpression -> "\$subj\$"
                is FirElseIfTrueCondition -> "else"
                else -> "??? ${expression.javaClass}"
            }
        )
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>) {
        constExpression.annotations.renderAnnotations()
        print("${constExpression.kind}(${constExpression.value})")
    }

    override fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression) {
        print(namedArgumentExpression.name)
        print(" = ")
        namedArgumentExpression.expression.accept(this)
    }

    override fun visitCall(call: FirCall) {
        print("(")
        call.arguments.renderSeparated()
        print(")")
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
        if (delegatedConstructorCall.isSuper) {
            print(": super<")
        } else if (delegatedConstructorCall.isThis) {
            print(": this<")
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
        print("<ERROR TYPE REF: ${errorTypeRef.reason}>")
    }

    override fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef) {
        print(if (implicitTypeRef is FirImplicitBuiltinTypeRef) "${implicitTypeRef.id}" else "<implicit>")
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

    private fun ConeClassifierSymbol.asString(): String {
        return when (this) {
            is ConeClassLikeSymbol -> classId.asString()
            is FirTypeParameterSymbol -> fir.name.asString()
            else -> "Unsupported: ${this::class}"
        }
    }


    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        resolvedTypeRef.annotations.renderAnnotations()
        print("R|")
        val coneType = resolvedTypeRef.type
        print(coneType.render())
        print("|")
        if (coneType !is ConeKotlinErrorType && coneType !is ConeClassErrorType) {
            print(coneType.nullability.suffix)
        }
    }

    override fun visitUserTypeRef(userTypeRef: FirUserTypeRef) {
        userTypeRef.annotations.renderAnnotations()
        for ((index, qualifier) in userTypeRef.qualifier.withIndex()) {
            if (index != 0) {
                print(".")
            }
            print(qualifier.name)
            if (qualifier.typeArguments.isNotEmpty()) {
                print("<")
                qualifier.typeArguments.renderSeparated()
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

    override fun visitNamedReference(namedReference: FirNamedReference) {
        print("${namedReference.name}#")
    }

    override fun visitResolvedCallableReference(resolvedCallableReference: FirResolvedCallableReference) {
        print("R|")
        val isFakeOverride = (resolvedCallableReference.callableSymbol as? FirFunctionSymbol)?.isFakeOverride == true

        if (isFakeOverride) {
            print("FakeOverride<")
        }
        val symbol = resolvedCallableReference.callableSymbol
        print(symbol.callableId)
        if (isFakeOverride) {
            when (symbol) {
                is FirFunctionSymbol -> {
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

    override fun visitThisReference(thisReference: FirThisReference) {
        print("this")
        val labelName = thisReference.labelName
        if (labelName != null) {
            print("@$labelName")
        } else {
            print("#")
        }
    }

    override fun visitSuperReference(superReference: FirSuperReference) {
        print("super<")
        superReference.superTypeRef.accept(this)
        print(">")
    }

    override fun visitQualifiedAccess(qualifiedAccess: FirQualifiedAccess) {
        val explicitReceiver = qualifiedAccess.explicitReceiver
        if (explicitReceiver != null) {
            explicitReceiver.accept(this)
            if (qualifiedAccess.safe) {
                print("?.")
            } else {
                print(".")
            }
        }
    }

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess) {
        callableReferenceAccess.annotations.renderAnnotations()
        callableReferenceAccess.explicitReceiver?.accept(this)
        print("::")
        callableReferenceAccess.calleeReference.accept(this)
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
        qualifiedAccessExpression.annotations.renderAnnotations()
        visitQualifiedAccess(qualifiedAccessExpression)
        qualifiedAccessExpression.calleeReference.accept(this)
    }

    override fun visitAssignment(assignment: FirAssignment) {
        print(assignment.operation.operator)
        print(" ")
        assignment.rValue.accept(this)
    }

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment) {
        variableAssignment.annotations.renderAnnotations()
        visitQualifiedAccess(variableAssignment)
        variableAssignment.lValue.accept(this)
        print(" ")
        visitAssignment(variableAssignment)
    }

    override fun visitArraySetCall(arraySetCall: FirArraySetCall) {
        arraySetCall.annotations.renderAnnotations()
        visitQualifiedAccess(arraySetCall)
        arraySetCall.lValue.accept(this)
        print("[")
        arraySetCall.indexes.renderSeparated()
        print("] ")
        visitAssignment(arraySetCall)
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall) {
        functionCall.annotations.renderAnnotations()
        visitQualifiedAccess(functionCall)
        functionCall.calleeReference.accept(this)
        if (functionCall.typeArguments.isNotEmpty()) {
            print("<")
            functionCall.typeArguments.renderSeparated()
            print(">")
        }
        visitCall(functionCall)
    }

    override fun visitOperatorCall(operatorCall: FirOperatorCall) {
        operatorCall.annotations.renderAnnotations()
        print(operatorCall.operation.operator)
        if (operatorCall is FirTypeOperatorCall) {
            print("/")
            operatorCall.conversionTypeRef.accept(this)
        }
        visitCall(operatorCall)
    }

    override fun visitArrayGetCall(arrayGetCall: FirArrayGetCall) {
        arrayGetCall.annotations.renderAnnotations()
        arrayGetCall.array.accept(this)
        print("[")
        arrayGetCall.arguments.renderSeparated()
        print("]")
    }

    override fun visitComponentCall(componentCall: FirComponentCall) {
        componentCall.annotations.renderAnnotations()
        componentCall.explicitReceiver.accept(this)
        print(".component${componentCall.componentIndex}()")
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
        print("ERROR_EXPR(${errorExpression.reason})")
    }
}
