/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.findStringPlusSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isPartialAnalyzable
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.isPartialBodyResolvable
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.correspondingProperty
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyExpressionBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.toKtPsiSourceElement
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions

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
    elementMapper: LLElementMapper = LLEagerElementMapper(declaration)
) {
    val mappings: KtToFirMapping = KtToFirMapping(elementMapper)

    companion object {
        fun recorderFor(fir: FirDeclaration): FirElementsRecorder = when (fir) {
            is FirFile -> RootStructureElement.Recorder(fir)
            is FirScript -> RootScriptStructureElement.Recorder(fir)
            is FirRegularClass -> ClassDeclarationStructureElement.Recorder(fir)
            else -> DeclarationStructureElement.Recorder
        }
    }
}

internal class KtToFirMapping(private val elementMapper: LLElementMapper) {
    fun get(element: KtElement): FirElement? {
        return elementMapper(element)
    }

    companion object {
        private fun checkStringLiteralFolderExpression(
            element: KtElement,
            session: FirSession,
            mapping: Map<KtElement, FirElement>,
        ): FirElement? {
            var current: PsiElement? = element
            var fir: FirElement? = null
            while (fir == null && (current is KtBinaryExpression || current is KtOperationReferenceExpression)) {
                fir = mapping[current]
                if (fir is FirStringConcatenationCall && fir.isFoldedStrings) {
                    // In case of folded string literals, we have to return plus operator reference for operation reference.
                    return if (element is KtOperationReferenceExpression)
                        findStringPlusSymbol(session)?.let {
                            buildResolvedNamedReference {
                                source = element.toKtPsiSourceElement()
                                name = OperatorNameConventions.PLUS
                                resolvedSymbol = it
                            }
                        }
                    else
                        fir
                }

                if (fir != null) {
                    return null
                }

                current = current.parent
            }

            return null
        }

        /**
         * If [element] is a reference with the name "suspend", returns a fake [FirResolvedNamedReference] to `kotlin.suspend`.
         */
        private fun fakeReferenceToBuiltInSuspendOrNull(
            element: KtElement,
            session: FirSession,
        ): FirResolvedNamedReference? {
            if (element !is KtNameReferenceExpression) return null
            if (element.getReferencedName() != StandardClassIds.Callables.suspend.callableName.identifier) return null

            return session.symbolProvider
                .getTopLevelCallableSymbols(
                    packageFqName = StandardClassIds.Callables.suspend.packageName,
                    name = StandardClassIds.Callables.suspend.callableName
                )
                .singleOrNull()
                ?.let {
                    buildResolvedNamedReference {
                        source = element.toKtPsiSourceElement()
                        name = StandardClassIds.Callables.suspend.callableName
                        resolvedSymbol = it
                    }
                }
        }

        private fun fakeCallToBuiltInSuspendOrNull(
            call: KtCallExpression,
            mapping: Map<KtElement, FirElement>,
            session: FirSession,
        ): FirFunctionCall? {
            val calleeExpression = call.calleeExpression ?: return null
            val lambdaArgument = call.lambdaArguments.firstOrNull()?.getLambdaExpression() ?: return null
            val argument = mapping[lambdaArgument] as? FirAnonymousFunctionExpression ?: return null
            val reference = fakeReferenceToBuiltInSuspendOrNull(calleeExpression, session) ?: return null
            val symbol = reference.resolvedSymbol as? FirFunctionSymbol ?: return null
            val valueParameter = symbol.valueParameterSymbols.singleOrNull() ?: return null
            return buildFunctionCall {
                calleeReference = reference
                source = call.toKtPsiSourceElement()
                argumentList = buildResolvedArgumentList(
                    original = null,
                    mapping = LinkedHashMap<FirExpression, FirValueParameter>().apply {
                        put(argument, valueParameter.fir)
                    }
                )
                typeArguments += buildTypeProjectionWithVariance {
                    typeRef = buildResolvedTypeRef {
                        coneType = argument.anonymousFunction.returnTypeRef.coneType
                    }
                    variance = Variance.INVARIANT
                }
                coneTypeOrNull = argument.resolvedType
            }
        }

        fun getFir(element: KtElement, session: FirSession, mapping: Map<KtElement, FirElement>): FirElement? {
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
                if (current is KtElement) mapping[current]?.let { return it }
                if (current is KtCallExpression) fakeCallToBuiltInSuspendOrNull(current, mapping, session)?.let {
                    return it
                }
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
                    -> mapping[current as KtElement]
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
                    ) {
                        mapping[parent]
                    } else {
                        mapping[current] ?: fakeReferenceToBuiltInSuspendOrNull(element, session)
                    }
                }
                is KtParenthesizedExpression -> checkStringLiteralFolderExpression(element, session, mapping)
                is KtBinaryExpression -> checkStringLiteralFolderExpression(element, session, mapping)
                // Here there is no separate FIR node for partial operator calls (like for a[i] = 1, there is no separate node for a[i])
                    ?: if (element is KtArrayAccessExpression || element is KtOperationReferenceExpression) mapping[current] else null
                is KtBlockExpression ->
                    // For script initializers, we need to return FIR element for script itself
                    if (element is KtScriptInitializer) mapping[current.parent as KtScript] else null
                is PsiErrorElement -> {
                    val parent = current.parent
                    if (parent is KtDestructuringDeclaration) mapping[parent] else null
                }
                // Value argument names and corresponding references are not part of the FIR tree
                is KtValueArgumentName -> mapping[current.parent as KtValueArgument]
                is KtContainerNode -> {
                    val parent = current.parent
                    // Labels in labeled expression (i.e., return@foo) have no FIR node
                    if (parent is KtExpressionWithLabel) mapping[parent] else null
                }
                // Enum entries/annotation entries constructor calls
                is KtConstructorCalleeExpression -> mapping[current.parent as KtCallElement]
                // KtParameter for destructuring declaration
                is KtParameter -> mapping[current as KtElement]
                else -> null
            }
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
    class Recorder(script: FirScript) : FirElementContainerRecorder(
        container = script,
        declarationsToIgnore = script.declarationsToIgnore,
    )
}

