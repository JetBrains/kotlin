/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLPartialBodyResolveRequest
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.LLPartialBodyResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets.partialBodyResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.LLFirLockProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.LLFirResolveDesignationCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirResolvableSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.body
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isPartialBodyResolvable
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirLazyBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry
import kotlin.collections.indexOf

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
    elementProvider: FirElementProvider = EagerFirElementProvider(declaration)
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

private typealias FirElementProvider = (KtElement) -> FirElement?

private class EagerFirElementProvider(firDeclaration: FirDeclaration) : FirElementProvider {
    private val mapping = FirElementsRecorder.recordElementsFrom(
        firElement = firDeclaration,
        recorder = FileStructureElement.recorderFor(firDeclaration),
    )

    override fun invoke(element: KtElement): FirElement? {
        return mapping[element]
    }
}

private class PartialBodyFirElementProvider(
    private val declaration: FirDeclaration,
    private val psiDeclaration: KtDeclaration,
    private val psiBlock: KtBlockExpression,
    private val psiStatements: List<KtExpression>,
    private val session: LLFirResolvableModuleSession
) : FirElementProvider {
    /**
     * Contains the latest known partial body resolution state.
     *
     * Initially, the [lastState] is empty, even though the declaration itself may already be partially resolved.
     * On querying the mapping (by calling [invoke]), the actual resolved state is synchronized with the [lastState],
     * and all missing elements are added to [bodyMappings].
     */
    @Volatile
    private var lastState = LLPartialBodyResolveState(
        totalPsiStatementCount = psiStatements.size,
        analyzedPsiStatementCount = 0,
        analyzedFirStatementCount = 0,
        performedAnalysesCount = 0,
        analysisStateSnapshot = null
    )

    /**
     * Contains mappings for non-body elements.
     */
    private val signatureMappings: Map<KtElement, FirElement>

    /**
     * Contains collected mappings.
     * Initially, only signature mappings are registered (the body is ignored).
     * After consequent partial body analysis, elements from analyzed statements are appended.
     */
    @Volatile
    private var bodyMappings: PersistentMap<KtElement, FirElement> = persistentMapOf()

    // The body block cannot be cached on the element provider construction, as the body might be lazy at that point
    private val bodyBlock: FirBlock
        get() = declaration.body ?: error("Partial body element provider supports only declarations with bodies")

    private val lockProvider: LLFirLockProvider
        get() = session.moduleComponents.globalResolveComponents.lockProvider

    init {
        signatureMappings = persistentHashMapOf<KtElement, FirElement>()
            .builder()
            .also { declaration.accept(DeclarationStructureElement.SignatureRecorder(bodyBlock), it) }
            .build()
    }

    override fun invoke(psiElement: KtElement): FirElement? {
        val psiStatement = findTopmostStatement(psiElement)

        if (psiStatement == null) {
            // Didn't find a containing topmost statement. It's either a signature element or some unrelated one
            return signatureMappings[psiElement]
        }

        val psiStatementIndex = psiStatements.indexOf(psiStatement)

        checkWithAttachment(psiStatementIndex >= 0, { "The topmost statement was not found" }) {
            withPsiEntry("statement", psiStatement)
            withPsiEntry("declaration", psiDeclaration)
        }

        synchronized(this) {
            if (lastState.analyzedPsiStatementCount > psiStatementIndex) {
                // The statement is already analyzed and its children are in collected
                return bodyMappings[psiElement]
            }
        }

        performBodyAnalysis(psiStatementIndex)

        synchronized(this) {
            val newState = fetchPartialBodyResolveState()
            if (newState != null) {
                val lastCount = lastState.analyzedFirStatementCount
                val newCount = newState.analyzedFirStatementCount

                // There are newly analyzed statements, let's collect them
                if (lastCount < newCount) {
                    val firBody = bodyBlock
                    require(firBody !is FirLazyBlock)

                    val newBodyMappingsBuilder = bodyMappings.builder()

                    for (index in lastCount until newCount) {
                        val firStatement = firBody.statements[index]
                        firStatement.accept(DeclarationStructureElement.Recorder, newBodyMappingsBuilder)
                    }

                    bodyMappings = newBodyMappingsBuilder.build()
                    lastState = newState
                }
            } else {
                // The body has never been analyzed (otherwise the partial body resolve state should have been present)
                bodyMappings = bodyMappings
                    .builder()
                    .also { bodyBlock.accept(FileStructureElement.recorderFor(declaration), it) }
                    .build()
            }
        }

        return bodyMappings[psiElement]
    }

    private fun findTopmostStatement(psiElement: KtElement): KtElement? {
        var previous: PsiElement? = null

        for (current in psiElement.parentsWithSelf) {
            when (current) {
                psiBlock -> return previous as? KtElement
                psiDeclaration -> return null
            }

            previous = current
        }

        return null
    }

    private fun fetchPartialBodyResolveState(): LLPartialBodyResolveState? {
        var result: LLPartialBodyResolveState? = null
        var isRun = false
        lockProvider.withReadLock(declaration, FirResolvePhase.BODY_RESOLVE) {
            result = declaration.partialBodyResolveState
            isRun = true // 'withReadLock' does not call the lambda if the declaration already resolved
        }
        return if (isRun) result else declaration.partialBodyResolveState
    }

    private fun performBodyAnalysis(psiStatementIndex: Int) {
        val psiStatementLimit = psiStatementIndex + 1
        if (psiStatementLimit < psiStatements.size) {
            val request = LLPartialBodyResolveRequest(
                target = declaration,
                totalPsiStatementCount = psiStatements.size,
                targetPsiStatementCount = psiStatementLimit,
                stopElement = psiStatements[psiStatementLimit]
            )

            val target = LLFirResolveDesignationCollector.getDesignationToResolveForPartialBody(request)
            if (target != null) {
                session.moduleComponents.firModuleLazyDeclarationResolver.lazyResolveTarget(target, FirResolvePhase.BODY_RESOLVE)
                return
            }
        }

        declaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
    }
}

internal class KtToFirMapping(private val elementProvider: FirElementProvider) {
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

        private fun createFirElementProvider(declaration: FirDeclaration): FirElementProvider {
            if (IS_PARTIAL_RESOLVE_ENABLED) {
                val bodyBlock = declaration.body
                if (declaration.isPartialBodyResolvable && bodyBlock != null && declaration.resolvePhase < FirResolvePhase.BODY_RESOLVE) {
                    require(declaration.resolvePhase == FirResolvePhase.BODY_RESOLVE.previous)

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
                        return PartialBodyFirElementProvider(declaration, psiDeclaration, psiBodyBlock, psiStatements, session)
                    }
                }
            }

            declaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            return EagerFirElementProvider(declaration)
        }

        private val KtDeclaration.bodyBlock: KtBlockExpression?
            get() = when (this) {
                is KtAnonymousInitializer -> body as? KtBlockExpression
                is KtDeclarationWithBody -> bodyBlockExpression
                else -> null
            }
    }

    object Recorder : AbstractRecorder()

    class SignatureRecorder(val bodyElement: FirElement) : AbstractRecorder() {
        override fun visitElement(element: FirElement, data: MutableMap<KtElement, FirElement>) {
            if (element === bodyElement) {
                return
            }
            super.visitElement(element, data)
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
