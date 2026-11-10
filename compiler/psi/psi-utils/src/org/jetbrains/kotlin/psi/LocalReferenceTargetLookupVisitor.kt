/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi

import org.jetbrains.annotations.TestOnly
import com.intellij.psi.util.siblings
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.LookupLocallyInternals.ContextKind
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter

/**
 * Performs local, PSI-only name lookups.
 *
 * If not null, the returned declaration is what the compiler would resolve the name to.
 *
 * @return `null` if the lookup failed / cannot be performed, otherwise the found local declaration.
 *
 * This is available as a local-only optimization. It makes use of the fact that local scopes always have the highest
 * priority in resolution.
 *
 * Note: does not work in dangling files.
 */
@KtExperimentalApi
fun KtNameReferenceExpression.lookupLocally(): KtNamedDeclaration? {
    val containingFile = containingFile
    if (containingFile.originalFile != containingFile) {
        return null
    }

    val contextKind = LookupLocallyInternals.getContextKind(this) ?: return null

    return LocalReferenceTargetLookupVisitor(this, contextKind).lookup()
}

private val KtElement.nonContainerParent: KtElement?
    get() {
        var e = parent ?: context
        while (e != null && (e is KtContainerNode || e is KtCodeFragment)) {
            e = e.parent ?: e.context
        }
        return e as? KtElement
    }