/** @see RootScriptStructureElement */
internal val FirScript.declarationsToIgnore: Set<FirDeclaration>
    get() = parameters.plus(declarations).toSet()

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
    class Recorder(firClass: FirRegularClass) : FirElementContainerRecorder(
        container = firClass,
        declarationsToIgnore = firClass.declarationsToIgnore,
    )
}

/** @see ClassDeclarationStructureElement */
internal val FirRegularClass.declarationsToIgnore: Set<FirDeclaration>
    get() = declarations.filterNot(FirDeclaration::isPartOfClassStructureElement).toSet()

/**
 * The recorder is supposed to visit only elements that belong to the [container].
 *
 * For instance, it should visit annotations, but not regular declarations.
 */
internal abstract class FirElementContainerRecorder(
    private val container: FirDeclaration,
    private val declarationsToIgnore: Set<FirDeclaration>,
) : FirElementsRecorder() {
    override fun visitElement(element: FirElement, data: MutableMap<KtElement, FirElement>) {
        // Entry point to the visitor
        if (element === container) {
            return super.visitElement(element, data)
        }

        val recordElement = if (element is FirDeclaration) {
            element !in declarationsToIgnore
        } else {
            true
        }

        if (recordElement) {
            // A separate recorder is called here as we don't have to check
            // conditions for nested elements – they should be recorded deeply
            element.accept(DeclarationStructureElement.Recorder, data)
        }
    }
}

/**
 * Whether a class member declaration is a part of the [ClassDeclarationStructureElement].
 *
 * [FirRegularClass] stands as an anchor for synthetic declarations which it produces (like an implicit constructor).
 * This is necessary to process diagnostics from such elements as they don't have real sources
 * (and a dedicated [FileStructureElement] as a consequence).
 *
 * @see ClassDeclarationStructureElement
 * @see ClassDiagnosticRetriever
 */
