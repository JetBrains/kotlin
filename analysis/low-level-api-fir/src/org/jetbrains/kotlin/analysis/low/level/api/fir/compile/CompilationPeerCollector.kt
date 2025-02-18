/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compile

import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getContainingFile
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasBody
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.impl.FirContractCallBlock
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * This exception indicates that two or more inline functions reference each other. For example,
 * ```
 * inline fun a(): String = "Hi" + b()
 * inline fun b(): String = "Hi" + c()
 * inline fun c(): String = "Hi" + a()
 * ```
 * since we do not have a way to inline the above functions, we have to throw an exception.
 */
class CyclicInlineDependencyException(message: String) : IllegalStateException(message)

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
object CompilationPeerCollector {
    fun process(declaration: FirDeclaration): CompilationPeerData {
        val visitor = CompilationPeerCollectingVisitor()
        visitor.process(declaration)
        return CompilationPeerData(visitor.files, visitor.inlinedClasses)
    }
}

class CompilationPeerData(
    /**
     * File with the original declaration and all files with called inline functions. [CompilationPeerCollector.process]
     * recursively collects them and keeps them in a post order. For example,
     *  - A is main source module. A has dependency on source module libraries B and C.
     *  - B contains an inline function. B has dependency on a source module library C.
     *  - C contains an inline function.
     *  - [filesToCompile] returned by [CompilationPeerCollector.process] will be {C, B, A}.
     *
     * More formally, i-th element of [filesToCompile] will not have inline-dependency on any j-th element of
     * [filesToCompile], where j > i.
     *
     * This list does not contain duplicated files.
     */
    val filesToCompile: List<KtFile>,

    /** Local classes inlined as a part of inline functions. */
    val inlinedClasses: Set<KtClassOrObject>
)

class CompilationPeerCollectingVisitor : FirDefaultVisitorVoid() {
    val processed = mutableSetOf<FirDeclaration>()

    /** The entry of this class must be [process]. In that case, [queue] will always be initialized by [process] */
    lateinit var queue: MutableSet<FirDeclaration>

    val collectedFiles = mutableListOf<KtFile>()
    val collectedInlinedClasses = mutableSetOf<KtClassOrObject>()
    var isInlineFunctionContext = false

    val files: List<KtFile>
        get() = collectedFiles

    val inlinedClasses: Set<KtClassOrObject>
        get() = collectedInlinedClasses

    fun process(declaration: FirDeclaration) {
        ProgressManager.checkCanceled()

        val containingKtFile = declaration.psi?.containingFile as? KtFile ?: return
        if (containingKtFile.isCompiled || containingKtFile in collectedFiles) return

        if (!processed.add(declaration)) {
            val exceptionMessage = buildString {
                appendLine("Inline functions have a cyclic dependency:")
                for (problematicFir in processed) {
                    val problematicFirFile = problematicFir.getContainingFile()
                    if (problematicFirFile != null) {
                        append('[')
                            .append(problematicFirFile.packageFqName.asString())
                            .append('/')
                            .append(problematicFirFile.name)
                            .appendLine(']')
                    } else {
                        appendLine("(No containing file)")
                    }
                    appendLine(problematicFir.render())
                }
            }

            throw CyclicInlineDependencyException(exceptionMessage.trim())
        }

        val inlineFunctionsUsedByDeclaration = mutableSetOf<FirDeclaration>()
        queue = inlineFunctionsUsedByDeclaration
        declaration.accept(this)

        for (dependency in inlineFunctionsUsedByDeclaration) {
            process(dependency)
        }

        /* When we have FirDeclarations other than `declaration` in the same file, and they use inline functions,
           we have to collect them as well. For example, if `foo.kt` has the following functions:
           ```
           inline fun inline1() = .. inlineFromOtherFile() ..
           fun bar() = .. inline2() .. // where inline2() is another inline function
           ```
           When `declaration` is `inline1`, we have to collect `inline2` as well. Since file is the unit of JVM IR gen,
           without `inline2`, the JVM IR gen filling inline functions causes an exception reporting that it's missing. */
        val inlineFunctionsUsedInSameFile = mutableSetOf<FirDeclaration>()
        queue = inlineFunctionsUsedInSameFile
        declaration.getContainingFile()?.accept(this)
        for (dependency in inlineFunctionsUsedInSameFile) {
            if (dependency !in processed) process(dependency)
        }

        // Since we want to put a file into `collectedFiles` only when its all dependencies are already in `collectedFiles`,
        // we have to use the post-order traversal.
        if (containingKtFile !in collectedFiles) collectedFiles.add(containingKtFile)
    }

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

        val oldIsInlineFunctionContext = isInlineFunctionContext
        try {
            isInlineFunctionContext = simpleFunction.isInline
            super.visitFunction(simpleFunction)
        } finally {
            isInlineFunctionContext = oldIsInlineFunctionContext
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
    fun processResolvable(element: FirResolvable) {
        fun addToQueue(function: FirFunction?) {
            val original = function?.unwrapSubstitutionOverrides() ?: return
            if (original.isInline && original.hasBody) {
                queue.add(function)
            }
        }

        val reference = element.calleeReference
        if (reference !is FirResolvedNamedReference) {
            return
        }

        val symbol = reference.resolvedSymbol
        if (symbol is FirCallableSymbol<*>) {
            when (val fir = symbol.fir) {
                is FirFunction -> {
                    addToQueue(fir)
                }
                is FirProperty -> {
                    addToQueue(fir.getter)
                    addToQueue(fir.setter)
                }
                else -> {}
            }
        }
    }
}