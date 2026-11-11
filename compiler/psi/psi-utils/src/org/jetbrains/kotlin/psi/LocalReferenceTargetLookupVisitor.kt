/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi

import com.intellij.psi.util.siblings
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.LookupLocallyInternals.ContextKind.*
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

    return when (contextKind) {
        is VALUE -> ValueLocalReferenceTargetLookupVisitor(this, contextKind.strict).lookup().toResult()
        TYPE -> TypeLocalReferenceTargetLookupVisitor(this).lookup().toResult()
    }
}

private val KtElement.nonContainerParent: KtElement?
    get() {
        var e = parent ?: context
        while (e != null && (e is KtContainerNode || e is KtCodeFragment)) {
            e = e.parent ?: e.context
        }
        return e as? KtElement
    }

private abstract class LocalReferenceTargetLookupVisitor(val element: KtNameReferenceExpression) :
    KtVisitor<Unit, VisitorContext>() {

    var _found: KtNamedDeclaration? = null
    protected val name: Name = element.getReferencedNameAsName()

    protected fun found(element: KtNamedDeclaration) {
        if (_found != null) return
        _found = element
    }

    protected fun foundIfNameMatches(element: KtNamedDeclaration) {
        if (element.nameAsSafeName == name) {
            found(element)
        }
    }

    var _stopResolution: Boolean = false
    protected fun stopResolution() {
        _stopResolution = true
    }

    abstract fun isStopElement(element: KtElement, lastDirection: LastDirection): Boolean

    protected inline fun <T> Iterable<T>.processMany(f: (T) -> Unit) {
        if (_found != null) return

        for (element in this) {
            f(element)
            if (_found != null) return
        }
    }
}