internal val FirDeclaration.isPartOfClassStructureElement: Boolean
    get() = when (source?.kind) {
        KtFakeSourceElementKind.ImplicitConstructor,
        KtFakeSourceElementKind.DataClassGeneratedMembers,
        KtFakeSourceElementKind.EnumGeneratedDeclaration,
        KtFakeSourceElementKind.ClassDelegationField,
            -> true

        else -> false
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
    elementMapper = createMapper(declaration),
) {
    private companion object {
        private val IS_PARTIAL_RESOLVE_ENABLED by lazy(LazyThreadSafetyMode.PUBLICATION) {
            Registry.`is`("kotlin.analysis.partialBodyAnalysis", true)
        }

        private fun createMapper(declaration: FirDeclaration): LLElementMapper {
            val partialBodyMapper = createPartialBodyMapperIfApplicable(declaration)
            if (partialBodyMapper != null) {
                return partialBodyMapper
            }

            declaration.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
            return LLEagerElementMapper(declaration)
        }

        private fun createPartialBodyMapperIfApplicable(declaration: FirDeclaration): LLElementMapper? {
            if (!IS_PARTIAL_RESOLVE_ENABLED) {
                return null
            }

            val bodyBlock = declaration.body
            if (!declaration.isPartialBodyResolvable || bodyBlock == null || declaration.resolvePhase >= FirResolvePhase.BODY_RESOLVE) {
                return null
            }

            require(declaration.resolvePhase >= FirResolvePhase.BODY_RESOLVE.previous)

            val isPartiallyResolvable = when (bodyBlock) {
                is FirSingleExpressionBlock -> false
                is FirEmptyExpressionBlock -> false
                is FirLazyBlock -> true // Optimistic (however, below we also check the PSI statement count)
                else -> bodyBlock.isPartialAnalyzable
            }

            if (!isPartiallyResolvable) {
                return null
            }

            val session = declaration.llFirResolvableSession ?: return null
            val psiDeclaration = declaration.realPsi as? KtDeclaration
            val psiBodyBlock = psiDeclaration?.bodyBlock
            val psiStatements = psiBodyBlock?.statements?.takeIf { it.size > 1 } ?: return null

            // Although we don't require the body to be resolved here, its changes must invalidate the element mapper.
            // Note that there might be changes in a number of statements, so here we keep the guarantee – a partial element mapper
            // is only created if there are more than one body statement.
            LLFirDeclarationModificationService.bodyResolved(declaration, phase = FirResolvePhase.BODY_RESOLVE)

            return LLPartialBodyElementMapper(declaration, psiDeclaration, psiBodyBlock, psiStatements, session)
        }
    }

    object Recorder : AbstractRecorder()

    /**
     * A recorder that skips content analyzed on the [FirResolvePhase.BODY_RESOLVE] phase.
     *
     * Sic! The recorder currently is only intended to be used for computing signature mappings in [LLPartialBodyElementMapper]
     * for [isPartialBodyResolvable] declarations.
     * For other usages, the behavior is unspecified.
     */
    class SignatureRecorder(private val declaration: FirDeclaration) : AbstractRecorder() {
        private var parent: FirElement? = null

        // Sic! The declaration might be resolved to 'BODY_RESOLVE' in some other thread while we traverse over it.
        override fun visitElement(element: FirElement, data: MutableMap<KtElement, FirElement>) {
            // Skip elements only directly nested in the declaration.
            // Note that annotation values technically can contain arbitrary code that we don't want to filter out here.
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
            file = file,
            moduleComponents = moduleComponents,
        )
    ),
) {
    class Recorder(file: FirFile) : FirElementContainerRecorder(
        container = file,
        declarationsToIgnore = file.declarationsToIgnore,
    )
}

/** @see RootStructureElement */
internal val FirFile.declarationsToIgnore: Set<FirDeclaration>
    get() = declarations.toSet()