private class LocalReferenceTargetLookupVisitor(val element: KtNameReferenceExpression, val contextKind: ContextKind) :
    KtVisitor<Unit, VisitorContext>() {
    fun lookup(): KtNamedDeclaration? {
        val availableNameElementSequence = sequence {
            // invariant: current is always yielded
            var current: KtElement = element
            var lastDirection = LastDirection.INITIAL
            yield(current to lastDirection)

            while (true) {
                if (current.isStopElement(lastDirection)) break

                when (val p = current.nonContainerParent) {
                    is KtBlockExpression -> {
                        for (sibling in current.siblings(forward = false, withSelf = false).filterIsInstance<KtElement>()) {
                            yield(sibling to LastDirection.BACKWARDS)
                        }
                        current = p
                        lastDirection = PARENT
                        yield(current to lastDirection)
                    }

                    null -> break

                    else -> {
                        current = p
                        lastDirection = PARENT
                        yield(current to lastDirection)
                    }
                }
            }
        }

        var previousElement: KtElement? = null

        for ([element, direction] in availableNameElementSequence) {
            element.accept(this, VisitorContext(direction, previousElement))
            if (_stopResolution) return null

            _found?.let { return it }

            previousElement = element
        }

        return null
    }

    private var _found: KtNamedDeclaration? = null
    private val name: Name = element.getReferencedNameAsName()

    private var constructorParametersAllowed: Boolean = false
    private var _stopResolution: Boolean = false
    private fun stopResolution() {
        _stopResolution = true
    }

    private val KtElement.isScriptTopLevelElement: Boolean
        get() = parent is KtBlockExpression && parent.parent is KtScript

    private fun KtElement.isStopElementInValueContext(lastDirection: LastDirection): Boolean =
        lastDirection == PARENT &&
                when (this) {
                    is KtNamedFunction -> isTopLevel
                    is KtProperty -> isTopLevel
                    is KtClassOrObject -> isTopLevel() || this is KtObjectDeclaration && this.isCompanion()
                    else -> false
                }
                || isScriptTopLevelElement

    private fun KtElement.isStopElementInTypeContext(lastDirection: LastDirection): Boolean =
        lastDirection == PARENT &&
                when (this) {
                    is KtNamedFunction -> !this.isLocal
                    is KtProperty -> !this.isLocal
                    is KtClassOrObject,
                    is KtAnonymousInitializer,
                    is KtConstructor<*>,
                        -> true
                    else -> false
                }
                || isScriptTopLevelElement

    private fun KtElement.isStopElement(lastDirection: LastDirection): Boolean = when (contextKind) {
        VALUE -> isStopElementInValueContext(lastDirection)
        TYPE -> isStopElementInTypeContext(lastDirection)
        VALUE_OR_TYPE -> isStopElementInValueContext(lastDirection) || isStopElementInTypeContext(lastDirection)
    }

    override fun visitKtElement(element: KtElement, data: VisitorContext?) {
        if (data?.previousElement is KtClass) constructorParametersAllowed = false
    }

    override fun visitSuperTypeCallEntry(call: KtSuperTypeCallEntry, data: VisitorContext) {
        super.visitSuperTypeCallEntry(call, data)

        // class A(i: Int)
        // class B(i: Int) : A(i)
        //                     ^ we need to be able to resolve this i to the parameter

        constructorParametersAllowed = true
    }

    override fun visitAnonymousInitializer(initializer: KtAnonymousInitializer, data: VisitorContext) {
        super.visitAnonymousInitializer(initializer, data)

        // class A(i: Int) {
        //     init {
        //         val x = i
        //                 ^ we need to be able to resolve this i to the constructor parameter
        //     }
        // }
        constructorParametersAllowed = true
    }

    override fun visitForExpression(element: KtForExpression, data: VisitorContext) {
        super.visitForExpression(element, data)

        inValueContext {
            data.parentOnly {
                if (data.previousElement != element.loopRange) {
                    // fun f(x: Int) {
                    //      for (x in 0..x) {}
                    //                   ^ this x refers to the function parameter rather than the for loop parameter, so we need to ignore
                    //                     the loop parameter.
                    // }
                    val loopParameter = element.loopParameter ?: return
                    processParameter(loopParameter, data)
                }
            }
        }
    }

    override fun visitWhenExpression(element: KtWhenExpression, data: VisitorContext) {
        super.visitWhenExpression(element, data)

        inValueContext {
            data.parentOnly {
                val subjectVariable = element.subjectVariable ?: return
                if (subjectVariable != data.previousElement) {
                    visitProperty(subjectVariable, data)
                }
            }
        }
    }

    override fun visitProperty(element: KtProperty, data: VisitorContext) {
        super.visitProperty(element, data)

        var allowResolveToProperty = true

        data.parentOnly {
            when (data.previousElement) {
                element.delegateExpressionOrInitializer -> {
                    // fun f(x: Int) {
                    //      val x = x
                    //              ^ this x cannot refer to the local variable
                    //                initializers / delegates cannot refer to the variable that they are initializing
                    // }
                    allowResolveToProperty = false

                    // class A(x: Int) {
                    //     val y: Int = x
                    //                  ^ we should resolve x to the constructor parameter
                    // }
                    constructorParametersAllowed = true
                }
                is KtPropertyAccessor -> {
                    // fun f(x: Int) {
                    //     class A(x: Int) {
                    //         val x: Int get() = x
                    //         val y: Int get() = x
                    //                            ^ this x refers to the parameter of f, not the constructor parameter
                    //     }
                    // }
                    allowResolveToProperty = false
                    constructorParametersAllowed = false
                }
            }
        }

        inValueContext {
            if (element.isLocal && allowResolveToProperty) {
                foundIfNameMatches(element)
            }
            data.parentOnly {
                element.contextParameters.processMany { processParameter(it, data) }
            }
        }

        inTypeContext {
            data.parentOnly {
                element.typeParameters.processMany(::processTypeParameter)
            }
        }
    }

    override fun visitClass(klass: KtClass, data: VisitorContext) {
        super.visitClass(klass, data)

        inValueContext(strict = true) {
            if (name == klass.nameAsSafeName) {
                //            object A
                //            fun f() {
                //                class A { companion object }
                //                A
                //                ^ this A should resolve to the companion object
                //            }
                //
                // note: only in value context, in a `::class` literal (VALUE_OR_TYPE context), we should resolve to the local `class A`
                klass.companionObjects.firstOrNull()?.let(::found)
            }
        }
        inTypeContext {
            foundIfNameMatches(klass) // visit class itself only in type context

            data.parentOnly {
                klass.typeParameters.processMany(::processTypeParameter)
            }
        }
        inValueContext {
            data.parentOnly {
                if (constructorParametersAllowed) {
                    klass.primaryConstructorParameters.processMany {
                        if (it.isPropertyParameter()) {
                            return@processMany
                        }

                        processParameter(it, data)
                    }
                }
            }
        }
    }

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration, data: VisitorContext) {
        super.visitObjectDeclaration(declaration, data)

        foundIfNameMatches(declaration)
    }

    override fun visitNamedFunction(element: KtNamedFunction, data: VisitorContext) {
        super.visitNamedFunction(element, data)

        data.parentOnly {
            inValueContext {
                element.valueParameters.processMany { processParameter(it, data) }
                element.contextParameters.processMany { processParameter(it, data) }
            }
            inTypeContext {
                element.typeParameters.processMany(::processTypeParameter)
            }
        }

        // functions cannot be referenced via simple references unless
        // they are part of KtCallExpressions, and we do not do any overload
        // resolution here
    }

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor, data: VisitorContext) {
        super.visitPrimaryConstructor(constructor, data)

        inValueContext {
            constructor.valueParameters.processMany { processParameter(it, data) }
            constructor.contextParameters.processMany { processParameter(it, data) }
        }
        inTypeContext {
            constructor.typeParameters.processMany(::processTypeParameter)
        }
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, data: VisitorContext) {
        super.visitSecondaryConstructor(constructor, data)

        inValueContext {
            constructor.valueParameters.processMany { processParameter(it, data) }
            constructor.contextParameters.processMany { processParameter(it, data) }
        }
        inTypeContext {
            constructor.typeParameters.processMany(::processTypeParameter)
        }
    }

    private fun processTypeParameter(tyParam: KtTypeParameter) {
        foundIfNameMatches(tyParam)
    }

    override fun visitLambdaExpression(element: KtLambdaExpression, data: VisitorContext) {
        super.visitLambdaExpression(element, data)

        inValueContext {
            data.parentOnly {
                if (
                    !element.functionLiteral.hasParameterSpecification() &&
                    name == IMPLICIT_LAMBDA_PARAMETER
                ) {
                    stopResolution()
                }

                element.valueParameters.processMany { processParameter(it, data) }
            }
        }
    }

    private fun processParameter(parameter: KtParameter, data: VisitorContext) {
        when (val des = parameter.destructuringDeclaration) {
            null -> foundIfNameMatches(parameter)

            else -> processDestructuringDeclaration(des, data)
        }
    }

    private fun found(element: KtNamedDeclaration) {
        if (_found != null) return
        _found = element
    }

    private fun foundIfNameMatches(element: KtNamedDeclaration) {
        if (element.nameAsSafeName == name) {
            found(element)
        }
    }

    override fun visitDestructuringDeclaration(decl: KtDestructuringDeclaration, data: VisitorContext) {
        super.visitDestructuringDeclaration(decl, data)

        processDestructuringDeclaration(decl, data)
    }

    fun processDestructuringDeclaration(decl: KtDestructuringDeclaration, data: VisitorContext) {
        data.parentOnly {
            if (data.previousElement == decl.initializer) {
                // fun f(x: Int) {
                //      val (x, y) = x to 1
                //                   ^ for the same reason as with KtProperties, x in the initializer cannot refer to the entry
                // }
                return
            }
        }

        inValueContext {
            decl.entries.processMany(::foundIfNameMatches)
        }
    }

    override fun visitTypeAlias(decl: KtTypeAlias, data: VisitorContext) {
        super.visitTypeAlias(decl, data)

        inTypeContext {
            foundIfNameMatches(decl)
        }
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor, data: VisitorContext) {
        super.visitPropertyAccessor(accessor, data)

        inValueContext {
            data.parentOnly {
                accessor.valueParameters.processMany { processParameter(it, data) }
            }
        }
    }

    override fun visitCatchSection(catchClause: KtCatchClause, data: VisitorContext) {
        super.visitCatchSection(catchClause, data)

        inValueContext {
            data.parentOnly {
                catchClause.parameterList?.parameters?.processMany { processParameter(it, data) }
            }
        }
    }

    private inline fun <T> Iterable<T>.processMany(f: (T) -> Unit) {
        if (_found != null) return

        for (element in this) {
            f(element)
        }
    }

    private inline fun inValueContext(strict: Boolean = false, block: () -> Unit) = when (contextKind) {
        VALUE -> block()
        TYPE -> {}
        VALUE_OR_TYPE if !strict -> block()
        else -> {}
    }

    private inline fun inTypeContext(block: () -> Unit) = when (contextKind) {
        VALUE -> {}
        TYPE -> block()
        VALUE_OR_TYPE -> block()
    }

    private inline fun VisitorContext.parentOnly(block: () -> Unit) {
        if (lastDirection == PARENT) block()
    }
}

