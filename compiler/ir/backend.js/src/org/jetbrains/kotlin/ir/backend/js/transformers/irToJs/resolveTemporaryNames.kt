/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.js.backend.ast.*

fun JsNode.resolveTemporaryNames() {
    val renamings = resolveNames()
    accept(object : RecursiveJsVisitor() {
        override fun visitElement(node: JsNode) {
            super.visitElement(node)
            if (node is HasName) {
                val name = node.name
                if (name != null) {
                    renamings[name]?.let { node.name = it }
                }
            }
        }
    })
}

private fun JsNode.resolveNames(): Map<JsName, JsName> {
    val rootScope = computeScopes().liftUsedNames()
    val replacements = hashMapOf<JsName, JsName>()
    fun traverse(scope: Scope) {
        // Don't clash with non-temporary names declared in current scope. It's for rare cases like `_` or `Kotlin` names,
        // since most of local declarations are temporary.
        val occupiedNames = scope.declaredNames.asSequence().filter { !it.isTemporary }.map { it.ident }.toMutableSet()

        // Don't clash with non-temporary names used in current scope. It's ok to clash with unused names.
        // Don't clash with used temporary names from outer scopes that get their resolved names. For example,
        // when function declares temporary name `foo` and inner function both declares temporary name `foo` and uses
        // (directly or propagates to inner scope) outer `foo`, we should resolve distinct strings for these `foo` names.
        // Outer `foo` resolves first, so when traversing inner scope, we should take it into account.
        occupiedNames += scope.usedNames.asSequence().mapNotNull { if (!it.isTemporary) it.ident else replacements[it]?.ident }

        val nextSuffix = hashMapOf<String, Int>()
        for (temporaryName in scope.declaredNames.asSequence().filter { it.isTemporary }) {
            var resolvedName = temporaryName.ident
            var suffix = nextSuffix.getOrDefault(temporaryName.ident, 0)
            while (resolvedName in JsDeclarationScope.RESERVED_WORDS || resolvedName in occupiedNames) {
                resolvedName = "${temporaryName.ident}_${suffix++}"
            }
            nextSuffix[temporaryName.ident] = suffix
            replacements[temporaryName] = JsDynamicScope.declareName(resolvedName).apply { copyMetadataFrom(temporaryName) }
            occupiedNames += resolvedName
        }
        scope.children.forEach(::traverse)
    }

    traverse(rootScope)

    return replacements
}

// When name is used by inner scope, it's implicitly used by outer scopes
private fun Scope.liftUsedNames(): Scope {
    fun traverse(scope: Scope) {
        scope.children.forEach { child ->
            scope.usedNames += child.declaredNames
            traverse(child)
            scope.usedNames += child.usedNames
        }
    }
    traverse(this)
    return this
}

private fun JsNode.computeScopes(): Scope {
    val rootScope = Scope()
    accept(object : RecursiveJsVisitor() {
        var currentScope: Scope = rootScope

        override fun visitClass(x: JsClass) {
            x.name?.let { currentScope.declaredNames += it }
            // We need it to not rename methods and fields inside class body
            // Because if they are in clash with something, it means overriding
            x.constructor?.accept(this)
            x.baseClass?.accept(this)
            x.members.forEach { visitFunction(it, shouldReserveName = false) }
        }

        override fun visitFunction(x: JsFunction) {
            visitFunction(x, shouldReserveName = true)
        }

        fun visitFunction(x: JsFunction, shouldReserveName: Boolean) {
            x.name?.takeIf { shouldReserveName }?.let { currentScope.declaredNames += it }
            val oldScope = currentScope
            currentScope = Scope().apply {
                currentScope.children += this
            }
            currentScope.declaredNames += x.parameters.map { it.name }
            super.visitFunction(x)
            currentScope = oldScope
        }

        override fun visitCatch(x: JsCatch) {
            currentScope.declaredNames += x.parameter.name
            super.visitCatch(x)
        }

        override fun visit(x: JsVars.JsVar) {
            currentScope.declaredNames += x.name
            super.visit(x)
        }

        override fun visitNameRef(nameRef: JsNameRef) {
            if (nameRef.qualifier == null) {
                val name = nameRef.name
                currentScope.usedNames += name ?: JsDynamicScope.declareName(nameRef.ident)
            }

            super.visitNameRef(nameRef)
        }

        override fun visitImport(import: JsImport) {
            when (val target = import.target) {
                is JsImport.Target.All -> target.alias.name?.let { currentScope.declaredNames += it }
                is JsImport.Target.Default -> target.name.name?.let { currentScope.declaredNames += it }
                is JsImport.Target.Elements -> target.elements.forEach {
                    currentScope.declaredNames += it.alias?.name ?: it.name
                }
            }
            super.visitImport(import)
        }

        override fun visitBreak(x: JsBreak) {}

        override fun visitContinue(x: JsContinue) {}
    })

    return rootScope
}

private class Scope {
    val declaredNames = mutableSetOf<JsName>()
    val usedNames = mutableSetOf<JsName>()
    val children = mutableSetOf<Scope>()
}
