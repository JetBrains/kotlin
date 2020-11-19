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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.util.ArrayList

object AnalyzingUtils {
    private const val WRITE_DEBUG_TRACE_NAMES = false

    @JvmStatic
    fun checkForSyntacticErrors(root: PsiElement) {
        root.acceptChildren(object : PsiErrorElementVisitor() {
            override fun visitErrorElement(element: PsiErrorElement) {
                throw IllegalArgumentException(
                    element.errorDescription + "; looking at " +
                            element.node.elementType + " '" +
                            element.text + PsiDiagnosticUtils.atLocation(element)
                )
            }
        })
    }

    @JvmStatic
    fun getSyntaxErrorRanges(root: PsiElement): List<PsiErrorElement> {
        val r: MutableList<PsiErrorElement> = ArrayList()
        root.acceptChildren(object : PsiErrorElementVisitor() {
            override fun visitErrorElement(element: PsiErrorElement) {
                r.add(element)
            }
        })
        return r
    }

    fun throwExceptionOnErrors(bindingContext: BindingContext) {
        throwExceptionOnErrors(bindingContext.diagnostics)
    }

    fun throwExceptionOnErrors(diagnostics: Diagnostics) {
        for (diagnostic in diagnostics) {
            DiagnosticSink.THROW_EXCEPTION.report(diagnostic)
        }
    }

    // --------------------------------------------------------------------------------------------------------------------------
    @JvmStatic
    fun formDebugNameForBindingTrace(debugName: String, resolutionSubjectForMessage: Any?): String {
        if (WRITE_DEBUG_TRACE_NAMES) {
            val debugInfo = StringBuilder(debugName)
            if (resolutionSubjectForMessage is KtElement) {
                debugInfo.append(" ").append(resolutionSubjectForMessage.getDebugText())
                //debugInfo.append(" in ").append(element.getContainingFile().getName());
                debugInfo.append(" in ").append(resolutionSubjectForMessage.containingKtFile.name).append(" ").append(
                    resolutionSubjectForMessage.textOffset
                )
            } else if (resolutionSubjectForMessage != null) {
                debugInfo.append(" ").append(resolutionSubjectForMessage)
            }
            return debugInfo.toString()
        }
        return ""
    }

    abstract class PsiErrorElementVisitor : KtTreeVisitorVoid() {
        abstract override fun visitErrorElement(element: PsiErrorElement)
    }
}