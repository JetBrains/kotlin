/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.body
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isPartialBodyResolvable
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLazyBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.psi.*

/**
 * Collects [KT -> FIR][KtToFirMapping] mapping and [diagnostics][FileStructureElementDiagnostics] for [declaration].
 *
 * @param declaration is a fully resolved declaration (not necessary in [FirResolvePhase.BODY_RESOLVE] phase)
 *
 * @see FileStructure
 * @see org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.fir.LLFirStructureElementDiagnosticsCollector
 */
internal sealed class FileStructureElement(
    val declaration: FirDeclaration,
    val diagnostics: FileStructureElementDiagnostics,
    elementProvider: DeclarationFirElementProvider = EagerDeclarationFirElementProvider(declaration)
) {
    val mappings: KtToFirMapping = KtToFirMapping(elementProvider)

    companion object {
        fun recorderFor(fir: FirDeclaration): FirElementsRecorder = when (fir) {
            is FirFile -> RootStructureElement.Recorder
            is FirScript -> RootScriptStructureElement.Recorder
            is FirRegularClass -> ClassDeclarationStructureElement.Recorder(fir)
            else -> DeclarationStructureElement.Recorder
        }
    }
}

internal class KtToFirMapping(private val elementProvider: DeclarationFirElementProvider) {
    private fun getElement(ktElement: KtElement): FirElement? {
        return elementProvider(ktElement)
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
            // We are still referring to the same element with possible type parameter/name qualification/nullability,
            // hence it is always correct to return a corresponding element if present
            if (current is KtElement) getElement(current)?.let { return it }
            current = current.parent
        }

