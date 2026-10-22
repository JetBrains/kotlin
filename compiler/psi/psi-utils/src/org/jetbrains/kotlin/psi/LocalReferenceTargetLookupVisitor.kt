/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi

import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.util.elementType
import com.intellij.psi.util.siblings
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.name.Name
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
 * Note: in dangling files, lookupLocally does not work.
 */
@KtExperimentalApi
fun KtNameReferenceExpression.lookupLocally(): KtNamedDeclaration? {
    if (containingFile.isDangling()) return null

    val contextKind = localLookupContextKind ?: return null

    return LocalReferenceTargetLookupVisitor(this, contextKind).lookup()
}

private fun PsiFile.isDangling(): Boolean = !isEquivalentTo(originalFile)

/**
 * Returns `true` if can perform a local lookup from the given starting expression.
 *
 * This API is marked as an implementation detail of the Kotlin PSI API
 * and is not intended for public or external use. It may change or be removed
 * without notice.
 */
@TestOnly
@KtImplementationDetail
fun KtNameReferenceExpression.canPerformLocalLookup(): Boolean = !containingFile.isDangling() && localLookupContextKind != null

private val KtElement.nonContainerParent: KtElement?
    get() {
        var e = parent ?: context
        while (e != null && (e is KtContainerNode || e.elementType in CODE_FRAGMENTS)) {
            e = e.parent ?: e.context
        }
        return e as? KtElement
    }

/**
 * Returns the kind of context in which a local lookup is performed.
 *
 * This API is an implementation detail and may change in the future.
 */
@KtImplementationDetail
val KtNameReferenceExpression.localLookupContextKind: LocalLookupContextKind?
    get() =
        when (val p = nonContainerParent) {
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
                LocalLookupContextKind.VALUE.takeIf { p.receiverExpression == this@localLookupContextKind }
            }
            is KtUserType -> {
                LocalLookupContextKind.TYPE
                    .takeIf { p.qualifier == null && (p.referenceExpression == this@localLookupContextKind) }
            }
            is KtClassLiteralExpression -> LocalLookupContextKind.VALUE_OR_TYPE
            is KtProperty -> {
                LocalLookupContextKind.VALUE.takeIf { p.delegateExpressionOrInitializer == this@localLookupContextKind }
            }
            is KtDestructuringDeclaration -> {
                LocalLookupContextKind.VALUE.takeIf { p.initializer == this@localLookupContextKind }
            }
            is KtNamedFunction -> {
                LocalLookupContextKind.VALUE.takeIf { p.bodyExpression == this@localLookupContextKind }
            }
            is KtPropertyAccessor -> {
                LocalLookupContextKind.VALUE.takeIf { p.bodyExpression == this@localLookupContextKind }
            }
            is KtParameter -> {
                LocalLookupContextKind.VALUE.takeIf { p.defaultValue == this@localLookupContextKind }
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
                -> LocalLookupContextKind.VALUE
            is KtTypeConstraint, is KtDelegatedSuperTypeEntry -> LocalLookupContextKind.TYPE
            else -> null
        }

