/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.ClassDiagnosticRetriever
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.FileDiagnosticRetriever
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.FileStructureElementDiagnostics
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.ScriptDiagnosticRetriever
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.SingleNonLocalDeclarationDiagnosticRetriever
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isScriptStatement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.psi.*

internal sealed class FileStructureElement {
    abstract val mappings: KtToFirMapping
    abstract val diagnostics: FileStructureElementDiagnostics

    companion object {
        val recorder = FirElementsRecorder()

        fun recorderFor(fir: FirElement): FirElementsRecorder = when (fir) {
            is FirFile -> RootStructureElement.Recorder
            is FirScript -> RootScriptStructureElement.Recorder
            is FirRegularClass -> ClassDeclarationStructureElement.Recorder(fir)
            else -> DeclarationStructureElement.Recorder
        }
    }
}

internal class KtToFirMapping(firElement: FirElement) {
    private val mapping = FirElementsRecorder.recordElementsFrom(
        firElement = firElement,
        recorder = FileStructureElement.recorderFor(firElement),
    )

    fun getElement(ktElement: KtElement): FirElement? {
        return mapping[ktElement]
    }

    fun getFir(element: KtElement): FirElement? {
        var current: PsiElement? = element
        while (
            current == element ||
            current is KtUserType ||
            current is KtTypeReference ||
            current is KtDotQualifiedExpression ||
            current is KtNullableType
        ) {
            // We are still referring to the same element with possible type parameter/name qualification/nullability, hence it is always
            // sane to return corresponding element if present
            if (current is KtElement) getElement(current)?.let { return it }
            current = current.parent
        }

        // Here current is the lowest ancestor that has different corresponding text
        return when (current) {
            // Constants with unary operation (i.e. +1 or -1) are saved as leaf element of FIR tree
            is KtPrefixExpression,
            // There is no separate element for annotation construction call
            is KtAnnotationEntry,
            // We replace source for selector for that of whole expression
            is KtSafeQualifiedExpression,
            // Top level destructuring declarations does not have FIR for r-value at the moment, would probably be changed later
            is KtDestructuringDeclaration,
            // There is no separate FIR node for this in this@foo expressions, same for super@Foo
            is KtThisExpression,
            is KtSuperExpression,
            // Part of path in import/package directives has no FIR node
            is KtImportDirective,
            is KtPackageDirective,
            // Super type refs are not recorded
            is KtSuperTypeCallEntry,
            // this/super in delegation calls are not part of FIR tree, this(args) is
            is KtConstructorDelegationCall,
            // In case of type projection we are not recording corresponding type reference
            is KtTypeProjection,
            // If we have, say, A(), reference A is not recorded, while call A() is recorded
            is KtCallExpression ->
                getElement(current as KtElement)
            is KtBinaryExpression ->
                // Here there is no separate FIR node for partial operator calls (like for a[i] = 1, there is no separate node for a[i])
                if (element is KtArrayAccessExpression || element is KtOperationReferenceExpression) getElement(current) else null
            is KtBlockExpression ->
                // For script initializers we need to return FIR element for script itself
                if (element is KtScriptInitializer) getElement(current.parent as KtScript) else null
            is PsiErrorElement -> {
                val parent = current.parent
                if (parent is KtDestructuringDeclaration) getElement(parent) else null
            }
            // Value argument names and corresponding references are not part of FIR tree
            is KtValueArgumentName -> getElement(current.parent as KtValueArgument)
            is KtContainerNode -> {
                val parent = current.parent
                // Labels in labeled expression (i.e. return@foo) has no FIR node
                if (parent is KtExpressionWithLabel) getElement(parent) else null
            }
            // Enum entries/annotation entries constructor calls
            is KtConstructorCalleeExpression -> getElement(current.parent as KtCallElement)
            // KtParameter for destructuring declaration
            is KtParameter -> getElement(current as KtElement)
            else -> null
        }
    }
}

internal sealed class DeclarationBaseStructureElement<F : FirDeclaration>(val declaration: F) : FileStructureElement() {
    override val mappings: KtToFirMapping = KtToFirMapping(declaration)
}