private val IMPLICIT_LAMBDA_PARAMETER: Name = Name.identifier("it")

@KtImplementationDetail
object LookupLocallyInternals {
    @KtImplementationDetail
    enum class ContextKind {
        VALUE,
        TYPE,
        VALUE_OR_TYPE,
    }

    @KtImplementationDetail
    fun getContextKind(expression: KtNameReferenceExpression): ContextKind? =
        when (val p = expression.nonContainerParent) {
            is KtCallExpression,
            is KtImportDirective,
            is KtPackageDirective,
            is KtCallableReferenceExpression,
            is KtValueArgumentName,
            is KtLabelReferenceExpression,
                -> null
            is KtDotQualifiedExpression,
            is KtSafeQualifiedExpression,
                -> {
                ContextKind.VALUE.takeIf { p.receiverExpression == expression }
            }
            is KtUserType -> {
                ContextKind.TYPE
                    .takeIf { p.qualifier == null && (p.referenceExpression == expression) }
            }
            is KtClassLiteralExpression -> VALUE_OR_TYPE
            is KtProperty -> {
                ContextKind.VALUE.takeIf { p.delegateExpressionOrInitializer == expression }
            }
            is KtDestructuringDeclaration -> {
                ContextKind.VALUE.takeIf { p.initializer == expression }
            }
            is KtNamedFunction -> {
                ContextKind.VALUE.takeIf { p.bodyExpression == expression }
            }
            is KtPropertyAccessor -> {
                ContextKind.VALUE.takeIf { p.bodyExpression == expression }
            }
            is KtParameter -> {
                ContextKind.VALUE.takeIf { p.defaultValue == expression }
            }
            is KtDelegatedSuperTypeEntry -> {
                if (p.delegateExpression == expression) return VALUE

                TYPE
            }
            is KtValueArgument,
            is KtBinaryExpression,
            is KtBinaryExpressionWithTypeRHS,
            is KtUnaryExpression,
            is KtIsExpression,
            is KtArrayAccessExpression,
            is KtParenthesizedExpression,
            is KtReturnExpression,
            is KtWhileExpression,
            is KtForExpression,
            is KtIfExpression,
            is KtWhenExpression,
            is KtBlockExpression,
            is KtExpressionCodeFragment,
            is KtWhenConditionInRange,
            is KtSimpleNameStringTemplateEntry,
            is KtWhenConditionWithExpression,
            is KtWhenEntry,
                -> VALUE
            is KtTypeConstraint -> TYPE
            else -> null
        }

    @KtImplementationDetail
    @TestOnly
    fun canPerformLocalLookup(expression: KtNameReferenceExpression): Boolean = getContextKind(expression) != null
}

private enum class LastDirection {
    BACKWARDS,
    PARENT,
    INITIAL,
    ;
}

private class VisitorContext(
    val lastDirection: LastDirection,
    val previousElement: KtElement?,
)
