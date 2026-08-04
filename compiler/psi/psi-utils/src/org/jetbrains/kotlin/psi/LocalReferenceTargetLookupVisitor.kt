/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.psi.PsiElement
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
    val contextKind = contextKind ?: return null

    return LocalReferenceTargetLookupVisitor(this, contextKind).lookup()
}

private val KtElement.nonContainerParent: KtElement?
    get() {
        var e = parent
        while (e is KtContainerNode) {
            e = e.parent
        }
        return e as? KtElement
    }

private fun KtSimpleNameExpression.typeIsValidForLocalLookup(): Boolean =
    this !is KtOperationReferenceExpression

private val KtSimpleNameExpression.contextKind: LocalReferenceTargetLookupVisitor.ContextKind?
    get() =
        if (!typeIsValidForLocalLookup()) null
        else when (val p = nonContainerParent) {
            is KtCallExpression,
            is KtImportDirective,
            is KtPackageDirective,
            is KtCallableReferenceExpression,
            is KtValueArgumentName,
                -> null
            is KtDotQualifiedExpression,
            is KtSafeQualifiedExpression,
                -> {
                LocalReferenceTargetLookupVisitor.ContextKind.VALUE.takeIf { p.receiverExpression == this@contextKind }
            }
            is KtUserType -> {
                LocalReferenceTargetLookupVisitor.ContextKind.TYPE
                    .takeIf { p.qualifier == null && (p.referenceExpression == this@contextKind) }
            }
            is KtClassLiteralExpression -> LocalReferenceTargetLookupVisitor.ContextKind.VALUE_OR_TYPE
            is KtValueArgument,
            is KtExpression,
            is KtExpressionCodeFragment,
            is KtWhenConditionInRange,
            is KtSimpleNameStringTemplateEntry,
            is KtWhenConditionWithExpression,
            is KtWhenEntry,
                -> LocalReferenceTargetLookupVisitor.ContextKind.VALUE
            is KtTypeConstraint, is KtDelegatedSuperTypeEntry -> LocalReferenceTargetLookupVisitor.ContextKind.TYPE
            else -> null
        }

private class LocalReferenceTargetLookupVisitor(val element: KtSimpleNameExpression, val contextKind: ContextKind) : KtVisitorVoid() {
    enum class ContextKind {
        VALUE,
        TYPE,
        VALUE_OR_TYPE,
    }

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

    private fun isIgnored(element: KtElement): Boolean = resolveIgnore.contains(element) || !typeMatchesGivenContext(element, contextKind)

    private val KtElement.typeMatchesForValueContext: Boolean
        get() = this is KtProperty
                || this is KtDestructuringDeclarationEntry
                || this is KtParameter
                || this is KtObjectDeclaration

    private val KtElement.typeMatchesForTypeContext: Boolean
        get() = this is KtClassOrObject
                || this is KtTypeAlias
                || this is KtTypeParameter

    private fun typeMatchesGivenContext(element: KtElement, contextKind: ContextKind): Boolean = when (contextKind) {
        ContextKind.VALUE -> element.typeMatchesForValueContext
        ContextKind.TYPE -> element.typeMatchesForTypeContext
        ContextKind.VALUE_OR_TYPE -> element.typeMatchesForValueContext || element.typeMatchesForTypeContext
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
        element is KtNamedFunction && lastDirectionIs(LastDirection.PARENT)
                || element is KtProperty && !element.isLocal && lastDirectionIs(LastDirection.PARENT)

    private fun shouldStopBeforeProcessing(element: KtElement): Boolean =
        element is KtClassOrObject && lastDirectionIs(LastDirection.PARENT)
                || element is KtFile || (element.parent is KtBlockExpression && element.parent.parent is KtScript)
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

        element.contextParameters.processMany(::processParameter)
    }

    override fun visitClass(klass: KtClass) {
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

    private fun nameMatchesAndIsValidCandidate(element: KtNamedDeclaration): Boolean =
        element.nameAsSafeName == name && !isIgnored(element)

    private fun foundIfNameMatches(element: KtNamedDeclaration) {
        if (nameMatchesAndIsValidCandidate(element)) {
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

    private inline fun <T> Iterable<T>.processMany(f: (T) -> Unit) {
        if (_found != null) return

        for (element in this) {
            f(element)
            if (_found != null) return
        }
    }
}
