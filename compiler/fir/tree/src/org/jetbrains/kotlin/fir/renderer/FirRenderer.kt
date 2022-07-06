/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
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
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.*

open class FirRenderer private constructor(
    builder: StringBuilder,
    protected val mode: RenderMode,
    components: FirComponentsImpl
) : FirPrinter(builder) {

    companion object {
        private val visibilitiesToRenderEffectiveSet = setOf(
            Visibilities.Private, Visibilities.PrivateToThis, Visibilities.Internal,
            Visibilities.Protected, Visibilities.Public, Visibilities.Local
        )
    }

    constructor(builder: StringBuilder = StringBuilder(), mode: RenderMode = RenderMode.Normal) : this(builder, mode, FirComponentsImpl())

    fun renderElementAsString(element: FirElement): String {
        element.accept(visitor)
        return toString()
    }

    fun renderAsCallableDeclarationString(callableDeclaration: FirCallableDeclaration): String {
        visitor.visitCallableDeclaration(callableDeclaration)
        return toString()
    }

    fun renderMemberDeclarationClass(firClass: FirClass) {
        visitor.visitMemberDeclaration(firClass)
    }

    private class FirComponentsImpl : FirRendererComponents {
        override var annotationRenderer: FirAnnotationRenderer? = null

        override var bodyRenderer: FirBodyRenderer? = null

        override lateinit var declarationRenderer: FirDeclarationRenderer

        override lateinit var typeRenderer: ConeTypeRenderer

        override lateinit var visitor: Visitor

        override lateinit var printer: FirPrinter
    }

    data class RenderMode(
        val renderCallArguments: Boolean,
        val renderCallableFqNames: Boolean,
        val renderDeclarationResolvePhase: Boolean,
        val renderAnnotation: Boolean,
        val renderBodies: Boolean = true,
        val renderPropertyAccessors: Boolean = true,
        val renderDeclarationAttributes: Boolean = false,
        val renderPackageDirective: Boolean = false,
        val renderNestedDeclarations: Boolean = true,
        val renderDefaultParameterValues: Boolean = true,
        val renderDetailedTypeReferences: Boolean = true,
        val renderAllModifiers: Boolean = true,
    ) {
        companion object {
            val Normal = RenderMode(
                renderCallArguments = true,
                renderCallableFqNames = false,
                renderDeclarationResolvePhase = false,
                renderAnnotation = true,
            )

            val WithFqNames = RenderMode(
                renderCallArguments = true,
                renderCallableFqNames = true,
                renderDeclarationResolvePhase = false,
                renderAnnotation = true,
            )

            val WithFqNamesExceptAnnotationAndBody = RenderMode(
                renderCallArguments = true,
                renderCallableFqNames = true,
                renderDeclarationResolvePhase = false,
                renderAnnotation = false,
                renderBodies = false,
            )

            val WithResolvePhases = RenderMode(
                renderCallArguments = true,
                renderCallableFqNames = false,
                renderDeclarationResolvePhase = true,
                renderAnnotation = true,
            )

            val NoBodies = RenderMode(
                renderCallArguments = false,
                renderCallableFqNames = false,
                renderDeclarationResolvePhase = false,
                renderAnnotation = false,
                renderBodies = false,
                renderPropertyAccessors = false,
            )

            val DeclarationHeader = RenderMode(
                renderCallArguments = false,
                renderCallableFqNames = false,
                renderDeclarationResolvePhase = false,
                renderAnnotation = true,
                renderBodies = false,
                renderPropertyAccessors = false,
                renderDeclarationAttributes = false,
                renderPackageDirective = false,
                renderNestedDeclarations = false,
                renderDefaultParameterValues = false,
                renderDetailedTypeReferences = false,
                renderAllModifiers = false,
            )

            val WithDeclarationAttributes = RenderMode(
                renderCallArguments = true,
                renderCallableFqNames = false,
                renderDeclarationResolvePhase = false,
                renderAnnotation = true,
                renderDeclarationAttributes = true,
            )
        }
    }

    private val visitor = Visitor()
    private val annotationRenderer = when {
        mode.renderAnnotation ->
            if (mode.renderCallArguments) FirAnnotationWithArgumentsRenderer(components) else FirAnnotationRenderer(components)
        else ->
            null
    }
    private val bodyRenderer =
        if (mode.renderBodies) FirBodyRenderer(components) else null

    private val declarationRenderer = when {
        mode.renderDeclarationAttributes && mode.renderDeclarationResolvePhase ->
            FirDeclarationRendererWithAttributesAndResolvePhase(components)
        mode.renderDeclarationAttributes ->
            FirDeclarationRendererWithAttributes(components)
        mode.renderDeclarationResolvePhase ->
            FirDeclarationRendererWithResolvePhase(components)
        else -> FirDeclarationRenderer(components)
    }

    @Suppress("LeakingThis")
    private val typeRenderer =
        if (mode.renderDetailedTypeReferences) ConeTypeRendererForDebugging(builder) else ConeTypeRenderer(builder)

    init {
        components.visitor = visitor
        components.annotationRenderer = annotationRenderer
        components.bodyRenderer = bodyRenderer
        components.declarationRenderer = declarationRenderer
        components.typeRenderer = typeRenderer
        @Suppress("LeakingThis")
        components.printer = this
    }

    private fun List<FirElement>.renderSeparated() {
        for ((index, element) in this.withIndex()) {
            if (index > 0) {
                print(", ")
            }
            element.accept(visitor)
        }
    }

    private fun List<FirElement>.renderSeparatedWithNewlines() {
        for ((index, element) in this.withIndex()) {
            if (index > 0) {
                print(",")
                newLine()
            }
            element.accept(visitor)
        }
    }

    private fun List<ConeKotlinType>.renderTypesSeparated() {
        for ((index, element) in this.withIndex()) {
            if (index > 0) {
                print(", ")
            }
            print(element.renderForDebugging())
        }
    }


    private fun List<FirValueParameter>.renderParameters() {
        print("(")
        renderSeparated()
        print(")")
    }

    private fun Variance.renderVariance() {
        label.let {
            print(it)
            if (it.isNotEmpty()) {
                print(" ")
            }
        }
    }

    private fun renderContexts(contextReceivers: List<FirContextReceiver>) {
        if (contextReceivers.isEmpty()) return
        print("context(")
        contextReceivers.renderSeparated()
        print(")")
        newLine()
    }

    private fun FirDeclaration.renderContractDescription() {
        val contractDescription = (this as? FirContractDescriptionOwner)?.contractDescription ?: return
        pushIndent()
        contractDescription.accept(visitor)
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

    protected fun List<FirDeclaration>.renderDeclarations() {
        renderInBraces {
            for (declaration in this) {
                declaration.accept(visitor)
                println()
            }
        }
    }

    fun renderSupertypes(regularClass: FirRegularClass) {
        if (regularClass.superTypeRefs.isNotEmpty()) {
            print(" : ")
            regularClass.superTypeRefs.renderSeparated()
        }
    }

    fun renderAnnotations(annotationContainer: FirAnnotationContainer) {
        annotationRenderer?.render(annotationContainer)
    }

    protected open fun renderClassDeclarations(regularClass: FirRegularClass) {
        if (mode.renderNestedDeclarations) {
            regularClass.declarations.renderDeclarations()
        }
    }

    private fun visitAssignment(operation: FirOperation, rValue: FirExpression) {
        print(operation.operator)
        print(" ")
        rValue.accept(visitor)
    }

    inner class Visitor internal constructor() : FirVisitorVoid() {

        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitFile(file: FirFile) {
            println("FILE: ${file.name}")
            pushIndent()
            annotationRenderer?.render(file)
            visitPackageDirective(file.packageDirective)
            file.imports.forEach { it.accept(this) }
            file.declarations.forEach { it.accept(this) }
            popIndent()
        }

        override fun visitAnnotation(annotation: FirAnnotation) {
            annotationRenderer?.renderAnnotation(annotation)
        }

        override fun visitAnnotationCall(annotationCall: FirAnnotationCall) {
            annotationRenderer?.renderAnnotation(annotationCall)
        }

        override fun visitCallableDeclaration(callableDeclaration: FirCallableDeclaration) {
            renderContexts(callableDeclaration.contextReceivers)
            annotationRenderer?.render(callableDeclaration)
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
                else -> {}
            }

            if (callableDeclaration is FirFunction) {
                callableDeclaration.valueParameters.renderParameters()
            }
            print(": ")
            callableDeclaration.returnTypeRef.accept(this)
            callableDeclaration.renderContractDescription()
        }

        override fun visitContextReceiver(contextReceiver: FirContextReceiver) {
            contextReceiver.customLabelName?.let {
                print(it.asString() + "@")
            }

            contextReceiver.typeRef.accept(this)
        }

        override fun visitTypeParameterRef(typeParameterRef: FirTypeParameterRef) {
            typeParameterRef.symbol.fir.accept(this)
        }

        override fun visitMemberDeclaration(memberDeclaration: FirMemberDeclaration) {
            if (mode.renderAllModifiers && (memberDeclaration !is FirProperty || !memberDeclaration.isLocal)) {
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
            if (mode.renderAllModifiers) {
                if (memberDeclaration.isExternal) {
                    print("external ")
                }
                if (memberDeclaration.isOverride) {
                    print("override ")
                }
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

            if (mode.renderAllModifiers) {
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
            declarationRenderer.render(declaration)
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

        override fun visitRegularClass(regularClass: FirRegularClass) {
            renderContexts(regularClass.contextReceivers)
            annotationRenderer?.render(regularClass)
            visitMemberDeclaration(regularClass)
            renderSupertypes(regularClass)
            renderClassDeclarations(regularClass)
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
            annotationRenderer?.render(anonymousObject)
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

            if (property.hasExplicitBackingField) {
                property.backingField?.accept(this)
                println()
            }

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

        override fun visitBackingField(backingField: FirBackingField) {
            print(backingField.visibility.asString() + " ")
            print("<explicit backing field>: ")
            backingField.returnTypeRef.accept(this)

            backingField.initializer?.let {
                print(" = ")
                it.accept(this)
            }
        }

        override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
            visitCallableDeclaration(simpleFunction)
            bodyRenderer?.render(simpleFunction)
            if (simpleFunction.body == null) {
                println()
            }
        }

        override fun visitConstructor(constructor: FirConstructor) {
            annotationRenderer?.render(constructor)
            // we can't access session.effectiveVisibilityResolver from here!
            // print(constructor.visibility.asString(constructor.getEffectiveVisibility(...)) + " ")
            print(constructor.visibility.asString() + " ")
            if (constructor.isExpect) {
                print("expect ")
            }
            if (constructor.isActual) {
                print("actual ")
            }
            declarationRenderer.render(constructor)

            constructor.dispatchReceiverType?.let {
                typeRenderer.render(it)
                print(".")
            }
            print("constructor")
            constructor.typeParameters.renderTypeParameters()
            constructor.valueParameters.renderParameters()
            print(": ")
            constructor.returnTypeRef.accept(this)
            val body = constructor.body
            val delegatedConstructor = constructor.delegatedConstructor
            if (body == null && mode.renderBodies) {
                if (delegatedConstructor != null) {
                    renderInBraces {
                        delegatedConstructor.accept(this)
                        println()
                    }
                } else {
                    println()
                }
            }
            bodyRenderer?.renderBody(body, listOfNotNull<FirStatement>(delegatedConstructor))
        }

        override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor) {
            declarationRenderer.render(propertyAccessor)
            annotationRenderer?.render(propertyAccessor)
            print(propertyAccessor.visibility.asString() + " ")
            print(if (propertyAccessor.isInline) "inline " else "")
            print(if (propertyAccessor.isExternal) "external " else "")
            print(if (propertyAccessor.isGetter) "get" else "set")
            propertyAccessor.valueParameters.renderParameters()
            print(": ")
            propertyAccessor.returnTypeRef.accept(this)
            propertyAccessor.renderContractDescription()
            bodyRenderer?.render(propertyAccessor)
        }

        override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression) {
            visitAnonymousFunction(anonymousFunctionExpression.anonymousFunction)
        }

        override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
            declarationRenderer.render(anonymousFunction)
            annotationRenderer?.render(anonymousFunction)
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
            if (anonymousFunction.valueParameters.isEmpty() &&
                anonymousFunction.hasExplicitParameterList &&
                anonymousFunction.returnTypeRef is FirImplicitTypeRef
            ) {
                print("(<no-parameters>)")
            }
            anonymousFunction.valueParameters.renderParameters()
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
            function.valueParameters.renderParameters()
            visitDeclaration(function)
            bodyRenderer?.render(function)
        }

        override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer) {
            print("init")
            bodyRenderer?.renderBody(anonymousInitializer.body)
        }

        override fun visitBlock(block: FirBlock) {
            bodyRenderer?.renderBody(block)
        }

        override fun visitTypeAlias(typeAlias: FirTypeAlias) {
            annotationRenderer?.render(typeAlias)
            visitMemberDeclaration(typeAlias)
            print(" = ")
            typeAlias.expandedTypeRef.accept(this)
            println()
        }

        override fun visitTypeParameter(typeParameter: FirTypeParameter) {
            annotationRenderer?.render(typeParameter)
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
            safeCallExpression.selector.accept(this)
            print(" }")
        }

        override fun visitCheckedSafeCallSubject(checkedSafeCallSubject: FirCheckedSafeCallSubject) {
            print("\$subj\$")
        }

        override fun visitValueParameter(valueParameter: FirValueParameter) {
            declarationRenderer.render(valueParameter)
            annotationRenderer?.render(valueParameter)
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
                if (mode.renderDefaultParameterValues) {
                    it.accept(this)
                } else {
                    print("...")
                }
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
            if (expression !is FirLazyExpression) {
                annotationRenderer?.render(expression)
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
            annotationRenderer?.render(constExpression)
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
            annotationRenderer?.render(typeRef)
            visitElement(typeRef)
        }

        override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef) {
            annotationRenderer?.render(errorTypeRef)
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
            annotationRenderer?.render(dynamicTypeRef)
            print("<dynamic>")
            visitTypeRefWithNullability(dynamicTypeRef)
        }

        override fun visitFunctionTypeRef(functionTypeRef: FirFunctionTypeRef) {
            if (functionTypeRef.contextReceiverTypeRefs.isNotEmpty()) {
                print("context(")
                functionTypeRef.contextReceiverTypeRefs.renderSeparated()
                print(")")
            }

            annotationRenderer?.renderAnnotations(functionTypeRef.annotations.dropExtensionFunctionAnnotation())
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
            typeRenderer.renderAsPossibleFunctionType(resolvedTypeRef.type)
        }

        override fun visitUserTypeRef(userTypeRef: FirUserTypeRef) {
            annotationRenderer?.render(userTypeRef)
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
            print(backingFieldReference.resolvedSymbol.fir.propertySymbol.callableId)
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
            visitQualifiedAccess(qualifiedAccessExpression)
            qualifiedAccessExpression.calleeReference.accept(this)
            qualifiedAccessExpression.typeArguments.renderTypeArguments()
        }

        override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
            visitQualifiedAccessExpression(propertyAccessExpression)
        }

        override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression) {
            visitQualifiedAccessExpression(thisReceiverExpression)
        }

        override fun visitExpressionWithSmartcast(expressionWithSmartcast: FirExpressionWithSmartcast) {
            visitQualifiedAccessExpression(expressionWithSmartcast)
        }

        override fun visitVariableAssignment(variableAssignment: FirVariableAssignment) {
            annotationRenderer?.render(variableAssignment)
            visitQualifiedAccess(variableAssignment)
            variableAssignment.lValue.accept(this)
            print(" ")
            visitAssignment(FirOperation.ASSIGN, variableAssignment.rValue)
        }

        override fun visitAugmentedArraySetCall(augmentedArraySetCall: FirAugmentedArraySetCall) {
            annotationRenderer?.render(augmentedArraySetCall)
            print("ArraySet:[")
            augmentedArraySetCall.lhsGetCall.accept(this)
            print(" ")
            print(augmentedArraySetCall.operation.operator)
            print(" ")
            augmentedArraySetCall.rhs.accept(this)
            print("]")
        }

        override fun visitFunctionCall(functionCall: FirFunctionCall) {
            annotationRenderer?.render(functionCall)
            visitQualifiedAccess(functionCall)
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

        override fun visitAssignmentOperatorStatement(assignmentOperatorStatement: FirAssignmentOperatorStatement) {
            annotationRenderer?.render(assignmentOperatorStatement)
            print(assignmentOperatorStatement.operation.operator)
            print("(")
            assignmentOperatorStatement.leftArgument.accept(visitor)
            print(", ")
            assignmentOperatorStatement.rightArgument.accept(visitor)
            print(")")
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
            annotationRenderer?.render(getClassCall)
            print("<getClass>")
            visitCall(getClassCall)
        }

        override fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression) {
            annotationRenderer?.render(classReferenceExpression)
            print("<getClass>")
            print("(")
            classReferenceExpression.classTypeRef.accept(this)
            print(")")
        }

        override fun visitArrayOfCall(arrayOfCall: FirArrayOfCall) {
            annotationRenderer?.render(arrayOfCall)
            print("<implicitArrayOf>")
            visitCall(arrayOfCall)
        }

        override fun visitThrowExpression(throwExpression: FirThrowExpression) {
            annotationRenderer?.render(throwExpression)
            print("throw ")
            throwExpression.exception.accept(this)
        }

        override fun visitErrorExpression(errorExpression: FirErrorExpression) {
            print("ERROR_EXPR(${errorExpression.diagnostic.reason})")
        }

        override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier) {
            annotationRenderer?.render(resolvedQualifier)
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

        override fun visitPackageDirective(packageDirective: FirPackageDirective) {
            if (mode.renderPackageDirective) {
                if (!packageDirective.packageFqName.isRoot) {
                    println("package ${packageDirective.packageFqName.asString()}")
                    println()
                }
            }
        }
    }
}
