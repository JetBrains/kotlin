/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapperPreservingSignatures
import org.jetbrains.kotlin.ir.util.deepCopySavingMetadata
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

fun <Context : CommonBackendContext> performByIrFile(
    name: String = "PerformByIrFile",
    description: String = "Perform phases by IrFile",
    copyBeforeLowering: Boolean = true,
    lower: List<CompilerPhase<Context, IrFile, IrFile>>,
): SameTypeNamedCompilerPhase<Context, IrModuleFragment> =
    SameTypeNamedCompilerPhase(
        name, description, emptySet(), PerformByIrFilePhase(lower, copyBeforeLowering), emptySet(), emptySet(), emptySet(),
        setOf(defaultDumper), nlevels = 1,
    )

private class PerformByIrFilePhase<Context : CommonBackendContext>(
    private val lower: List<CompilerPhase<Context, IrFile, IrFile>>,
    private val copyBeforeLowering: Boolean,
) : SameTypeCompilerPhase<Context, IrModuleFragment> {
    override fun invoke(
        phaseConfig: PhaseConfigurationService,
        phaserState: PhaserState<IrModuleFragment>,
        context: Context,
        input: IrModuleFragment
    ): IrModuleFragment {
        val nThreads = context.configuration.get(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS) ?: 1
        return if (nThreads > 1)
            invokeParallel(phaseConfig, phaserState, context, input, nThreads)
        else
            invokeSequential(phaseConfig, phaserState, context, input)
    }

    private fun invokeSequential(
        phaseConfig: PhaseConfigurationService, phaserState: PhaserState<IrModuleFragment>, context: Context, input: IrModuleFragment
    ): IrModuleFragment {
        for (irFile in input.files) {
            try {
                val filePhaserState = phaserState.changePhaserStateType<IrModuleFragment, IrFile>()
                for (phase in lower) {
                    phase.invoke(phaseConfig, filePhaserState, context, irFile)
                }
            } catch (e: Throwable) {
                CodegenUtil.reportBackendException(e, "IR lowering", irFile.fileEntry.name) { offset ->
                    irFile.fileEntry.takeIf { it.supportsDebugInfo }?.let {
                        val (line, column) = it.getLineAndColumnNumbers(offset)
                        line to column
                    }
                }
            }
        }

        // TODO: no guarantee that module identity is preserved by `lower`
        return input
    }

    private fun invokeParallel(
        phaseConfig: PhaseConfigurationService, phaserState: PhaserState<IrModuleFragment>, context: Context, input: IrModuleFragment, nThreads: Int
    ): IrModuleFragment {
        if (input.files.isEmpty()) return input

        // We can only report one exception through ISE
        val thrownFromThread = AtomicReference<Pair<Throwable, IrFile>?>(null)

        val remappedFiles = mutableMapOf<IrFileSymbol, IrFileSymbol>()
        val remappedFunctions = mutableMapOf<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol>()
        val remappedClasses = mutableMapOf<IrClassSymbol, IrClassSymbol>()

        // Each thread needs its own copy of phaserState.alreadyDone
        val filesAndStates = input.files.map {
            if (copyBeforeLowering)
                it.copySavingMappings(remappedFiles, remappedFunctions, remappedClasses) to phaserState.copyOf()
            else
                it to phaserState.copyOf()
        }

        val executor = Executors.newFixedThreadPool(nThreads)
        for ((irFile, state) in filesAndStates) {
            executor.execute {
                try {
                    val filePhaserState = state.changePhaserStateType<IrModuleFragment, IrFile>()
                    for (phase in lower) {
                        phase.invoke(phaseConfig, filePhaserState, context, irFile)
                    }
                } catch (e: Throwable) {
                    thrownFromThread.set(Pair(e, irFile))
                }
            }
        }
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.DAYS) // Wait long enough

        thrownFromThread.get()?.let { (e, irFile) ->
            CodegenUtil.reportBackendException(e, "Experimental parallel IR backend", irFile.fileEntry.name) { offset ->
                irFile.fileEntry.takeIf { it.supportsDebugInfo }?.let {
                    val (line, column) = it.getLineAndColumnNumbers(offset)
                    line to column
                }
            }
        }

        // Presumably each thread has run through the same list of phases.
        phaserState.alreadyDone.addAll(filesAndStates[0].second.alreadyDone)

        // Repair after working on copied files.
        if (copyBeforeLowering) {
            input.files.clear()
            input.files.addAll(filesAndStates.map { (irFile, _) -> irFile }.toMutableList())

            // Some remappers in handleDeepCopy depend on entries in remappedFunctions inserted by adjustDefaultArgumentStubs.
            adjustDefaultArgumentStubs(context, remappedFunctions)
            context.handleDeepCopy(remappedFiles, remappedClasses, remappedFunctions)
            // and some entries in adjustDefaultArgumentStubs depend on those inserted by handleDeepCopy, so we need to repeat the call.
            adjustDefaultArgumentStubs(context, remappedFunctions)

            input.acceptChildrenVoid(CrossFileCallAdjuster(remappedFunctions))
        }

        // TODO: no guarantee that module identity is preserved by `lower`
        return input
    }

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, AbstractNamedCompilerPhase<Context, *, *>>> =
        lower.flatMap { it.getNamedSubphases(startDepth) }
}

