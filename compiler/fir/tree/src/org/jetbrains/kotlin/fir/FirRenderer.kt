/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.contracts.*
import org.jetbrains.kotlin.fir.contracts.description.ConeContractRenderer
import org.jetbrains.kotlin.fir.contracts.impl.FirEmptyContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.*
import kotlin.reflect.KClass

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
            Visibilities.Private, Visibilities.PrivateToThis, Visibilities.Internal,
            Visibilities.Protected, Visibilities.Public, Visibilities.Local
        )
    }

    data class RenderMode(
        val renderLambdaBodies: Boolean,
        val renderCallArguments: Boolean,
        val renderCallableFqNames: Boolean,
        val renderDeclarationResolvePhase: Boolean,
        val renderAnnotation: Boolean,
        val renderBodies: Boolean = true,
        val renderPropertyAccessors: Boolean = true,
        val renderDeclarationAttributes: Boolean = false,
        val renderDeclarationOrigin: Boolean = false,
    ) {
        companion object {
            val Normal = RenderMode(
                renderLambdaBodies = true,
                renderCallArguments = true,
                renderCallableFqNames = false,
                renderDeclarationResolvePhase = false,
                renderAnnotation = true,
            )

            val WithFqNames = RenderMode(
                renderLambdaBodies = true,
                renderCallArguments = true,
                renderCallableFqNames = true,
                renderDeclarationResolvePhase = false,
                renderAnnotation = true,
            )

            val WithFqNamesExceptAnnotationAndBody = RenderMode(
                renderLambdaBodies = true,
                renderCallArguments = true,
                renderCallableFqNames = true,
                renderDeclarationResolvePhase = false,
                renderAnnotation = false,
                renderBodies = false,
            )

            val WithResolvePhases = RenderMode(
                renderLambdaBodies = true,
                renderCallArguments = true,
                renderCallableFqNames = false,
                renderDeclarationResolvePhase = true,
                renderAnnotation = true,
            )

            val NoBodies = RenderMode(
                renderLambdaBodies = false,
                renderCallArguments = false,
                renderCallableFqNames = false,
                renderDeclarationResolvePhase = false,
                renderAnnotation = false,
                renderBodies = false,
                renderPropertyAccessors = false,
            )

            val WithDeclarationAttributes = RenderMode(
                renderLambdaBodies = true,
                renderCallArguments = true,
                renderCallableFqNames = false,
                renderDeclarationResolvePhase = false,
                renderAnnotation = true,
                renderDeclarationAttributes = true,
            )
        }
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

    fun renderAnnotations(annotationContainer: FirAnnotationContainer) {
        annotationContainer.annotations.renderAnnotations()
    }

    private fun List<FirAnnotationCall>.renderAnnotations() {
        if (!mode.renderAnnotation) return
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
        callableDeclaration.annotations.renderAnnotations()
        visitMemberDeclaration(callableDeclaration)
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
            is FirVariable -> {
                if (!mode.renderCallableFqNames) {
                    print(callableDeclaration.name)
                } else {
                    print(callableDeclaration.symbol.callableId)
                }
            }
        }

        if (callableDeclaration is FirFunction) {
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

    private fun Visibility.asString(effectiveVisibility: EffectiveVisibility? = null): String {
        val itself = when (this) {
            Visibilities.Unknown -> return "public?"
            else -> toString()
        }
        if (effectiveVisibility == null) return itself
        val effectiveAsVisibility = effectiveVisibility.toVisibility()
        if (effectiveAsVisibility == this) return itself
        if (effectiveAsVisibility == Visibilities.Private && this == Visibilities.PrivateToThis) return itself
        if (this !in visibilitiesToRenderEffectiveSet) return itself
        return itself + "[${effectiveVisibility.name}]"
    }

    private fun FirMemberDeclaration.modalityAsString(): String {
        return modality?.name?.toLowerCaseAsciiOnly() ?: run {
            if (this is FirCallableDeclaration && this.isOverride) {
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
        if (memberDeclaration.isExternal) {
            print("external ")
        }
        if (memberDeclaration.isOverride) {
            print("override ")
        }
        if (memberDeclaration.isStatic) {
            print("static ")
        }
        if (memberDeclaration.isInner) {
            print("inner ")
        }

        // `companion/data/fun` modifiers are only valid for FirRegularClass, but we render them to make sure they are not
        // incorrectly loaded for other declarations during deserialization.
        if (memberDeclaration.status.isCompanion) {
            print("companion ")
        }
        if (memberDeclaration.status.isData) {
            print("data ")
        }
        // All Java interfaces are considered `fun` (functional interfaces) for resolution purposes
        // (see JavaSymbolProvider.createFirJavaClass). Don't render `fun` for Java interfaces; it's not a modifier in Java.
        val isJavaInterface =
            memberDeclaration is FirRegularClass && memberDeclaration.classKind == ClassKind.INTERFACE && memberDeclaration.isJava
        if (memberDeclaration.status.isFun && !isJavaInterface) {
            print("fun ")
        }

        if (memberDeclaration.isInline) {
            print("inline ")
        }
        if (memberDeclaration.isOperator) {
            print("operator ")
        }
        if (memberDeclaration.isInfix) {
            print("infix ")
        }
        if (memberDeclaration.isTailRec) {
            print("tailrec ")
        }
        if (memberDeclaration.isSuspend) {
            print("suspend ")
        }
        if (memberDeclaration.isConst) {
            print("const ")
        }
        if (memberDeclaration.isLateInit) {
            print("lateinit ")
        }

        visitDeclaration(memberDeclaration as FirDeclaration)
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

    override fun visitDeclaration(declaration: FirDeclaration) {
        declaration.renderDeclarationData()
        print(
            when (declaration) {
                is FirRegularClass -> declaration.classKind.name.toLowerCaseAsciiOnly().replace("_", " ")
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

    private fun FirDeclaration.renderDeclarationData() {
        renderDeclarationResolvePhaseIfNeeded()
        renderDeclarationAttributesIfNeeded()
        renderDeclarationOriginIfNeeded()
    }

    private fun FirDeclaration.renderDeclarationResolvePhaseIfNeeded() {
        if (mode.renderDeclarationResolvePhase) {
            print("[${resolvePhase}] ")
        }
    }

    private fun FirDeclaration.renderDeclarationAttributesIfNeeded() {
        if (mode.renderDeclarationAttributes && attributes.isNotEmpty()) {
            val attributes = getAttributesWithValues().mapNotNull { (klass, value) ->
                value?.let { klass.simpleName to value.renderAsDeclarationAttributeValue() }
            }.joinToString { (name, value) -> "$name=$value" }
            print("[$attributes] ")
        }
    }

    private fun FirDeclaration.renderDeclarationOriginIfNeeded() {
        if (mode.renderDeclarationOrigin) {
            print("[$origin] ")
        }
    }

    private fun FirDeclaration.getAttributesWithValues(): List<Pair<KClass<out FirDeclarationDataKey>, Any?>> {
        val attributesMap = FirDeclarationDataRegistry.allValuesThreadUnsafeForRendering()
        return attributesMap.entries.sortedBy { it.key.simpleName }.map { (klass, index) -> klass to attributes[index] }
    }

    private fun Any.renderAsDeclarationAttributeValue() = when (this) {
        is FirCallableSymbol<*> -> callableId.toString()
        is FirClassLikeSymbol<*> -> classId.asString()
        else -> toString()
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
        regularClass.annotations.renderAnnotations()
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

    override fun visitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression) {
        anonymousObjectExpression.anonymousObject.accept(this)
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject) {
        anonymousObject.annotations.renderAnnotations()
        print("object : ")
        anonymousObject.superTypeRefs.renderSeparated()
        anonymousObject.declarations.renderDeclarations()
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

    override fun visitField(field: FirField) {
        visitVariable(field)
        println()
    }

    override fun visitProperty(property: FirProperty) {
        visitVariable(property)
        if (property.isLocal) return
        if (!mode.renderPropertyAccessors) return
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
        constructor.renderDeclarationData()
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
        propertyAccessor.renderDeclarationData()
        propertyAccessor.annotations.renderAnnotations()
        print(propertyAccessor.visibility.asString() + " ")
        print(if (propertyAccessor.isInline) "inline " else "")
        print(if (propertyAccessor.isExternal) "external " else "")
        print(if (propertyAccessor.isGetter) "get" else "set")
        propertyAccessor.valueParameters.renderParameters()
        print(": ")
        propertyAccessor.returnTypeRef.accept(this)
        propertyAccessor.renderContractDescription()
        propertyAccessor.body?.renderBody()
    }

    override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression) {
        visitAnonymousFunction(anonymousFunctionExpression.anonymousFunction)
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
        anonymousFunction.renderDeclarationData()
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
        print(" <inline=${anonymousFunction.inlineStatus}")
        if (anonymousFunction.invocationKind != null) {
            print(", kind=${anonymousFunction.invocationKind}")
        }
        print("> ")
        if (mode.renderLambdaBodies) {
            anonymousFunction.body?.renderBody()
        }
    }

    override fun visitFunction(function: FirFunction) {
        function.valueParameters.renderParameters()
        visitDeclaration(function)
        function.body?.renderBody()
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer) {
        print("init")
        anonymousInitializer.body?.renderBody()
    }

    private fun FirBlock.renderBody(additionalStatements: List<FirStatement> = emptyList()) {
        if (!mode.renderBodies) return
        when (this) {
            is FirLazyBlock -> {
                println(" { LAZY_BLOCK }")
            }
            else -> renderInBraces {
                for (statement in additionalStatements + statements) {
                    statement.accept(this@FirRenderer)
                    println()
                }
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
        valueParameter.renderDeclarationData()
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
        if (expression !is FirLazyExpression) {
            expression.annotations.renderAnnotations()
        }
        print(
            when (expression) {
                is FirExpressionStub -> "STUB"
                is FirLazyExpression -> "LAZY_EXPRESSION"
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

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef) {
        errorTypeRef.annotations.renderAnnotations()
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
        functionTypeRef.annotations.dropExtensionFunctionAnnotation().renderAnnotations()
        print("( ")
        if (functionTypeRef.isSuspend) {
            print("suspend ")
        }
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
        print("R|")
        val coneType = resolvedTypeRef.type
        print(coneType.renderFunctionType(kind, resolvedTypeRef.annotations.any {
            it.isExtensionFunctionAnnotationCall
        }))
        print("|")
    }

    override fun visitUserTypeRef(userTypeRef: FirUserTypeRef) {
        userTypeRef.annotations.renderAnnotations()
        if (userTypeRef.customRenderer) {
            print(userTypeRef.toString())
            return
        }
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

    private fun FirBasedSymbol<*>.render(): String {
        return when (this) {
            is FirCallableSymbol<*> -> callableId.toString()
            is FirClassLikeSymbol<*> -> classId.toString()
            else -> "?"
        }
    }

    override fun visitNamedReference(namedReference: FirNamedReference) {
        val symbol = namedReference.candidateSymbol
        when {
            namedReference is FirErrorNamedReference -> print("<${namedReference.diagnostic.reason}>#")
            symbol != null -> print("R?C|${symbol.render()}|")
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
        val isSubstitutionOverride = (symbol.fir as? FirCallableDeclaration)?.isSubstitutionOverride == true

        if (isSubstitutionOverride) {
            print("SubstitutionOverride<")
        }

        print(symbol.unwrapIntersectionOverrides().render())

        if (resolvedNamedReference is FirResolvedCallableReference) {
            if (resolvedNamedReference.inferredTypeArguments.isNotEmpty()) {
                print("<")

                resolvedNamedReference.inferredTypeArguments.renderTypesSeparated()

                print(">")
            }
        }

        if (isSubstitutionOverride) {
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

    private fun FirBasedSymbol<*>.unwrapIntersectionOverrides(): FirBasedSymbol<*> {
        (this as? FirCallableSymbol<*>)?.baseForIntersectionOverride?.let { return it.unwrapIntersectionOverrides() }
        return this
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

    override fun visitImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall) {
        visitFunctionCall(implicitInvokeCall)
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

    override fun visitLegacyRawContractDescription(legacyRawContractDescription: FirLegacyRawContractDescription) {
        newLine()
        print("[Contract description]")
        renderInBraces("<", ">") {
            legacyRawContractDescription.contractCall.accept(this)
            newLine()
        }
    }

    override fun visitRawContractDescription(rawContractDescription: FirRawContractDescription) {
        newLine()
        print("[Contract description]")
        renderInBraces("<", ">") {
            rawContractDescription.rawEffects.renderSeparatedWithNewlines()
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
