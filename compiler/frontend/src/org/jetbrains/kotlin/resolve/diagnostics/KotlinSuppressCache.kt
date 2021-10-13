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

package org.jetbrains.kotlin.resolve.diagnostics

import com.google.common.collect.ImmutableSet
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.util.ExtensionProvider

interface DiagnosticSuppressor {
    fun isSuppressed(diagnostic: Diagnostic): Boolean
    fun isSuppressed(diagnostic: Diagnostic, bindingContext: BindingContext?): Boolean = isSuppressed(diagnostic)

    companion object {
        val EP_NAME: ExtensionPointName<DiagnosticSuppressor> =
            ExtensionPointName.create("org.jetbrains.kotlin.diagnosticSuppressor")
    }
}

abstract class AbstractKotlinSuppressCache<Element> {
    // The cache is weak: we're OK with losing it
    protected val suppressors = ContainerUtil.createConcurrentWeakValueMap<Element, Suppressor<Element>>()

    fun isSuppressed(element: Element, suppressionKey: String, severity: Severity) =
        isSuppressed(StringSuppressRequest(element, severity, suppressionKey.lowercase()))

    protected open fun isSuppressed(request: SuppressRequest<Element>): Boolean {

        val annotated = getParentAnnotatedElement(request.element, false) ?: return false

        return isSuppressedByAnnotated(request.suppressKey, request.severity, annotated, 0)

    }

    protected abstract fun getParentAnnotatedElement(element: Element, strict: Boolean): Element?

    /*
       The cache is optimized for the case where no warnings are suppressed (most frequent one)

       trait Root {
         suppress("X")
         trait A {
           trait B {
             suppress("Y")
             trait C {
               fun foo() = warning
             }
           }
         }
       }

       Nothing is suppressed at foo, so we look above. While looking above we went up to the root (once) and propagated
       all the suppressors down, so now we have:

          foo  - suppress(Y) from C
          C    - suppress(Y) from C
          B    - suppress(X) from A
          A    - suppress(X) from A
          Root - suppress() from Root

       Next time we look up anything under foo, we try the Y-suppressor and then immediately the X-suppressor, then to the empty
       suppressor at the root. All the intermediate empty nodes are skipped, because every suppressor remembers its definition point.

       This way we need no more lookups than the number of suppress() annotations from here to the root.
     */
    protected open fun isSuppressedByAnnotated(
        suppressionKey: String,
        severity: Severity,
        annotated: Element,
        debugDepth: Int
    ): Boolean {
        val suppressor = getOrCreateSuppressor(annotated)
        if (suppressor.isSuppressed(suppressionKey, severity)) return true

        val annotatedAbove = getParentAnnotatedElement(suppressor.annotatedElement, true) ?: return false

        val suppressed = isSuppressedByAnnotated(suppressionKey, severity, annotatedAbove, debugDepth + 1)
        val suppressorAbove = suppressors[annotatedAbove]
        if (suppressorAbove != null && suppressorAbove.dominates(suppressor)) {
            suppressors[annotated] = suppressorAbove
        }

        return suppressed
    }

    protected fun getOrCreateSuppressor(annotated: Element): Suppressor<Element> =
        suppressors.getOrPut(annotated) {
            val strings = getSuppressingStrings(annotated)
            when (strings.size) {
                0 -> EmptySuppressor(annotated)
                1 -> SingularSuppressor(annotated, strings.first())
                else -> MultiSuppressor(annotated, strings)
            }
        }

    protected abstract fun getSuppressingStrings(annotated: Element): Set<String>

    companion object {
        private fun isSuppressedByStrings(key: String, strings: Set<String>, severity: Severity): Boolean =
            severity == Severity.WARNING && "warnings" in strings || key.lowercase() in strings
    }

    protected abstract class Suppressor<Element>(val annotatedElement: Element) {
        abstract fun isSuppressed(suppressionKey: String, severity: Severity): Boolean

        // true is \forall x. other.isSuppressed(x) -> this.isSuppressed(x)
        abstract fun dominates(other: Suppressor<Element>): Boolean
    }

    private class EmptySuppressor<Element>(annotated: Element) : Suppressor<Element>(annotated) {
        override fun isSuppressed(suppressionKey: String, severity: Severity): Boolean = false
        override fun dominates(other: Suppressor<Element>): Boolean = other is EmptySuppressor
    }

    private class SingularSuppressor<Element>(annotated: Element, private val string: String) : Suppressor<Element>(annotated) {
        override fun isSuppressed(suppressionKey: String, severity: Severity): Boolean {
            return isSuppressedByStrings(suppressionKey, ImmutableSet.of(string), severity)
        }

