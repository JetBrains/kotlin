/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.assignmentKeyFactory
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.name.Name

interface AssignmentKey {
    val isAugmentedAssignment: Boolean
}

interface LexicalScopeKey

class AssignmentLexicalScope(
    val key: LexicalScopeKey,
    val children: Map<LexicalScopeKey, AssignmentLexicalScope>,
    val insideAssignments: Map<FirPropertySymbol, Set<AssignmentKey>>,
    val followingAssignments: Map<FirPropertySymbol, Set<AssignmentKey>>,
) {
    companion object {
        fun empty(key: LexicalScopeKey): AssignmentLexicalScope {
            return AssignmentLexicalScope(
                key = key,
                children = emptyMap(),
                insideAssignments = emptyMap(),
                followingAssignments = emptyMap()
            )
        }
    }

    internal class Builder private constructor(
        val key: LexicalScopeKey,
        // Properties which are declared before this scope.
        val visibleProperties: Set<FirPropertySymbol> = emptySet(),
        // The surrounding lexical scope builder.
        val parent: Builder? = null,
    ) {
        val children = mutableSetOf<Builder>()

        // Assignments which happen anywhere after this scope, to properties declared before this scope.
        val following = mutableMapOf<FirPropertySymbol, MutableSet<AssignmentKey>>()

        // Assignments which happen anywhere within this scope, to properties declared before this scope.
        val inside = mutableMapOf<FirPropertySymbol, MutableSet<AssignmentKey>>()

        // All declared local properties declared before or within this scope.
        private val declaredProperties = mutableSetOf<FirPropertySymbol>()

        // Declared properties by name; used to sudo-resolve references to local properties.
        private val namedProperties = mutableMapOf<Name, FirPropertySymbol>()

        companion object {
            fun start(key: LexicalScopeKey): Builder {
                return Builder(key)
            }
        }

        fun child(key: LexicalScopeKey = this.key): Builder {
            val builder = Builder(key, visibleProperties = declaredProperties.toSet(), parent = this)
            children.add(builder)
            builder.namedProperties.putAll(namedProperties)
            builder.declaredProperties.addAll(declaredProperties)
            return builder
        }

        fun declare(property: FirPropertySymbol) {
            declaredProperties.add(property)
            namedProperties[property.name] = property
        }

        fun add(name: Name, assignment: AssignmentKey) {
            val property = namedProperties[name] ?: return
            propagateUp(source = this, property, assignment)
        }

        private fun propagateUp(source: Builder, property: FirPropertySymbol, assignment: AssignmentKey) {
            if (property in visibleProperties) {
                inside.getOrPut(property) { mutableSetOf() }.add(assignment)
            }

            propagateDown(source, property, assignment) // Add assignment to all children.

            // If the property is not declared in the parent scope, there is no need to continue propagating.
            if (parent != null && property in parent.declaredProperties) {
                parent.propagateUp(source = this, property, assignment)
            }
        }

        private fun propagateDown(child: Builder, property: FirPropertySymbol, assignment: AssignmentKey) {
            for (builder in children) {
                // If the property is not visible to the child scope, there is no need to add or continue propagating.
                if (builder !== child && property in builder.visibleProperties) {
                    builder.following.getOrPut(property) { mutableSetOf() }.add(assignment)
                    builder.propagateDown(child, property, assignment)
                }
            }
        }

        fun addFollowingFrom(other: Builder) {
            var updated = false
            for ([property, assignments] in other.inside) {
                if (property in visibleProperties) {
                    updated = following.getOrPut(property) { mutableSetOf() }.addAll(assignments) || updated
                }
            }
            if (updated) {
                for (builder in children) {
                    builder.addFollowingFrom(other)
                }
            }
        }

        fun build(): AssignmentLexicalScope {
            return AssignmentLexicalScope(
                key = key,
                children = children.associate { it.key to it.build() },
                insideAssignments = inside,
                followingAssignments = following,
            )
        }
    }
}

fun AssignmentLexicalScope.findScope(key: LexicalScopeKey): AssignmentLexicalScope? {
    children[key]?.let { return it }
    for (child in children.values) {
        child.findScope(key)?.let { return it }
    }
    return null
}

context(holder: SessionHolder)
fun buildAssignmentLexicalScope(declaration: FirDeclaration): AssignmentLexicalScope {
    val builder = AssignmentLexicalScope.Builder.start(key = assignmentKeyFactory.createLexicalScopeKey(declaration))
    BuilderVisitor(holder.session, builder).visitElement(declaration)
    return builder.build()
}

