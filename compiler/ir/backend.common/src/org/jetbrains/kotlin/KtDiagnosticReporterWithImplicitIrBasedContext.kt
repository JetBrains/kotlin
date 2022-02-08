/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.jetbrains.kotlin.backend.common.psi.PsiSourceManager
import org.jetbrains.kotlin.backend.common.sourceElement
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.AbstractKotlinSuppressCache
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtDiagnosticReporterWithContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.hasEqualFqName
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import java.util.*

class KtDiagnosticReporterWithImplicitIrBasedContext(
    diagnosticReporter: DiagnosticReporter,
    languageVersionSettings: LanguageVersionSettings
) : KtDiagnosticReporterWithContext(diagnosticReporter, languageVersionSettings) {

    private val suppressCache = IrBasedSuppressCache()

    private fun IrElement.toSourceElement(containingIrFile: IrFile) =
        (PsiSourceManager.findPsiElement(this, containingIrFile)?.let(::KtRealPsiSourceElement)
            ?: sourceElement())

    fun atFirstValidFrom(vararg irElements: IrElement, containingIrFile: IrFile): DiagnosticContextImpl? =
        atFirstValidFrom(irElements.asIterable(), containingIrFile)

    fun atFirstValidFrom(irElements: Iterable<IrElement>, containingIrFile: IrFile): DiagnosticContextImpl? {
        var irElement: IrElement? = null
        for (e in irElements) {
            PsiSourceManager.findPsiElement(e, containingIrFile)?.let {
                return at(KtRealPsiSourceElement(it), e, containingIrFile)
            }
            if (irElement == null && e.startOffset >= 0) {
                irElement = e
            }
        }
        return irElement?.let { at(it.sourceElement(), it, containingIrFile) }
    }

    fun at(irElement: IrElement, containingIrDeclaration: IrDeclaration): DiagnosticContextImpl =
        at(irElement, containingIrDeclaration.file)

    fun at(irDeclaration: IrDeclaration): DiagnosticContextImpl =
        at(irDeclaration, irDeclaration.file)

    fun at(irElement: IrElement, containingIrFile: IrFile): DiagnosticContextImpl =
        at(irElement.toSourceElement(containingIrFile), irElement, containingIrFile)

    fun at(
        sourceElement: AbstractKtSourceElement?,
        irElement: IrElement,
        containingFile: IrFile
    ): DiagnosticContextImpl =
        DiagnosticContextWithSuppressionImpl(sourceElement, irElement, containingFile)

    override fun at(sourceElement: AbstractKtSourceElement?, containingFilePath: String): DiagnosticContextImpl {
        error("Should not be called directly")
    }

    internal inner class DiagnosticContextWithSuppressionImpl(
        sourceElement: AbstractKtSourceElement?,
        private val irElement: IrElement,
        private val containingFile: IrFile
    ) : DiagnosticContextImpl(sourceElement, containingFile.path) {

        override fun isDiagnosticSuppressed(diagnostic: KtDiagnostic): Boolean =
            suppressCache.isSuppressed(
                irElement, containingFile, diagnostic.factory.name.lowercase(), diagnostic.severity
            )
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

    private inner class AnnotatedTreeVisitor : IrElementVisitor<Unit, Stack<IrElement>> {

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
                    fun addIfStringConst(irConst: IrConst<*>) {
                        if (irConst.kind == IrConstKind.String) {
                            add((irConst.value as String).lowercase())
                        }
                    }

                    for (i in 0 until it.valueArgumentsCount) {
                        when (val arg = it.getValueArgument(i)) {
                            is IrConst<*> -> addIfStringConst(arg)
                            is IrConstantArray -> arg.elements.filterIsInstance<IrConstantPrimitive>().forEach {
                                addIfStringConst(it.value)
                            }
                            // TODO: consider leaving only this branch
                            is IrVararg -> arg.elements.filterIsInstance<IrConst<*>>().forEach {
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
