/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.util.elementType
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.name.Name

/**
 * Performs local, PSI-only name lookups.
 *
 * If not null, the returned declaration is what the compiler would resolve the name to.
 *
 * @return `null` if the lookup failed / cannot be performed, otherwise the found local declaration.
 *
 * This is available as a local-only optimization. It makes use of the fact that local scopes always have the highest
 * priority in resolution.
 */
@KtExperimentalApi
fun KtSimpleNameExpression.lookupLocally(): KtNamedDeclaration? {
    val contextKind = localLookupContextKind ?: return null

    return LocalReferenceTargetLookupVisitor(this, contextKind).lookup()
}

/**
 * Returns `true` if can perform a local lookup from the given starting expression.
 *
 * This API is marked as an implementation detail of the Kotlin PSI API
 * and is not intended for public or external use. It may change or be removed
 * without notice.
 */
@TestOnly
@KtImplementationDetail
fun KtSimpleNameExpression.canPerformLocalLookup(): Boolean = localLookupContextKind != null

private val KtElement.nonContainerParent: KtElement?
    get() {
        var e = parent ?: context
        while (e != null && (e is KtContainerNode || e.elementType in CODE_FRAGMENTS)) {
            e = e.parent ?: e.context
        }
        return e as? KtElement
    }

private fun KtSimpleNameExpression.typeIsValidForLocalLookup(): Boolean =
    this !is KtOperationReferenceExpression && this !is KtLabelReferenceExpression

/**
 * Returns the kind of context in which a local lookup is performed.
 *
 * This API is marked as an implementation detail of the Kotlin PSI API
 * and is not intended for public or external use. It may change or be removed
 * without notice.
 */
