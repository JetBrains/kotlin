/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.api

import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isInImportDirective
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.synthetic.JavaSyntheticPropertiesScope

internal class IncompatibleAPIKotlinVisitor(
    private val holder: ProblemsHolder,
    private val problemsCache: ProblemsCache
) : KtVisitorVoid() {
    override fun visitImportList(importList: KtImportList) {
        // Do not report anything in imports
    }

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        val nameStr = expression.text
        if (!Name.isValidIdentifier(nameStr)) {
            return
        }

        val names = HashSet<String>()
        names.add(nameStr)

        val gettersNames = JavaSyntheticPropertiesScope.possibleGetMethodNames(
            Name.identifier(nameStr)
        )
        if (!gettersNames.isEmpty()) {
            names.addAll(gettersNames.map { it.identifier })
            names.add(JavaSyntheticPropertiesScope.setMethodName(gettersNames.first()).identifier)
        }

        if (names.none { name -> problemsCache.containsWord(name) }) {
            return
        }

        checkReference(expression)
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)

        if (function.modifierList?.getModifier(KtTokens.OVERRIDE_KEYWORD) == null) {
            return
        }

        val funName = function.name
        if (funName == null || !problemsCache.containsWord(funName)) {
            return
        }

        val functionDescriptor = function.resolveToDescriptorIfAny() ?: return
        for (overriddenDescriptor in functionDescriptor.original.overriddenDescriptors) {
            val psi = overriddenDescriptor.source.getPsi() ?: continue
            val problem = findProblem(psi, problemsCache)
            if (problem != null) {
                registerProblemForElement(function.nameIdentifier, holder, problem)
                return
            }
        }
    }

    private fun checkReference(expression: KtSimpleNameExpression) {
        if (expression.isInImportDirective()) {
            // Ignore imports
            return
        }

        for (reference in expression.references) {
            val resolveTo = reference.resolve()
            val problem = findProblem(resolveTo, problemsCache) ?: continue

            registerProblemForReference(reference, holder, problem)
            break
        }
    }
}