        override fun dominates(other: Suppressor<Element>): Boolean {
            return other is EmptySuppressor || (other is SingularSuppressor && other.string == string)
        }
    }

    private class MultiSuppressor<Element>(annotated: Element, private val strings: Set<String>) : Suppressor<Element>(annotated) {
        override fun isSuppressed(suppressionKey: String, severity: Severity): Boolean {
            return isSuppressedByStrings(suppressionKey, strings, severity)
        }

        override fun dominates(other: Suppressor<Element>): Boolean {
            // it's too costly to check set inclusion
            return other is EmptySuppressor
        }
    }

    protected interface SuppressRequest<Element> {
        val element: Element
        val severity: Severity
        val suppressKey: String
    }

    private class StringSuppressRequest<Element>(
        override val element: Element,
        override val severity: Severity,
        override val suppressKey: String
    ) : SuppressRequest<Element>
}

abstract class KotlinSuppressCache : AbstractKotlinSuppressCache<PsiElement>() {

    private val diagnosticSuppressors = ExtensionProvider.create(DiagnosticSuppressor.EP_NAME)

    val filter: (Diagnostic) -> Boolean = { diagnostic: Diagnostic ->
        !isSuppressed(DiagnosticSuppressRequest(diagnostic))
    }

    protected open fun isSuppressedByExtension(suppressor: DiagnosticSuppressor, diagnostic: Diagnostic): Boolean {
        return suppressor.isSuppressed(diagnostic)
    }

    abstract fun getSuppressionAnnotations(annotated: PsiElement): List<AnnotationDescriptor>

    override fun getSuppressingStrings(annotated: PsiElement): Set<String> {
        val builder = ImmutableSet.builder<String>()

        for (annotationDescriptor in getSuppressionAnnotations(annotated)) {
            processAnnotation(builder, annotationDescriptor)
        }

        return builder.build()
    }

    private fun processAnnotation(builder: ImmutableSet.Builder<String>, annotationDescriptor: AnnotationDescriptor) {
        if (annotationDescriptor.fqName != StandardNames.FqNames.suppress) return

        // We only add strings and skip other values to facilitate recovery in presence of erroneous code
        for (arrayValue in annotationDescriptor.allValueArguments.values) {
            if (arrayValue is ArrayValue) {
                for (value in arrayValue.value) {
                    if (value is StringValue) {
                        builder.add(value.value.lowercase())
                    }
                }
            }
        }
    }

    override fun isSuppressed(request: SuppressRequest<PsiElement>): Boolean {
        // If diagnostics are reported in a synthetic file generated by KtPsiFactory (dummy.kt),
        // there's no point to present such diagnostics to the user, because the user didn't write this code
        val element = request.element
        if (!element.isValid) return true

        val file = element.containingFile
        if (file is KtFile) {
            if (file.doNotAnalyze != null) return true
        }

        if (request is DiagnosticSuppressRequest) {
            for (suppressor in diagnosticSuppressors.get()) {
                if (isSuppressedByExtension(suppressor, request.diagnostic)) return true
            }
        }
        return super.isSuppressed(request)
    }

    override fun getParentAnnotatedElement(element: PsiElement, strict: Boolean) =
        KtStubbedPsiUtil.getPsiOrStubParent(element, KtAnnotated::class.java, strict)

    protected class DiagnosticSuppressRequest(val diagnostic: Diagnostic) : SuppressRequest<PsiElement> {
        override val element: PsiElement get() = diagnostic.psiElement
        override val severity: Severity get() = diagnostic.severity
        override val suppressKey: String get() = getDiagnosticSuppressKey(diagnostic)
    }

    companion object {
        internal fun getDiagnosticSuppressKey(diagnostic: Diagnostic): String =
            diagnostic.factory.name.lowercase()
    }
}

class BindingContextSuppressCache(val context: BindingContext) : KotlinSuppressCache() {
    override fun getSuppressionAnnotations(annotated: PsiElement): List<AnnotationDescriptor> {
        val descriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, annotated)

        return descriptor?.annotations?.toList()
            ?: (annotated as? KtAnnotated)?.annotationEntries?.mapNotNull { context.get(BindingContext.ANNOTATION, it) }
            ?: emptyList()
    }

    override fun isSuppressedByExtension(suppressor: DiagnosticSuppressor, diagnostic: Diagnostic): Boolean {
        return suppressor.isSuppressed(diagnostic, context)
    }
}