private class LocalReferenceTargetLookupVisitor(val element: KtNameReferenceExpression, val contextKind: LocalLookupContextKind) :
    KtVisitorVoid() {
    fun lookup(): KtNamedDeclaration? {
        val s = sequence {
            // invariant: current is always yielded
            var current: KtElement = element
            yield(current to LastDirection.INITIAL)

            while (true) {
                if (current.isStopElement) break

                when (val p = current.nonContainerParent) {
                    is KtBlockExpression -> {
                        for (sibling in current.siblings(forward = false, withSelf = false).filterIsInstance<KtElement>()) {
                            yield(sibling to LastDirection.BACKWARDS)
                        }
                        current = p
                        yield(current to LastDirection.PARENT)
                    }

                    null -> break

                    else -> {
                        current = p
                        yield(current to LastDirection.PARENT)
                    }
                }
            }
        }

        var previousElement: KtElement? = null

        for ([element, direction] in s) {
            myLastDirection = direction
            processMove(element, previousElement)
            element.accept(this)
            if (_stopResolution) return null

            _found?.let { return it }

            previousElement = element
        }

        return null
    }

    private var _found: KtNamedDeclaration? = null
    private val name: Name = element.getReferencedNameAsName()

    private val resolveIgnore: MutableSet<KtElement> = mutableSetOf()
    private var constructorParametersAllowed: Boolean = false
    private var _stopResolution: Boolean = false
    private fun stopResolution() {
        _stopResolution = true
    }

    private fun ignore(element: KtElement) {
        resolveIgnore.add(element)
    }

    private fun isIgnored(element: KtElement): Boolean = resolveIgnore.contains(element)

    private enum class LastDirection {
        BACKWARDS,
        PARENT,
        INITIAL,
        ;
    }

    private var myLastDirection: LastDirection = LastDirection.INITIAL

    private val KtElement.isScriptTopLevelElement: Boolean
        get() = parent is KtBlockExpression && parent.parent is KtScript

    private val KtElement.isStopElementInValueContext: Boolean
        get() = myLastDirection == PARENT &&
                when (this) {
                    is KtNamedFunction -> isTopLevel
                    is KtProperty -> isTopLevel
                    is KtClassOrObject -> isTopLevel() || this is KtObjectDeclaration && this.isCompanion()
                    else -> false
                }
                || isScriptTopLevelElement

    private val KtElement.isStopElementInTypeContext: Boolean
        get() = myLastDirection == PARENT &&
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

    private val KtElement.isStopElement: Boolean
        get() = when (contextKind) {
            LocalLookupContextKind.VALUE -> isStopElementInValueContext
            LocalLookupContextKind.TYPE -> isStopElementInTypeContext
            LocalLookupContextKind.VALUE_OR_TYPE -> isStopElementInValueContext || isStopElementInTypeContext
        }

    /**
     * Process declarations we should ignore.
     *
     * This is necessary because instead of going down the tree until we find the declaration,
     * we are walking up the tree from the reference [element]. So in cases like `val x = x`,
     * we cannot use the variable inside its own initializer. In such cases, when we detect that
     * we are coming from the initializer, we mark the variable as ignored. This prevents us from
     * resolving the reference to that variable, meaning we have to continue the search, and we'll go
     * in the previous statements, check if they are local variables, then the parents of that block,
     * etc.
     *
     * @see ignore
     */
    private fun processMove(current: KtElement, previousElement: KtElement?) {
        if (previousElement is KtClass) constructorParametersAllowed = false

        when (current) {
            is KtProperty -> {
                parentOnly {
                    if (previousElement == current.delegateExpressionOrInitializer) {
                        // fun f(x: Int) {
                        //      val x = x
                        //              ^ this x cannot refer to the local variable
                        //                initializers / delegates cannot refer to the variable that they are initializing
                        // }
                        ignore(current)

                        // class A(x: Int) {
                        //     val y: Int = x
                        //                  ^ we should resolve x to the constructor parameter
                        // }
                        constructorParametersAllowed = true
                    } else if (previousElement is KtPropertyAccessor) {
                        // fun f(x: Int) {
                        //     class A(x: Int) {
                        //         val x: Int get() = x
                        //         val y: Int get() = x
                        //                            ^ this x refers to the parameter of f, not the constructor parameter
                        //     }
                        // }
                        ignore(current)
                        constructorParametersAllowed = false
                    }
                }
            }

            is KtDestructuringDeclaration -> {
                parentOnly {
                    if (previousElement == current.initializer) {
                        // fun f(x: Int) {
                        //      val (x, y) = x to 1
                        //                   ^ for the same reason as with KtProperties, x in the initializer cannot refer to the entry
                        // }
                        ignore(current)
                        current.entries.forEach(::ignore)
                    }
                }
            }

            is KtForExpression -> {
                parentOnly {
                    if (previousElement == current.loopRange) {
                        // fun f(x: Int) {
                        //      for (x in 0..x) {}
                        //                   ^ this x refers to the function parameter rather than the for loop parameter, so we need to ignore
                        //                     the loop parameter.
                        // }
                        val loopParameter = current.loopParameter ?: return
                        ignoreParameter(loopParameter)
                    }
                }
            }

            is KtSuperTypeCallEntry -> {
                // class A(i: Int)
                // class B(i: Int) : A(i)
                //                     ^ we need to be able to resolve this i to the parameter

                constructorParametersAllowed = true
            }

            is KtAnonymousInitializer -> {
                // class A(i: Int) {
                //     init {
                //         val x = i
                //                 ^ we need to be able to resolve this i to the constructor parameter
                //     }
                // }
                constructorParametersAllowed = true
            }
        }
    }

    private fun ignoreParameter(param: KtParameter) {
        ignore(param)
        param.destructuringDeclaration?.entries?.forEach(::ignore)
    }

    override fun visitForExpression(element: KtForExpression) {
        val loopParameter = element.loopParameter ?: return

        inValueContext {
            parentOnly {
                processParameter(loopParameter)
            }
        }
    }

    override fun visitWhenExpression(element: KtWhenExpression) {
        inValueContext {
            parentOnly {
                element.subjectVariable?.let(::visitProperty)
            }
        }
    }

    override fun visitProperty(element: KtProperty) {
        inValueContext {
            foundIfNameMatches(element)
            parentOnly {
                element.contextParameters.processMany(::processParameter)
            }
        }

        inTypeContext {
            parentOnly {
                element.typeParameters.processMany(::processTypeParameter)
            }
        }
    }

    override fun visitClass(klass: KtClass) {
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
                klass.companionObjects.firstOrNull()
                    ?.takeIf(::isValidCandidate)
                    ?.let(::found)
            }
        }
        inTypeContext {
            foundIfNameMatches(klass) // visit class itself only in type context

            parentOnly {
                klass.typeParameters.processMany(::processTypeParameter)
            }
        }
        inValueContext {
            parentOnly {
                if (constructorParametersAllowed) {
                    klass.primaryConstructorParameters.processMany {
                        if (it.isPropertyParameter()) {
                            return@processMany
                        }

                        processParameter(it)
                    }
                }
            }
        }
    }

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
        foundIfNameMatches(declaration)
    }

    override fun visitNamedFunction(element: KtNamedFunction) {
        inValueContext {
            element.valueParameters.processMany(::processParameter)
            element.contextParameters.processMany(::processParameter)
        }
        inTypeContext {
            element.typeParameters.processMany(::processTypeParameter)
        }

        // functions cannot be referenced via simple references unless
        // they are part of KtCallExpressions, and we do not do any overload
        // resolution here
    }

    override fun visitPrimaryConstructor(constructor: KtPrimaryConstructor) {
        inValueContext {
            constructor.valueParameters.processMany(::processParameter)
            constructor.contextParameters.processMany(::processParameter)
        }
        inTypeContext {
            constructor.typeParameters.processMany(::processTypeParameter)
        }
    }

    override fun visitSecondaryConstructor(constructor: KtSecondaryConstructor) {
        inValueContext {
            constructor.valueParameters.processMany(::processParameter)
            constructor.contextParameters.processMany(::processParameter)
        }
        inTypeContext {
            constructor.typeParameters.processMany(::processTypeParameter)
        }
    }

    private fun processTypeParameter(tyParam: KtTypeParameter) {
        foundIfNameMatches(tyParam)
    }

    override fun visitLambdaExpression(element: KtLambdaExpression) {
        inValueContext {
            parentOnly {
                if (
                    !element.functionLiteral.hasParameterSpecification() &&
                    name == IMPLICIT_LAMBDA_PARAMETER
                ) {
                    stopResolution()
                }

                element.valueParameters.processMany(::processParameter)
            }
        }
    }

    private fun processParameter(parameter: KtParameter) {
        if (isIgnored(parameter)) return

        when (val des = parameter.destructuringDeclaration) {
            null -> foundIfNameMatches(parameter)

            else -> visitDestructuringDeclaration(des)
        }
    }

    private fun found(element: KtNamedDeclaration) {
        if (_found != null) return
        _found = element
    }

    private fun isValidCandidate(element: KtNamedDeclaration): Boolean = !isIgnored(element)

    private fun foundIfNameMatches(element: KtNamedDeclaration) {
        if (element.nameAsSafeName == name && isValidCandidate(element)) {
            found(element)
        }
    }

    override fun visitClassOrObject(element: KtClassOrObject) {
        foundIfNameMatches(element)
    }

    override fun visitDestructuringDeclaration(decl: KtDestructuringDeclaration) {
        if (isIgnored(decl)) return

        inValueContext {
            decl.entries.processMany(::foundIfNameMatches)
        }
    }

    override fun visitTypeAlias(decl: KtTypeAlias) {
        inTypeContext {
            foundIfNameMatches(decl)
        }
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
        inValueContext {
            parentOnly {
                accessor.valueParameters.processMany(::processParameter)
            }
        }
    }

    override fun visitCatchSection(catchClause: KtCatchClause) {
        inValueContext {
            parentOnly {
                catchClause.parameterList?.parameters?.processMany(::processParameter)
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

    private inline fun parentOnly(block: () -> Unit) {
        if (myLastDirection == PARENT) block()
    }
}

private val CODE_FRAGMENTS = setOf(
    TokenType.CODE_FRAGMENT,
    KtNodeTypes.BLOCK_CODE_FRAGMENT,
    KtNodeTypes.EXPRESSION_CODE_FRAGMENT,
    KtNodeTypes.TYPE_CODE_FRAGMENT
)

/**
 * Represents the kind of context in which a local lookup is performed.
 *
 * This enum is used internally by the Kotlin PSI API to differentiate
 * between different types of lookups, such as resolving values, types,
 * or both values and types in the local context.
 *
 * VALUE - Indicates a context where only values are looked up.
 * TYPE - Indicates a context where only types are looked up.
 * VALUE_OR_TYPE - Indicates a context where both values and types can be looked up.
 *
 * This API is marked as an implementation detail of the Kotlin PSI API
 * and is not intended for public or external use. It may change or be removed
 * without notice.
 */
@KtImplementationDetail
enum class LocalLookupContextKind {
    /**
     * Indicates a context where only values are looked up.
     */
    VALUE,

    /**
     * Indicates a context where only types are looked up.
     */
    TYPE,

    /**
     * Indicates a context where both values and types can be looked up.
     *
     * For example, `a::class` class literals.
     */
    VALUE_OR_TYPE,
}

private val IMPLICIT_LAMBDA_PARAMETER: Name = Name.identifier("it")