// We need to remap inline function calls after lowering files

fun IrFile.copySavingMappings(
    remappedFiles: MutableMap<IrFileSymbol, IrFileSymbol>,
    remappedFunctions: MutableMap<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol>,
    remappedClasses: MutableMap<IrClassSymbol, IrClassSymbol>,
): IrFile {
    val symbolRemapper = DeepCopySymbolRemapperSavingFunctions()

    val newIrFile = deepCopySavingMetadata(symbolRemapper = symbolRemapper)

    for (function in symbolRemapper.declaredFunctions) {
        remappedFunctions[function] = symbolRemapper.getReferencedSimpleFunction(function)
    }
    for (klass in symbolRemapper.declaredClasses) {
        remappedClasses[klass] = symbolRemapper.getReferencedClass(klass)
    }

    remappedFiles[symbol] = newIrFile.symbol

    return newIrFile
}

private class DeepCopySymbolRemapperSavingFunctions : DeepCopySymbolRemapperPreservingSignatures() {
    val declaredFunctions = mutableSetOf<IrSimpleFunctionSymbol>()
    val declaredClasses = mutableSetOf<IrClassSymbol>()

    override fun getDeclaredFunction(symbol: IrSimpleFunctionSymbol): IrSimpleFunctionSymbol {
        declaredFunctions.add(symbol)
        return super.getDeclaredFunction(symbol)
    }

    override fun getDeclaredClass(symbol: IrClassSymbol): IrClassSymbol {
        declaredClasses.add(symbol)
        return super.getDeclaredClass(symbol)
    }
}

private fun adjustDefaultArgumentStubs(
    context: CommonBackendContext,
    remappedFunctions: MutableMap<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol>,
) {
    for (defaultStub in context.mapping.defaultArgumentsOriginalFunction.keys) {
        if (defaultStub !is IrSimpleFunction) continue
        val original = context.mapping.defaultArgumentsOriginalFunction[defaultStub] as? IrSimpleFunction ?: continue
        val originalNew = remappedFunctions[original.symbol]?.owner ?: continue
        val defaultStubNew = context.mapping.defaultArgumentsDispatchFunction[originalNew] ?: continue
        remappedFunctions[defaultStub.symbol] = defaultStubNew.symbol as IrSimpleFunctionSymbol
    }
}

private class CrossFileCallAdjuster(
    val remappedFunctions: Map<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol>
) : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        declaration.overriddenSymbols = declaration.overriddenSymbols.map { remappedFunctions[it] ?: it }
        declaration.acceptChildren(this, null)
    }

    override fun visitCall(expression: IrCall) {
        expression.acceptChildren(this, null)
        remappedFunctions[expression.symbol]?.let {
            expression.symbol = it
        }
    }
}