@KtImplementationDetail
val KtSimpleNameExpression.localLookupContextKind: LocalLookupContextKind?
    get() =
        if (!typeIsValidForLocalLookup()) null
        else when (val p = nonContainerParent) {
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

private class LocalReferenceTargetLookupVisitor(val element: KtSimpleNameExpression, val contextKind: LocalLookupContextKind) :
    KtVisitorVoid() {
    fun lookup(): KtNamedDeclaration? {
        var current: KtElement = element

        while (true) {
            current.accept(this)
            _found?.let { return it }

            previousElement = current
            current = next(current) ?: return null
            processIgnores(current)
        }
    }

    private var _found: KtNamedDeclaration? = null
    private var previousElement: KtElement? = null
    private val name: Name = element.getReferencedNameAsName()

    private val resolveIgnore: MutableSet<KtElement> = mutableSetOf()

    private fun ignore(element: KtElement) {
        resolveIgnore.add(element)
    }

    private fun isIgnored(element: KtElement): Boolean = resolveIgnore.contains(element)

    private val KtElement.typeMatchesForValueContext: Boolean
        get() = this is KtProperty
                || this is KtDestructuringDeclarationEntry
                || this is KtParameter
                || this is KtObjectDeclaration

    private val KtElement.typeMatchesForTypeContext: Boolean
        get() = this is KtClassOrObject
                || this is KtTypeAlias
                || this is KtTypeParameter

    private fun typeMatchesGivenContext(element: KtElement, contextKind: LocalLookupContextKind): Boolean = when (contextKind) {
        LocalLookupContextKind.VALUE -> element.typeMatchesForValueContext
        LocalLookupContextKind.TYPE -> element.typeMatchesForTypeContext
        LocalLookupContextKind.VALUE_OR_TYPE -> element.typeMatchesForValueContext || element.typeMatchesForTypeContext
    }

    fun PsiElement.prevKtSibling(): KtElement? {
        var current: PsiElement? = this.prevSibling
        while (current != null) {
            if (current is KtElement) return current
            current = current.prevSibling
        }
        return null
    }

    private enum class LastDirection {
        BACKWARDS,
        PARENT,
        UNKNOWN,
        INITIAL,
        ;
    }

    private var myLastDirection: LastDirection = LastDirection.INITIAL

    private fun isStopElement(element: KtElement): Boolean =
        element is KtNamedFunction && !element.isLocal && lastDirectionIs(LastDirection.PARENT)
                || element is KtProperty && !element.isLocal && lastDirectionIs(LastDirection.PARENT)

    private fun shouldStopBeforeProcessing(element: KtElement): Boolean =
        element is KtClassOrObject && lastDirectionIs(LastDirection.PARENT)
                || element is KtFile && element.elementType !in CODE_FRAGMENTS
                || (element.parent is KtBlockExpression && element.parent.parent is KtScript)
                || element is KtAnonymousInitializer

    /**
     * Given the current element, this function returns the next element we should visit.
     */
    private fun next(element: KtElement): KtElement? {
        if (isStopElement(element)) return null

        myLastDirection = LastDirection.UNKNOWN
        return when (val p = element.parent) {
            is KtBlockExpression -> {
                val prev = element.prevKtSibling()
                if (prev != null) {
                    myLastDirection = LastDirection.BACKWARDS
                    return prev
                }

                myLastDirection = LastDirection.PARENT
                p
            }

            else -> {
                myLastDirection = LastDirection.PARENT
                element.nonContainerParent
            }
        }
            ?.takeUnless(::shouldStopBeforeProcessing)
    }

    private fun lastDirectionIs(@Suppress("SameParameterValue") value: LastDirection): Boolean =
        when (val d = myLastDirection) {
            LastDirection.UNKNOWN -> throw IllegalStateException("Last direction is unknown")
            else -> d == value
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
     * @see next
     */
    private fun processIgnores(current: KtElement) {
        when (current) {
            is KtProperty -> {
                if (lastDirectionIs(LastDirection.PARENT) && previousElement == current.delegateExpressionOrInitializer) {
                    // fun f(x: Int) {
                    //      val x = x
                    //              ^ this x cannot refer to the local variable
                    //                initializers / delegates cannot refer to the variable that they are initializing
                    // }
                    ignore(current)
                }
            }

            is KtDestructuringDeclaration -> {
                if (lastDirectionIs(LastDirection.PARENT) && previousElement == current.initializer) {
                    // fun f(x: Int) {
                    //      val (x, y) = x to 1
                    //                   ^ for the same reason as with KtProperties, x in the initializer cannot refer to the entry
                    // }
                    ignore(current)
                    current.entries.forEach(::ignore)
                }
            }

            is KtNamedFunction -> {
                if (!lastDirectionIs(LastDirection.PARENT)) {
                    // if the last direction isn't parent, then in a case like this, where we are vising the block backwards to look
                    // for the name, we need to ignore everything that's introduced in the scope of the function
                    //
                    // fun f(x: Int) {
                    //      fun g(x: Int) {}
                    //      val y = x
                    //              ^ this x cannot be resolved to the x parameter of g
                    // }
                    current.valueParameters.forEach(::ignoreParameter)
                    current.typeParameters.forEach(::ignore)
                    current.contextParameters.forEach(::ignoreParameter)
                }
            }

            is KtLambdaExpression -> {
                if (!lastDirectionIs(LastDirection.PARENT)) {
                    // Same as with functions, if we run into a free-standing lambda statement (which may be quite uncommon but
                    // we need to be correct here), we need to ignore its parameters
                    //
                    // fun f(x: Int): Int {
                    //     {x: Int -> x}
                    //     return x
                    //            ^ this x cannot be resolved to the x parameter of the lambda
                    // }
                    current.valueParameters.forEach(::ignoreParameter)
                }
            }

            is KtForExpression -> {
                if (lastDirectionIs(LastDirection.PARENT) && previousElement == current.loopRange) {
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
    }

    private fun ignoreParameter(param: KtParameter) {
        ignore(param)
        param.destructuringDeclaration?.entries?.forEach(::ignore)
    }

    override fun visitForExpression(element: KtForExpression) {
        val loopParameter = element.loopParameter ?: return

        processParameter(loopParameter)
    }

    override fun visitWhenExpression(element: KtWhenExpression) {
        element.subjectVariable?.let(::visitProperty)
    }

    override fun visitProperty(element: KtProperty) {
        foundIfNameMatches(element)

        element.typeParameters.processMany(::processTypeParameter)
        element.contextParameters.processMany(::processParameter)
    }

    override fun visitClass(klass: KtClass) {
        if (contextKind == LocalLookupContextKind.VALUE && name == klass.nameAsSafeName) {
            klass.companionObjects.firstOrNull()
                ?.takeIf(::isValidCandidate)
                ?.let(::found)
        }
        foundIfNameMatches(klass)
    }

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration) {
        foundIfNameMatches(declaration)
    }

    override fun visitNamedFunction(element: KtNamedFunction) {
        element.valueParameters.processMany(::processParameter)
        element.contextParameters.processMany(::processParameter)
        element.typeParameters.processMany(::processTypeParameter)

        // functions cannot be referenced via simple references unless
        // they are part of KtCallExpressions, and we do not do any overload
        // resolution here
    }

    private fun processTypeParameter(tyParam: KtTypeParameter) {
        foundIfNameMatches(tyParam)
    }

    override fun visitLambdaExpression(element: KtLambdaExpression) {
        element.valueParameters.processMany(::processParameter)
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

    private fun isValidCandidate(element: KtNamedDeclaration): Boolean =
        !isIgnored(element) && typeMatchesGivenContext(element, contextKind)

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

        decl.entries.processMany(::foundIfNameMatches)
    }

    override fun visitTypeAlias(decl: KtTypeAlias) {
        foundIfNameMatches(decl)
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
        accessor.valueParameters.processMany(::processParameter)
    }

    private inline fun <T> Iterable<T>.processMany(f: (T) -> Unit) {
        if (_found != null) return

        for (element in this) {
            f(element)
            if (_found != null) return
        }
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
     * Indicates a context where we are looking up values.
     */
    VALUE,

    /**
     * Indicates a context where we are looking up types.
     */
    TYPE,

    /**
     * Indicates a context where both values and types can be looked up.
     * For example, `T::class` literals.
     */
    VALUE_OR_TYPE,
}
