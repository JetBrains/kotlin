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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.container.PlatformExtensionsClashResolver
import org.jetbrains.kotlin.container.PlatformSpecificExtension
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

interface IdentifierChecker : PlatformSpecificExtension<IdentifierChecker> {
    fun checkIdentifier(simpleNameExpression: KtSimpleNameExpression, diagnosticHolder: DiagnosticSink)
    fun checkDeclaration(declaration: KtDeclaration, diagnosticHolder: DiagnosticSink)

    object Default : IdentifierChecker {
        override fun checkIdentifier(simpleNameExpression: KtSimpleNameExpression, diagnosticHolder: DiagnosticSink) {}
        override fun checkDeclaration(declaration: KtDeclaration, diagnosticHolder: DiagnosticSink) {}
    }
}

class IdentifierCheckerClashesResolver : PlatformExtensionsClashResolver<IdentifierChecker>(IdentifierChecker::class.java) {
    override fun resolveExtensionsClash(extensions: List<IdentifierChecker>): IdentifierChecker = CompositeIdentifierChecker(extensions)
}

// Launches every underlying checker
class CompositeIdentifierChecker(private val identifierCheckers: List<IdentifierChecker>) : IdentifierChecker {
    override fun checkIdentifier(simpleNameExpression: KtSimpleNameExpression, diagnosticHolder: DiagnosticSink) {
        identifierCheckers.forEach { it.checkIdentifier(simpleNameExpression, diagnosticHolder) }
    }

    override fun checkDeclaration(declaration: KtDeclaration, diagnosticHolder: DiagnosticSink) {
        identifierCheckers.forEach { it.checkDeclaration(declaration, diagnosticHolder) }
    }
}