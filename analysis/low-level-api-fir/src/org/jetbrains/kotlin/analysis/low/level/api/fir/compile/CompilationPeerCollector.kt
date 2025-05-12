/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compile

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinActualDeclarationProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirResolveSessionService
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLPlatformActualizer
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.containingKtFileIfAny
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasBody
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.impl.FirContractCallBlock
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

/**
 * Processes the declaration, collecting files that would need to be submitted to the backend (or handled specially)
 * in case if the declaration is compiled.
 *
 * Besides the file that owns the declaration, the visitor also recursively collects source files with called inline functions.
 * In addition, the visitor collects a list of inlined local classes. Such a list might be useful in [GenerationState.GenerateClassFilter]
 * to filter out class files unrelated to the current compilation.
 *
 * Note that compiled declarations are not analyzed, as the backend can inline them natively.
 */
@KaImplementationDetail
class CompilationPeerCollector private constructor(private val actualizer: LLPlatformActualizer?) {
    companion object {
        fun process(file: FirFile, actualizer: LLPlatformActualizer?): CompilationPeerData {
            val collector = CompilationPeerCollector(actualizer)
            collector.process(file)

            return CompilationPeerData(
                peers = collector.peers,
                inlinedClasses = collector.inlinedClasses
            )
        }
    }

    private val peers = LinkedHashMap<KaModule, MutableList<KtFile>>()
    private val inlinedClasses = LinkedHashSet<KtClassOrObject>()

    private val visited = HashSet<FirFile>()
    private val moduleStack = ArrayDeque<KaModule>()

    private fun process(file: FirFile) {
        ProgressManager.checkCanceled()

        val ktFile = file.containingKtFileIfAny
        if (ktFile == null || ktFile.isCompiled) {
            return
        }

        val module = file.llFirModuleData.ktModule
        if (module in moduleStack && module != moduleStack.last()) {
            // We cannot compile two or more modules together
            errorWithAttachment("Cyclic dependency between modules") {
                withEntry("cycle") {
                    (moduleStack + module).forEach { this@withEntry.println(it) }
                }
            }

            // Skip non-inlined indirect recursion
            return
        }

        if (!visited.add(file)) {
            // Skip the declaration we visited before
            // Sic: this happens after the inline recursion check
            return
        }

        // Avoid deep stacks by gathering callee files first
        val visitor = CompilationPeerCollectingVisitor(ktFile.project, actualizer)
        file.accept(visitor)

        inlinedClasses.addAll(visitor.inlinedClasses)

        withModule(module) {
            visitor.files.forEach(::process)
        }

        peers.getOrPut(module, ::ArrayList).add(ktFile)
    }

    private inline fun withModule(module: KaModule, block: () -> Unit) {
        if (moduleStack.lastOrNull() == module) {
            block()
            return
        }

        moduleStack.addLast(module)
        try {
            block()
        } finally {
            moduleStack.removeLast()
        }
    }
}

@KaImplementationDetail
class CompilationPeerData(
    /**
     * The original file and all files that contain inline functions/properties called from that file or any other files from the list.
     * The returned list is in post-order.
     * Files in the list are unique.
     *
     * For example,
     *  - A is a source file in module M(A) to be compiled; it calls an inline function from the file B of module M(B).
     *  - B calls another inline function defined in C in module M(C).
     *  - [peers] returned by [CompilationPeerCollector.process] then will be {C, B, A}.
     *
     * More formally, i-th element of [peers] will not have inline-dependency on any j-th element of
     * [peers], where j > i.
     *
     * This list does not contain duplicated files.
     */
    val peers: Map<KaModule, List<KtFile>>,

    /** Local classes inlined as a part of inline functions. */
    val inlinedClasses: Set<KtClassOrObject>
)