private class BuilderVisitor(
    override val session: FirSession,
    var builder: AssignmentLexicalScope.Builder,
) : FirDefaultVisitorVoid(), SessionHolder {
    private inline fun withFork(key: LexicalScopeKey, block: () -> Unit): AssignmentLexicalScope.Builder {
        val parent = builder
        val child = parent.child(key)

        builder = child
        block()
        builder = parent

        return child
    }

    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    // Local Properties and Assignments

    override fun visitProperty(property: FirProperty) {
        visitElement(property)
        if (property.isEffectivelyLocal) {
            builder.declare(property.symbol)
        }
    }

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment) {
        visitElement(variableAssignment)
        if (variableAssignment.explicitReceiver != null) return
        val name = (variableAssignment.calleeReference as? FirNamedReference)?.name ?: return
        builder.add(name, assignmentKeyFactory.createAssignmentKey(variableAssignment))
    }

    override fun visitAugmentedAssignment(augmentedAssignment: FirAugmentedAssignment) {
        visitElement(augmentedAssignment)
        val lhs = augmentedAssignment.leftArgument as? FirQualifiedAccessExpression ?: return
        if (lhs.explicitReceiver != null) return
        val name = (lhs.calleeReference as? FirNamedReference)?.name ?: return
        builder.add(name, assignmentKeyFactory.createAssignmentKey(augmentedAssignment))
    }

    // Lexical Declarations

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer) {
        visitLexicalDeclaration(anonymousInitializer)
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
        visitLexicalDeclaration(anonymousFunction)
    }

    override fun visitNamedFunction(namedFunction: FirNamedFunction) {
        visitLexicalDeclaration(namedFunction)
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor) {
        if (propertyAccessor is FirDefaultPropertyAccessor) return
        visitLexicalDeclaration(propertyAccessor)
    }

    override fun visitConstructor(constructor: FirConstructor) {
        visitLexicalDeclaration(constructor)
    }

    override fun visitClass(klass: FirClass) {
        visitLexicalDeclaration(klass)
    }

    private fun visitLexicalDeclaration(declaration: FirDeclaration) {
        withFork(key = assignmentKeyFactory.createLexicalScopeKey(declaration)) {
            declaration.acceptChildren(this)
        }
    }

    // Function Calls

    override fun visitFunctionCall(functionCall: FirFunctionCall) {
        functionCall.explicitReceiver?.accept(this)
        if (functionCall.dispatchReceiver !== functionCall.explicitReceiver) {
            functionCall.dispatchReceiver?.accept(this)
        }
        if (functionCall.extensionReceiver !== functionCall.explicitReceiver && functionCall.extensionReceiver !== functionCall.dispatchReceiver) {
            functionCall.extensionReceiver?.accept(this)
        }

        // Delay processing of lambda args because lambda body are evaluated after all arguments have been evaluated.
        val [lambdas, values] = functionCall.argumentList.arguments.partition { it.unwrapAnonymousFunctionExpression() != null }
        values.forEach { it.accept(this) }
        lambdas.forEach { it.accept(this) }

        functionCall.calleeReference.accept(this)
    }

    // Branches

    override fun visitWhenExpression(whenExpression: FirWhenExpression) {
        whenExpression.subjectVariable?.accept(this)
        for (branch in whenExpression.branches) {
            withFork(key = assignmentKeyFactory.createLexicalScopeKey(branch)) {
                branch.accept(this)
            }
        }
    }

    // Catches

    override fun visitTryExpression(tryExpression: FirTryExpression) {
        tryExpression.tryBlock.accept(this)
        for (catch in tryExpression.catches) {
            withFork(key = assignmentKeyFactory.createLexicalScopeKey(catch)) {
                catch.accept(this)
            }
        }
        tryExpression.finallyBlock?.accept(this)
    }

    // Loops

    override fun visitLoop(loop: FirLoop) {
        val builder = withFork(key = assignmentKeyFactory.createLexicalScopeKey(loop)) {
            loop.acceptChildren(this)
        }
        // A loop's children must include the inside of the loop.
        for (child in builder.children) {
            child.addFollowingFrom(builder)
        }
    }

    override fun visitWhileLoop(whileLoop: FirWhileLoop) {
        visitLoop(whileLoop)
    }

    override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop) {
        visitLoop(doWhileLoop)
    }

    // Misc. FIR Structure Oddities

    override fun visitWrappedDelegateExpression(wrappedDelegateExpression: FirWrappedDelegateExpression) {
        // An FirWrappedDelegateExpression contains the same expression twice for delegate creation:
        // 'provideDelegateCall' contains 'expression' as its explicit receiver in a 'providedDelegate()' call.
        // This causes 'expression' to be visited twice, thus an exponential growth in the number of expressions being analyzed.
        // So, only visit 'expression', as the local variable analysis will be the same.
        wrappedDelegateExpression.expression.accept(this)
    }

    override fun visitReplDeclarationReference(replDeclarationReference: FirReplDeclarationReference) {
        replDeclarationReference.symbol.fir.accept(this)
    }

    override fun visitReplPropertyInitializer(replPropertyInitializer: FirReplPropertyInitializer) {
        replPropertyInitializer.propertySymbol.fir.accept(this)
    }

    override fun visitReplPropertyDelegate(replPropertyDelegate: FirReplPropertyDelegate) {
        replPropertyDelegate.propertySymbol.fir.accept(this)
    }

    override fun visitReplExpressionReference(replExpressionReference: FirReplExpressionReference) {
        replExpressionReference.expressionRef.value.accept(this)
    }
}