private fun LocalReferenceTargetLookupVisitor.availableNameElementSequence(): Sequence<Pair<KtElement, LastDirection>> = sequence {
    // invariant: current is always yielded
    var current: KtElement = element
    var lastDirection = LastDirection.INITIAL
    yield(current to lastDirection)

    while (true) {
        if (isStopElement(current, lastDirection)) break

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

private sealed interface LookupResult {
    data class Found(val declaration: KtNamedDeclaration) : LookupResult
    data object Stopped : LookupResult
    data object NotFound : LookupResult
}

private fun LookupResult.toResult() = when (this) {
    is Found -> declaration
    is Stopped -> null
    NotFound -> null
}

private fun LocalReferenceTargetLookupVisitor.lookup(): LookupResult {
    val availableNameElementSequence = availableNameElementSequence()

    var previousElement: KtElement? = null

    for ([element, direction] in availableNameElementSequence) {
        element.accept(this, VisitorContext(direction, previousElement))
        if (_stopResolution) return LookupResult.Stopped

        _found?.let { return LookupResult.Found(it) }

        previousElement = element
    }

    return LookupResult.NotFound
}

private class ValueLocalReferenceTargetLookupVisitor(
    element: KtNameReferenceExpression,
    val strict: Boolean,
) : LocalReferenceTargetLookupVisitor(element) {
    private var constructorParametersAllowed: Boolean = false

    override fun isStopElement(element: KtElement, lastDirection: LastDirection): Boolean =
        lastDirection == PARENT &&
                when (element) {
                    is KtNamedFunction -> element.isTopLevel
                    is KtProperty -> element.isTopLevel
                    is KtClassOrObject -> element.isTopLevel() || element is KtObjectDeclaration && element.isCompanion()
                    else -> false
                }
                || element.isScriptTopLevelElement

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

    override fun visitDelegatedSuperTypeEntry(specifier: KtDelegatedSuperTypeEntry, data: VisitorContext) {
        super.visitDelegatedSuperTypeEntry(specifier, data)

        // interface I
        // class A : I
        // class B(i: I) : I by i
        //                      ^
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

    override fun visitWhenExpression(element: KtWhenExpression, data: VisitorContext) {
        super.visitWhenExpression(element, data)

        data.parentOnly {
            val subjectVariable = element.subjectVariable ?: return
            if (subjectVariable != data.previousElement) {
                visitProperty(subjectVariable, data)
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

                    if (!element.isLocal) {
                        // class A(x: Int) {
                        //     val y: Int = x
                        //                  ^ we should resolve x to the constructor parameter
                        //     fun f() {
                        //         val z = x
                        //                 ^ but not this x
                        //     }
                        // }
                        constructorParametersAllowed = true
                    }
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

        if (element.isLocal && allowResolveToProperty) {
            foundIfNameMatches(element)
        }
        data.parentOnly {
            element.contextParameters.processMany { processParameter(it, data) }
        }
    }

    override fun visitClass(klass: KtClass, data: VisitorContext) {
        super.visitClass(klass, data)

        if (strict) {
            if (name == klass.nameAsSafeName) {
                //            object A
                //            fun f() {
                //                class A { companion object }
                //                A
                //                ^ this A should resolve to the companion object
                //            }
                //
                // note: only in value context, in a `::class` literal (!strict context), we should resolve to the local `class A`
                klass.companionObjects.firstOrNull()?.let(::found)
            }
        }

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

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration, data: VisitorContext) {
        super.visitObjectDeclaration(declaration, data)

        foundIfNameMatches(declaration)
    }

    override fun visitNamedFunction(element: KtNamedFunction, data: VisitorContext) {
        super.visitNamedFunction(element, data)

        constructorParametersAllowed = false

        data.parentOnly {
            element.valueParameters.processMany { processParameter(it, data) }
            element.contextParameters.processMany { processParameter(it, data) }
        }

        // functions cannot be referenced via simple references unless
        // they are part of KtCallExpressions, and we do not do any overload
        // resolution here
    }

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor, data: VisitorContext) {
        super.visitPrimaryConstructor(constructor, data)

        constructor.valueParameters.processMany { processParameter(it, data) }
        constructor.contextParameters.processMany { processParameter(it, data) }
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, data: VisitorContext) {
        super.visitSecondaryConstructor(constructor, data)

        constructor.valueParameters.processMany { processParameter(it, data) }
        constructor.contextParameters.processMany { processParameter(it, data) }

        constructorParametersAllowed = false
    }

    override fun visitLambdaExpression(element: KtLambdaExpression, data: VisitorContext) {
        super.visitLambdaExpression(element, data)

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

    private fun processParameter(parameter: KtParameter, data: VisitorContext) {
        when (val des = parameter.destructuringDeclaration) {
            null -> foundIfNameMatches(parameter)

            else -> processDestructuringDeclaration(des, data)
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

        decl.entries.processMany(::foundIfNameMatches)
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor, data: VisitorContext) {
        super.visitPropertyAccessor(accessor, data)

        data.parentOnly {
            accessor.valueParameters.processMany { processParameter(it, data) }
        }
    }

    override fun visitCatchSection(catchClause: KtCatchClause, data: VisitorContext) {
        super.visitCatchSection(catchClause, data)

        data.parentOnly {
            catchClause.parameterList?.parameters?.processMany { processParameter(it, data) }
        }
    }
}

private class TypeLocalReferenceTargetLookupVisitor(
    element: KtNameReferenceExpression,
) : LocalReferenceTargetLookupVisitor(element) {
    override fun isStopElement(element: KtElement, lastDirection: LastDirection): Boolean =
        lastDirection == PARENT &&
                when (element) {
                    is KtNamedFunction -> !element.isLocal
                    is KtProperty -> !element.isLocal
                    is KtClassOrObject,
                    is KtAnonymousInitializer,
                    is KtConstructor<*>,
                        -> true
                    else -> false
                }
                || element.isScriptTopLevelElement

    override fun visitProperty(element: KtProperty, data: VisitorContext) {
        super.visitProperty(element, data)

        data.parentOnly {
            element.typeParameters.processMany(::processTypeParameter)
        }
    }

    override fun visitClass(klass: KtClass, data: VisitorContext) {
        super.visitClass(klass, data)

        foundIfNameMatches(klass) // visit class itself only in type context

        data.parentOnly {
            klass.typeParameters.processMany(::processTypeParameter)
        }
    }

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration, data: VisitorContext) {
        super.visitObjectDeclaration(declaration, data)

        foundIfNameMatches(declaration)
    }

    override fun visitNamedFunction(element: KtNamedFunction, data: VisitorContext) {
        super.visitNamedFunction(element, data)

        data.parentOnly {
            element.typeParameters.processMany(::processTypeParameter)
        }

        // functions cannot be referenced via simple references unless
        // they are part of KtCallExpressions, and we do not do any overload
        // resolution here
    }

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor, data: VisitorContext) {
        super.visitPrimaryConstructor(constructor, data)

        constructor.typeParameters.processMany(::processTypeParameter)
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor, data: VisitorContext) {
        super.visitSecondaryConstructor(constructor, data)

        constructor.typeParameters.processMany(::processTypeParameter)
    }

    private fun processTypeParameter(tyParam: KtTypeParameter) {
        foundIfNameMatches(tyParam)
    }

    override fun visitTypeAlias(decl: KtTypeAlias, data: VisitorContext) {
        super.visitTypeAlias(decl, data)

        foundIfNameMatches(decl)
    }
}

private val IMPLICIT_LAMBDA_PARAMETER: Name = Name.identifier("it")

@KtImplementationDetail
object LookupLocallyInternals {
    @KtImplementationDetail
    sealed interface ContextKind {
        data class VALUE(val strict: Boolean = true) : ContextKind
        data object TYPE : ContextKind
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
                ContextKind.VALUE().takeIf { p.receiverExpression == expression }
            }
            is KtUserType -> {
                ContextKind.TYPE
                    .takeIf { p.qualifier == null && (p.referenceExpression == expression) }
            }
            // We can use types in a class literal expression as well, but between a non-local variable and a local class of the same name,
            // the compiler picks the non-local variable. I.e. in this case:
            //
            // val x: Int = 0
            // fun f() {
            //     class x
            //     x::class
            //     ^ resolves to `val x` rather than `class x`
            // }
            //
            // So we can only resolve local variables in class literal expressions, trying to resolve types causes it to be unsound
            // because of the above.
            is KtClassLiteralExpression -> VALUE(false)
            is KtProperty -> {
                ContextKind.VALUE().takeIf { p.delegateExpressionOrInitializer == expression }
            }
            is KtDestructuringDeclaration -> {
                ContextKind.VALUE().takeIf { p.initializer == expression }
            }
            is KtNamedFunction -> {
                ContextKind.VALUE().takeIf { p.bodyExpression == expression }
            }
            is KtPropertyAccessor -> {
                ContextKind.VALUE().takeIf { p.bodyExpression == expression }
            }
            is KtParameter -> {
                ContextKind.VALUE().takeIf { p.defaultValue == expression }
            }
            is KtDelegatedSuperTypeEntry -> {
                if (p.delegateExpression == expression) return VALUE()

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
                -> VALUE()
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

private inline fun VisitorContext.parentOnly(block: () -> Unit) {
    if (lastDirection == PARENT) block()
}

private val KtElement.isScriptTopLevelElement: Boolean
    get() = parent is KtBlockExpression && parent.parent is KtScript