private class CompilationPeerCollectingVisitor(
    val project: Project,
    val actualizer: LLPlatformActualizer?,
) : FirDefaultVisitorVoid() {
    private val collectedFunctions = HashSet<FirFunction>()
    private val collectedFiles = LinkedHashSet<FirFile>()
    private val collectedInlinedClasses = LinkedHashSet<KtClassOrObject>()

    private var isInlineFunctionContext: Boolean = false

    val files: Set<FirFile>
        get() = collectedFiles

    val inlinedClasses: Set<KtClassOrObject>
        get() = collectedInlinedClasses

    override fun visitElement(element: FirElement) {
        if (element is FirResolvable) {
            processResolvable(element)
        }

        element.acceptChildren(this)
    }

    override fun visitBlock(block: FirBlock) {
        if (block !is FirContractCallBlock) {
            super.visitBlock(block)
        }
    }

    override fun visitContractDescription(contractDescription: FirContractDescription) {
        // Skip contract description.
        // Contract blocks are skipped in BE, so we would never need to inline contract DSL calls.
    }

    override fun visitConstructor(constructor: FirConstructor) {
        constructor.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

        super.visitConstructor(constructor)
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
        simpleFunction.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

        withInlineFunctionContext(simpleFunction) {
            super.visitFunction(simpleFunction)
        }
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor) {
        withInlineFunctionContext(propertyAccessor) {
            super.visitPropertyAccessor(propertyAccessor)
        }
    }

    override fun visitProperty(property: FirProperty) {
        property.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)

        super.visitProperty(property)
    }

    override fun visitClass(klass: FirClass) {
        super.visitClass(klass)

        if (isInlineFunctionContext) {
            collectedInlinedClasses.addIfNotNull(klass.psi as? KtClassOrObject)
        }
    }

    @OptIn(SymbolInternals::class)
    private fun processResolvable(element: FirResolvable) {
        val reference = element.calleeReference
        if (reference !is FirResolvedNamedReference) {
            return
        }

        val symbol = reference.resolvedSymbol
        if (symbol is FirCallableSymbol<*>) {
            when (val fir = symbol.fir) {
                is FirFunction -> {
                    register(fir)
                }
                is FirProperty -> {
                    fir.getter?.let(::register)
                    fir.setter?.let(::register)
                }
                else -> {}
            }
        }
    }

    private val actualDeclarationProvider by lazy { KotlinActualDeclarationProvider.getInstance(project) }
    private val projectStructureProvider by lazy { KotlinProjectStructureProvider.getInstance(project) }
    private val resolveSessionService by lazy { LLFirResolveSessionService.getInstance(project) }

    /**
     * Register a containing source file for an inline function.
     */
    private fun register(callee: FirFunction) {
        val originalFunction = callee.unwrapSubstitutionOverrides()
        if (originalFunction.isInline) {
            if (originalFunction.isExpect) {
                registerActualCounterpart(callee)
            } else if (originalFunction.hasBody) {
                if (collectedFunctions.add(originalFunction)) {
                    val calleeFile = callee.getContainingFile()
                    if (calleeFile != null && calleeFile.origin == FirDeclarationOrigin.Source) {
                        collectedFiles.add(calleeFile)
                    }
                }
            }
        }
    }

    /**
     * Find and register the implementation of the used function.
     *
     * The 'expect' function is supposed to lack body, so compiling it won't have any effect.
     * Instead, we need to compile the 'actual' counterpart that does have a body.
     */
    private fun registerActualCounterpart(originalFunction: FirFunction) {
        if (actualizer == null) {
            // We aren't sure which implementation platform to choose. So, aborting
            return
        }

        val originalPsi = originalFunction.source?.psi as? KtDeclaration ?: return
        val originalModule = projectStructureProvider.getModule(originalPsi, useSiteModule = null)

        val targetModule = actualizer.actualize(originalModule) ?: return

        // Across all 'actual' declarations, find those with a matching platform kind, and register their containing files
        for (actualPsi in actualDeclarationProvider?.getActualDeclarations(originalPsi).orEmpty()) {
            val actualPsiFile = actualPsi.containingFile as? KtFile ?: continue
            val actualModule = projectStructureProvider.getModule(actualPsiFile, useSiteModule = null)

            // The file we found is from the correct actualized module
            if (targetModule == actualModule) {
                val actualResolveSession = resolveSessionService.getFirResolveSession(actualModule)
                val actualFile = actualResolveSession.getOrBuildFirFile(actualPsiFile)
                collectedFiles.add(actualFile)
            }
        }
    }

    private inline fun withInlineFunctionContext(function: FirFunction, block: () -> Unit) {
        val needsInlineContext = !isInlineFunctionContext && function.isInline

        try {
            if (needsInlineContext) {
                isInlineFunctionContext = true
            }
            block()
        } finally {
            if (needsInlineContext) {
                isInlineFunctionContext = false
            }
        }
    }
}