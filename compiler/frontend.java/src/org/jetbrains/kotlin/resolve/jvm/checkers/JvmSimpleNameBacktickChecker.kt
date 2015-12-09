/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.IdentifierChecker

object JvmSimpleNameBacktickChecker : IdentifierChecker {
    // See The Java Virtual Machine Specification, section 4.7.9.1 https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.9.1
    private val CHARS = setOf('.', ';', '[', ']', '/', '<', '>', ':', '\\')

    override fun checkIdentifier(identifier: PsiElement?, diagnosticHolder: DiagnosticSink) {
        if (identifier == null) return

        reportIfNeeded(identifier.text, identifier, diagnosticHolder)
    }

    override fun checkDeclaration(declaration: KtDeclaration, diagnosticHolder: DiagnosticSink) {
        if (declaration is KtDestructuringDeclaration) {
            declaration.entries.forEach { checkNamed(it, diagnosticHolder) }
        }
        if (declaration is KtCallableDeclaration) {
            declaration.valueParameters.forEach { checkNamed(it, diagnosticHolder) }
        }
        if (declaration is KtTypeParameterListOwner) {
            declaration.typeParameters.forEach { checkNamed(it, diagnosticHolder) }
        }
        if (declaration is KtNamedDeclaration) {
            checkNamed(declaration, diagnosticHolder)
        }
    }

    fun checkNamed(declaration: KtNamedDeclaration, diagnosticHolder: DiagnosticSink) {
        val name = declaration.name ?: return

        val element = declaration.nameIdentifier ?: declaration
        reportIfNeeded(name, element, diagnosticHolder)
    }

    private fun reportIfNeeded(name: String, element: PsiElement, diagnosticHolder: DiagnosticSink) {
        val text = KtPsiUtil.unquoteIdentifier(name)
        if (text.isEmpty()) {
            diagnosticHolder.report(Errors.INVALID_CHARACTERS.on(element, "should not be empty"))
        }
        else if (text.any { it in CHARS }) {
            diagnosticHolder.report(Errors.INVALID_CHARACTERS.on(element, "contains illegal characters: ${CHARS.intersect(text.toSet()).joinToString("")}"))
        }
    }
}