        // Here current is the lowest ancestor that has different corresponding text
        return when (current) {
            // Constants with unary operation (i.e., +1 or -1) are saved as a leaf element of FIR tree
            is KtPrefixExpression,
                // There is no separate element for annotation construction call
            is KtAnnotationEntry,
                // We replace a source for selector with the whole expression
            is KtSafeQualifiedExpression,
                // Top level destructuring declarations do not have FIR for r-value at the moment, would probably be changed later
            is KtDestructuringDeclaration,
                // There is no separate FIR node for this in this@foo expressions, same for super@Foo
            is KtThisExpression,
            is KtSuperExpression,
                // Part of the path in import/package directives has no FIR node
            is KtImportDirective,
            is KtPackageDirective,
                // Super type refs are not recorded
            is KtSuperTypeCallEntry,
                // this/super in delegation calls are not part of FIR tree, this(args) is
            is KtConstructorDelegationCall,
                // In case of type projection we are not recording the corresponding type reference
            is KtTypeProjection,
                -> getElement(current as KtElement)
            is KtCallExpression -> {
                // Case 1:
                // If we have, say, A(), reference A is not recorded, while call A() is recorded.
                //
                // Case 2:
                // A<Ty> and B<Ty> in `A<Ty>.B<Ty>` are both calls, but neither A nor B nor B<Ty> are recorded.
                // Only A<Ty> and the whole qualified expression (as FirResolvedQualifier) are recorded.
                val parent = current.parent
                if (current.valueArgumentList == null &&
                    parent is KtQualifiedExpression &&
                    parent.selectorExpression == current
                ) getElement(parent)
                else getElement(current)
            }
            is KtBinaryExpression ->
                // Here there is no separate FIR node for partial operator calls (like for a[i] = 1, there is no separate node for a[i])
                if (element is KtArrayAccessExpression || element is KtOperationReferenceExpression) getElement(current) else null
            is KtBlockExpression ->
                // For script initializers, we need to return FIR element for script itself
                if (element is KtScriptInitializer) getElement(current.parent as KtScript) else null
            is PsiErrorElement -> {
                val parent = current.parent
                if (parent is KtDestructuringDeclaration) getElement(parent) else null
            }
            // Value argument names and corresponding references are not part of the FIR tree
            is KtValueArgumentName -> getElement(current.parent as KtValueArgument)
            is KtContainerNode -> {
                val parent = current.parent
                // Labels in labeled expression (i.e., return@foo) have no FIR node
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

internal class RootScriptStructureElement(
    file: FirFile,
    script: FirScript,
    moduleComponents: LLFirModuleResolveComponents,
) : FileStructureElement(
    declaration = script,
    diagnostics = FileStructureElementDiagnostics(
        ScriptDiagnosticRetriever(
            declaration = script,
            file = file,
            moduleComponents = moduleComponents,
        )
    ),
) {
    object Recorder : FirElementsRecorder() {
        override fun visitScript(script: FirScript, data: MutableMap<KtElement, FirElement>) {
            cacheElement(script, data)
            visitScriptDependentElements(script, this, data)
        }
    }
}

internal fun <T, R> visitScriptDependentElements(script: FirScript, visitor: FirVisitor<T, R>, data: R) {
    script.annotations.forEach { it.accept(visitor, data) }
    script.receivers.forEach { it.accept(visitor, data) }
}

internal class ClassDeclarationStructureElement(
    file: FirFile,
    clazz: FirRegularClass,
    moduleComponents: LLFirModuleResolveComponents,
) : FileStructureElement(
    declaration = clazz,
    diagnostics = FileStructureElementDiagnostics(
        ClassDiagnosticRetriever(
            declaration = clazz,
            file = file,
            moduleComponents = moduleComponents,
        )
    ),
) {
    class Recorder(private val firClass: FirRegularClass) : FirElementsRecorder() {
        override fun visitProperty(property: FirProperty, data: MutableMap<KtElement, FirElement>) {
        }

        override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: MutableMap<KtElement, FirElement>) {
        }

        override fun visitConstructor(constructor: FirConstructor, data: MutableMap<KtElement, FirElement>) {
            if (constructor.isImplicitConstructor) {
                DeclarationStructureElement.Recorder.visitConstructor(constructor, data)
            }
        }

        override fun visitField(field: FirField, data: MutableMap<KtElement, FirElement>) {
            if (field.source?.kind == KtFakeSourceElementKind.ClassDelegationField) {
                DeclarationStructureElement.Recorder.visitField(field, data)
            }
        }

        override fun visitErrorPrimaryConstructor(
            errorPrimaryConstructor: FirErrorPrimaryConstructor,
            data: MutableMap<KtElement, FirElement>,
        ) = visitConstructor(errorPrimaryConstructor, data)

        override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: MutableMap<KtElement, FirElement>) {
        }

        override fun visitRegularClass(regularClass: FirRegularClass, data: MutableMap<KtElement, FirElement>) {
            if (regularClass === firClass) {
                super.visitRegularClass(regularClass, data)
            }
        }

        override fun visitTypeAlias(typeAlias: FirTypeAlias, data: MutableMap<KtElement, FirElement>) {
        }
    }
}

internal class DeclarationStructureElement(
    file: FirFile,
    declaration: FirDeclaration,
    moduleComponents: LLFirModuleResolveComponents,
) : FileStructureElement(
    declaration = declaration,
    diagnostics = FileStructureElementDiagnostics(
        SingleNonLocalDeclarationDiagnosticRetriever(
            declaration = declaration,
            file = file,
            moduleComponents = moduleComponents,
        )
    ),
    elementProvider = createFirElementProvider(declaration),
) {
    private companion object {
        private val IS_PARTIAL_RESOLVE_ENABLED = Registry.`is`("kotlin.analysis.partialBodyAnalysis", true)

        private fun createFirElementProvider(declaration: FirDeclaration): DeclarationFirElementProvider {
            if (IS_PARTIAL_RESOLVE_ENABLED) {
                val bodyBlock = declaration.body
                if (declaration.isPartialBodyResolvable && bodyBlock != null && declaration.resolvePhase < FirResolvePhase.BODY_RESOLVE) {
                    require(declaration.resolvePhase >= FirResolvePhase.BODY_RESOLVE.previous)

                    val isPartiallyResolvable = when (bodyBlock) {
                        is FirSingleExpressionBlock -> false
                        is FirEmptyExpressionBlock -> false
                        is FirLazyBlock -> true // Optimistic (however, below we also check the PSI statement count)
                        else -> bodyBlock.statements.size > 1
                    }

                    val session = declaration.llFirResolvableSession
                    val psiDeclaration = declaration.realPsi as? KtDeclaration
                    val psiBodyBlock = psiDeclaration?.bodyBlock
                    val psiStatements = psiBodyBlock?.statements

                    if (isPartiallyResolvable && session != null && psiStatements != null && psiStatements.size > 1) {
                        return PartialBodyDeclarationFirElementProvider(declaration, psiDeclaration, psiBodyBlock, psiStatements, session)
                    }
                }
            }

            declaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            return EagerDeclarationFirElementProvider(declaration)
        }

        private val KtDeclaration.bodyBlock: KtBlockExpression?
            get() = when (this) {
                is KtAnonymousInitializer -> body as? KtBlockExpression
                is KtDeclarationWithBody -> bodyBlockExpression
                else -> null
            }
    }

    object Recorder : AbstractRecorder()

    /**
     * A recorder that skips content analyzed on the [FirResolvePhase.BODY_RESOLVE] phase.
     */
    class SignatureRecorder(private val declaration: FirDeclaration) : AbstractRecorder() {
        private var parent: FirElement? = null

        // Sic! The declaration might be resolved to 'BODY_RESOLVE' in some other thread while we traverse over it.
        override fun visitElement(element: FirElement, data: MutableMap<KtElement, FirElement>) {
            val currentParent = parent

            if (element is FirBlock && currentParent == declaration) {
                // Skip declaration body
                return
            }

            if (element is FirExpression && currentParent is FirValueParameter && currentParent.defaultValue == element) {
                // Skip default value parameters
                return
            }

            if (element is FirDelegatedConstructorCall && currentParent is FirConstructor && currentParent == declaration) {
                // Skip delegated constructors
                return
            }

            cacheElement(element, data)

            try {
                parent = element
                element.acceptChildren(this, data)
            } finally {
                parent = currentParent
            }
        }
    }

    class BodyBlockRecorder(block: FirBlock) : AbstractRecorder() {
        private val statements = block.statements.toSet()

        override fun visitElement(element: FirElement, data: MutableMap<KtElement, FirElement>) {
            // Statements are already registered
            if (element !in statements) {
                super.visitElement(element, data)
            }
        }
    }

    abstract class AbstractRecorder : FirElementsRecorder() {
        override fun visitConstructor(constructor: FirConstructor, data: MutableMap<KtElement, FirElement>) {
            super.visitConstructor(constructor, data)

            if (constructor is FirPrimaryConstructor) {
                constructor.valueParameters.forEach { parameter ->
                    parameter.correspondingProperty?.let { property ->
                        visitProperty(property, data)
                    }
                }
            }
        }
    }
}

internal class RootStructureElement(
    file: FirFile,
    moduleComponents: LLFirModuleResolveComponents,
) : FileStructureElement(
    declaration = file,
    diagnostics = FileStructureElementDiagnostics(
        FileDiagnosticRetriever(
            declaration = file,
            file = file,
            moduleComponents = moduleComponents,
        )
    ),
) {
    object Recorder : FirElementsRecorder() {
        override fun visitElement(element: FirElement, data: MutableMap<KtElement, FirElement>) {
            if (element !is FirDeclaration || element is FirFile) {
                super.visitElement(element, data)
            }
        }
    }
}