internal class RootScriptStructureElement(
    file: FirFile,
    script: FirScript,
    moduleComponents: LLFirModuleResolveComponents,
) : DeclarationBaseStructureElement<FirScript>(script) {
    override val diagnostics: FileStructureElementDiagnostics = FileStructureElementDiagnostics(
        file,
        ScriptDiagnosticRetriever(declaration),
        moduleComponents,
    )

    object Recorder : FirElementsRecorder() {
        override fun visitScript(script: FirScript, data: MutableMap<KtElement, FirElement>) {
            cacheElement(script, data)
            visitScriptDependentElements(script, this, data)
        }
    }
}

internal fun <T, R> visitScriptDependentElements(script: FirScript, visitor: FirVisitor<T, R>, data: R) {
    script.annotations.forEach { it.accept(visitor, data) }
    script.contextReceivers.forEach { it.accept(visitor, data) }
    script.statements.forEach {
        if (it.isScriptStatement) {
            it.accept(visitor, data)
        }
    }
}

internal class ClassDeclarationStructureElement(
    file: FirFile,
    clazz: FirRegularClass,
    moduleComponents: LLFirModuleResolveComponents,
) : DeclarationBaseStructureElement<FirRegularClass>(clazz) {
    override val diagnostics = FileStructureElementDiagnostics(
        file,
        ClassDiagnosticRetriever(declaration),
        moduleComponents,
    )

    class Recorder(private val firClass: FirRegularClass) : FirElementsRecorder() {
        override fun visitProperty(property: FirProperty, data: MutableMap<KtElement, FirElement>) {
            if (property.source?.kind == KtFakeSourceElementKind.PropertyFromParameter) {
                super.visitProperty(property, data)
            }
        }

        override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: MutableMap<KtElement, FirElement>) {
        }

        override fun visitConstructor(constructor: FirConstructor, data: MutableMap<KtElement, FirElement>) {
            if (
                (constructor is FirPrimaryConstructor || constructor is FirErrorPrimaryConstructor) &&
                constructor.source?.kind == KtFakeSourceElementKind.ImplicitConstructor
            ) {
                DeclarationStructureElement.Recorder.visitConstructor(constructor, data)
            }
        }

        override fun visitErrorPrimaryConstructor(errorPrimaryConstructor: FirErrorPrimaryConstructor, data: MutableMap<KtElement, FirElement>) =
            visitConstructor(errorPrimaryConstructor, data)

        override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: MutableMap<KtElement, FirElement>) {
        }

        override fun visitRegularClass(regularClass: FirRegularClass, data: MutableMap<KtElement, FirElement>) {
            if (regularClass != firClass) return
            super.visitRegularClass(regularClass, data)
        }

        override fun visitTypeAlias(typeAlias: FirTypeAlias, data: MutableMap<KtElement, FirElement>) {
        }
    }
}

internal class DeclarationStructureElement(
    file: FirFile,
    declaration: FirDeclaration,
    moduleComponents: LLFirModuleResolveComponents,
) : DeclarationBaseStructureElement<FirDeclaration>(declaration) {
    override val diagnostics = FileStructureElementDiagnostics(
        file,
        SingleNonLocalDeclarationDiagnosticRetriever(declaration),
        moduleComponents,
    )

    object Recorder : FirElementsRecorder() {
        override fun visitConstructor(constructor: FirConstructor, data: MutableMap<KtElement, FirElement>) {
            if (constructor is FirPrimaryConstructor) {
                constructor.valueParameters.forEach { parameter ->
                    parameter.correspondingProperty?.let { property ->
                        visitProperty(property, data)
                    }
                }
            }

            super.visitConstructor(constructor, data)
        }
    }
}

internal class RootStructureElement(
    val file: FirFile,
    moduleComponents: LLFirModuleResolveComponents,
) : FileStructureElement() {
    override val mappings = KtToFirMapping(file)
    override val diagnostics = FileStructureElementDiagnostics(file, FileDiagnosticRetriever, moduleComponents)

    object Recorder : FirElementsRecorder() {
        override fun visitElement(element: FirElement, data: MutableMap<KtElement, FirElement>) {
            if (element !is FirDeclaration || element is FirFile) {
                super.visitElement(element, data)
            }
        }
    }
}
