/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.impl.deduplicating
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.hasEqualFqName
import org.jetbrains.kotlin.ir.util.sourceElement
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.name.FqName
import java.util.*

class KtDiagnosticReporterWithImplicitIrBasedContext(
    diagnosticReporter: DiagnosticReporter,
    val languageVersionSettings: LanguageVersionSettings
) : DiagnosticReporter(), IrDiagnosticReporter {
    val diagnosticReporter: DiagnosticReporter = diagnosticReporter.deduplicating()

    override val hasErrors: Boolean get() = diagnosticReporter.hasErrors
    override val hasWarningsForWError: Boolean get() = diagnosticReporter.hasWarningsForWError

    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
        diagnosticReporter.report(diagnostic, context)
    }

    private val suppressCache = IrBasedSuppressCache()

    private fun IrElement.toSourceElement(containingIrFile: IrFile): AbstractKtSourceElement? {
        return PsiSourceManager.findPsiElement(this, containingIrFile)?.let(::KtRealPsiSourceElement)
            ?: (this as? IrMetadataSourceOwner)?.metadata?.source
            ?: sourceElement()
    }

    override fun at(irElement: IrElement, containingIrDeclaration: IrDeclaration): IrDiagnosticReporter.IrDiagnosticContext {
        return at(irElement, containingIrDeclaration.file)
    }

    override fun at(irDeclaration: IrDeclaration): IrDiagnosticReporter.IrDiagnosticContext {
        return at(irDeclaration, irDeclaration.file)
    }

    override fun at(irElement: IrElement, containingIrFile: IrFile): IrDiagnosticReporter.IrDiagnosticContext {
        return at(irElement.toSourceElement(containingIrFile), irElement, containingIrFile)
    }

    override fun at(
        sourceElement: AbstractKtSourceElement?,
        irElement: IrElement,
        containingFile: IrFile
    ): IrDiagnosticReporter.IrDiagnosticContext {
        return DiagnosticContextWithSuppressionImpl(sourceElement, irElement, containingFile)
    }

    override fun report(factory: KtSourcelessDiagnosticFactory, message: String) {
        val context = object : DiagnosticContext {
            override val containingFilePath: String?
                get() = null

            override fun isDiagnosticSuppressed(diagnostic: KtDiagnostic): Boolean = false
            override val languageVersionSettings: LanguageVersionSettings
                get() = this@KtDiagnosticReporterWithImplicitIrBasedContext.languageVersionSettings
        }
        val diagnostic = factory.create(message, location = null, context) ?: return
        report(diagnostic, context)
    }

    internal inner class DiagnosticContextWithSuppressionImpl(
        override val sourceElement: AbstractKtSourceElement?,
        private val irElement: IrElement,
        private val containingFile: IrFile
    ) : IrDiagnosticReporter.IrDiagnosticContext {
        override val containingFilePath: String = containingFile.path

        override val languageVersionSettings: LanguageVersionSettings
            get() = this@KtDiagnosticReporterWithImplicitIrBasedContext.languageVersionSettings


        override fun isDiagnosticSuppressed(diagnostic: KtDiagnostic): Boolean =
            suppressCache.isSuppressed(
                irElement, containingFile, diagnostic.factory.name.lowercase(), diagnostic.severity
            )

        override fun report(factory: KtDiagnosticFactory0) {
            sourceElement?.let {
                reportOn(it, factory)
            }
        }

        override fun <A : Any> report(factory: KtDiagnosticFactory1<A>, a: A) {
            sourceElement?.let {
                reportOn(it, factory, a)
            }
        }

        override fun <A : Any, B : Any> report(factory: KtDiagnosticFactory2<A, B>, a: A, b: B) {
            sourceElement?.let {
                reportOn(it, factory, a, b)
            }
        }

        override fun <A : Any, B : Any, C : Any> report(factory: KtDiagnosticFactory3<A, B, C>, a: A, b: B, c: C) {
            sourceElement?.let {
                reportOn(it, factory, a, b, c)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is IrDiagnosticReporter.IrDiagnosticContext) return false

            if (sourceElement != other.sourceElement) return false
            if (containingFilePath != other.containingFilePath) return false
            if (languageVersionSettings != other.languageVersionSettings) return false

            return true
        }

        override fun hashCode(): Int {
            var result = sourceElement?.hashCode() ?: 0
            result = 31 * result + containingFilePath.hashCode()
            result = 31 * result + languageVersionSettings.hashCode()
            return result
        }
    }
}

internal class IrBasedSuppressCache : AbstractKotlinSuppressCache<IrElement>() {

    private val annotatedAncestorsPerRoot = mutableMapOf<IrElement, MutableMap<IrElement, IrElement>>()

    private val annotationKeys = mutableMapOf<IrElement, Set<String>>()

    @Synchronized
    private fun ensureRootProcessed(rootElement: IrElement) =
        annotatedAncestorsPerRoot.getOrPut(rootElement) {
            val visitor = AnnotatedTreeVisitor()
            rootElement.accept(visitor, Stack())
            visitor.annotatedAncestors
        }

    private inner class AnnotatedTreeVisitor : IrVisitor<Unit, Stack<IrElement>>() {

        val annotatedAncestors = mutableMapOf<IrElement, IrElement>()

        override fun visitElement(element: IrElement, data: Stack<IrElement>) {
            if (data.isNotEmpty()) {
                annotatedAncestors[element] = data.peek()
            }
            val isAnnotated = collectSuppressAnnotationKeys(element)
            if (isAnnotated) {
                data.push(element)
            }
            element.acceptChildren(this, data)
            if (isAnnotated) {
                data.pop()
            }
        }

        private fun collectSuppressAnnotationKeys(element: IrElement): Boolean =
            (element as? IrAnnotationContainer)?.annotations?.filter {
                it.type.classOrNull?.owner?.hasEqualFqName(SUPPRESS) == true
            }?.flatMap {
                buildList {
                    fun addIfStringConst(irConst: IrConst) {
                        if (irConst.kind == IrConstKind.String) {
                            add((irConst.value as String).lowercase())
                        }
                    }

                    for (arg in it.arguments) {
                        when (arg) {
                            is IrConst -> addIfStringConst(arg)
                            is IrConstantArray -> arg.elements.filterIsInstance<IrConstantPrimitive>().forEach {
                                addIfStringConst(it.value)
                            }
                            // TODO: consider leaving only this branch
                            is IrVararg -> arg.elements.filterIsInstance<IrConst>().forEach {
                                addIfStringConst(it)
                            }
                        }
                    }
                }
            }?.takeIf { it.isNotEmpty() }?.also {
                annotationKeys[element] = it.toSet()
            } != null
    }

    override fun getClosestAnnotatedAncestorElement(element: IrElement, rootElement: IrElement, excludeSelf: Boolean): IrElement? {
        val annotatedAncestors = ensureRootProcessed(rootElement)
        return if (!excludeSelf && annotationKeys.containsKey(element)) element else annotatedAncestors[element]
    }

    override fun getSuppressingStrings(annotated: IrElement): Set<String> = annotationKeys[annotated].orEmpty()
}

private val SUPPRESS = FqName("kotlin.Suppress")
