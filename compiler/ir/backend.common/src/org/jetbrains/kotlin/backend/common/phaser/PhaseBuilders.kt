/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFileSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.deepCopySavingMetadata
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

// Phase composition.
private class CompositePhase<Context : CommonBackendContext, Input, Output>(
    val phases: List<CompilerPhase<Context, Any?, Any?>>
) : CompilerPhase<Context, Input, Output> {

    override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Input>, context: Context, input: Input): Output {
        @Suppress("UNCHECKED_CAST") var currentState = phaserState as PhaserState<Any?>
        var result = phases.first().invoke(phaseConfig, currentState, context, input)
        for ((previous, next) in phases.zip(phases.drop(1))) {
            if (next !is SameTypeCompilerPhase<*, *>) {
                // Discard `stickyPostconditions`, they are useless since data type is changing.
                currentState = currentState.changeType()
            }
            currentState.stickyPostconditions.addAll(previous.stickyPostconditions)
            result = next.invoke(phaseConfig, currentState, context, result)
        }
        @Suppress("UNCHECKED_CAST")
        return result as Output
    }

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, NamedCompilerPhase<Context, *>>> =
        phases.flatMap { it.getNamedSubphases(startDepth) }

    override val stickyPostconditions get() = phases.last().stickyPostconditions
}

@Suppress("UNCHECKED_CAST")
infix fun <Context : CommonBackendContext, Input, Mid, Output> CompilerPhase<Context, Input, Mid>.then(
    other: CompilerPhase<Context, Mid, Output>
): CompilerPhase<Context, Input, Output> {
    val unsafeThis = this as CompilerPhase<Context, Any?, Any?>
    val unsafeOther = other as CompilerPhase<Context, Any?, Any?>
    return CompositePhase(if (this is CompositePhase<Context, *, *>) phases + unsafeOther else listOf(unsafeThis, unsafeOther))
}

fun <Context : CommonBackendContext, Element : IrElement> makeCustomPhase(
    op: (Context, Element) -> Unit,
    name: String,
    description: String,
    prerequisite: Set<NamedCompilerPhase<Context, *>> = emptySet(),
    preconditions: Set<Checker<Element>> = emptySet(),
    postconditions: Set<Checker<Element>> = emptySet(),
    stickyPostconditions: Set<Checker<Element>> = emptySet(),
    actions: Set<Action<Element, Context>> = setOf(defaultDumper, validationAction),
    nlevels: Int = 1
): NamedCompilerPhase<Context, Element> =
    NamedCompilerPhase(
        name, description, prerequisite, CustomPhaseAdapter(op), preconditions, postconditions, stickyPostconditions, actions, nlevels,
    )

private class CustomPhaseAdapter<Context : CommonBackendContext, Element>(
    private val op: (Context, Element) -> Unit
) : SameTypeCompilerPhase<Context, Element> {
    override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Element>, context: Context, input: Element): Element {
        op(context, input)
        return input
    }
}

fun <Context : CommonBackendContext> namedUnitPhase(
    name: String,
    description: String,
    prerequisite: Set<NamedCompilerPhase<Context, *>> = emptySet(),
    nlevels: Int = 1,
    lower: CompilerPhase<Context, Unit, Unit>
): NamedCompilerPhase<Context, Unit> =
    NamedCompilerPhase(
        name, description, prerequisite, lower, nlevels = nlevels
    )

@Suppress("unused") // Used in kotlin-native
fun <Context : CommonBackendContext> namedOpUnitPhase(
    name: String,
    description: String,
    prerequisite: Set<NamedCompilerPhase<Context, *>>,
    op: Context.() -> Unit
): NamedCompilerPhase<Context, Unit> = namedUnitPhase(
    name, description, prerequisite,
    nlevels = 0,
    lower = object : SameTypeCompilerPhase<Context, Unit> {
        override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Unit>, context: Context, input: Unit) {
            context.op()
        }
    }
)

fun <Context : CommonBackendContext> performByIrFile(
    name: String = "PerformByIrFile",
    description: String = "Perform phases by IrFile",
    lower: List<CompilerPhase<Context, IrFile, IrFile>>
): NamedCompilerPhase<Context, IrModuleFragment> =
    NamedCompilerPhase(
        name, description, emptySet(), PerformByIrFilePhase(lower), emptySet(), emptySet(), emptySet(),
        setOf(defaultDumper), nlevels = 1,
    )

private class PerformByIrFilePhase<Context : CommonBackendContext>(
    private val lower: List<CompilerPhase<Context, IrFile, IrFile>>
) : SameTypeCompilerPhase<Context, IrModuleFragment> {
    override fun invoke(
        phaseConfig: PhaseConfig,
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
        phaseConfig: PhaseConfig, phaserState: PhaserState<IrModuleFragment>, context: Context, input: IrModuleFragment
    ): IrModuleFragment {
        for (irFile in input.files) {
            try {
                val filePhaserState = phaserState.changeType<IrModuleFragment, IrFile>()
                for (phase in lower) {
                    phase.invoke(phaseConfig, filePhaserState, context, irFile)
                }
            } catch (e: Throwable) {
                CodegenUtil.reportBackendException(e, "IR lowering", irFile.fileEntry.name)
            }
        }
        
        // TODO: no guarantee that module identity is preserved by `lower`
        return input
    }

    private fun invokeParallel(
        phaseConfig: PhaseConfig, phaserState: PhaserState<IrModuleFragment>, context: Context, input: IrModuleFragment, nThreads: Int
    ): IrModuleFragment {
        if (input.files.isEmpty()) return input

        // We can only report one exception through ISE
        val thrownFromThread = AtomicReference<Pair<Throwable, IrFile>?>(null)

        val remappedFiles = mutableMapOf<IrFileSymbol, IrFileSymbol>()
        val remappedFunctions = mutableMapOf<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol>()
        val remappedClasses = mutableMapOf<IrClassSymbol, IrClassSymbol>()

        // Each thread needs its own copy of phaserState.alreadyDone
        val filesAndStates = input.files.map {
            it.copySavingMappings(remappedFiles, remappedFunctions, remappedClasses) to phaserState.copyOf()
        }

        val executor = Executors.newFixedThreadPool(nThreads)
        for ((irFile, state) in filesAndStates) {
            executor.execute {
                try {
                    val filePhaserState = state.changeType<IrModuleFragment, IrFile>()
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
            CodegenUtil.reportBackendException(e, "IrLowering", irFile.fileEntry.name)
        }

        // Presumably each thread has run through the same list of phases.
        phaserState.alreadyDone.addAll(filesAndStates[0].second.alreadyDone)

        input.files.clear()
        input.files.addAll(filesAndStates.map { (irFile, _) -> irFile }.toMutableList())

        adjustDefaultArgumentStubs(context, remappedFunctions)
        context.handleDeepCopy(remappedFiles, remappedClasses, remappedFunctions)
        input.transformChildrenVoid(CrossFileCallAdjuster(remappedFunctions))

        // TODO: no guarantee that module identity is preserved by `lower`
        return input
    }

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, NamedCompilerPhase<Context, *>>> =
        lower.flatMap { it.getNamedSubphases(startDepth) }
}

fun <Context : CommonBackendContext> makeIrFilePhase(
    lowering: (Context) -> FileLoweringPass,
    name: String,
    description: String,
    prerequisite: Set<NamedCompilerPhase<Context, *>> = emptySet(),
    preconditions: Set<Checker<IrFile>> = emptySet(),
    postconditions: Set<Checker<IrFile>> = emptySet(),
    stickyPostconditions: Set<Checker<IrFile>> = emptySet(),
    actions: Set<Action<IrFile, Context>> = setOf(defaultDumper, validationAction)
): NamedCompilerPhase<Context, IrFile> =
    NamedCompilerPhase(
        name, description, prerequisite, FileLoweringPhaseAdapter(lowering), preconditions, postconditions, stickyPostconditions, actions,
        nlevels = 0,
    )

private class FileLoweringPhaseAdapter<Context : CommonBackendContext>(
    private val lowering: (Context) -> FileLoweringPass
) : SameTypeCompilerPhase<Context, IrFile> {
    override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<IrFile>, context: Context, input: IrFile): IrFile {
        lowering(context).lower(input)
        return input
    }
}

fun <Context : CommonBackendContext> makeIrModulePhase(
    lowering: (Context) -> FileLoweringPass,
    name: String,
    description: String,
    prerequisite: Set<NamedCompilerPhase<Context, *>> = emptySet(),
    preconditions: Set<Checker<IrModuleFragment>> = emptySet(),
    postconditions: Set<Checker<IrModuleFragment>> = emptySet(),
    stickyPostconditions: Set<Checker<IrModuleFragment>> = emptySet(),
    actions: Set<Action<IrModuleFragment, Context>> = setOf(defaultDumper, validationAction)
): NamedCompilerPhase<Context, IrModuleFragment> =
    NamedCompilerPhase(
        name, description, prerequisite, ModuleLoweringPhaseAdapter(lowering), preconditions, postconditions, stickyPostconditions, actions,
        nlevels = 0,
    )

private class ModuleLoweringPhaseAdapter<Context : CommonBackendContext>(
    private val lowering: (Context) -> FileLoweringPass
) : SameTypeCompilerPhase<Context, IrModuleFragment> {
    override fun invoke(
        phaseConfig: PhaseConfig, phaserState: PhaserState<IrModuleFragment>, context: Context, input: IrModuleFragment
    ): IrModuleFragment {
        lowering(context).lower(input)
        return input
    }
}

@Suppress("unused") // Used in kotlin-native
fun <Context : CommonBackendContext, Input> unitSink(): CompilerPhase<Context, Input, Unit> =
    object : CompilerPhase<Context, Input, Unit> {
        override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Input>, context: Context, input: Input) {}
    }

// Intermediate phases to change the object of transformations
@Suppress("unused") // Used in kotlin-native
fun <Context : CommonBackendContext, OldData, NewData> takeFromContext(op: (Context) -> NewData): CompilerPhase<Context, OldData, NewData> =
    object : CompilerPhase<Context, OldData, NewData> {
        override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<OldData>, context: Context, input: OldData) = op(context)
    }

fun <Context : CommonBackendContext, OldData, NewData> transform(op: (OldData) -> NewData): CompilerPhase<Context, OldData, NewData> =
    object : CompilerPhase<Context, OldData, NewData> {
        override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<OldData>, context: Context, input: OldData) = op(input)
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

private class DeepCopySymbolRemapperSavingFunctions : DeepCopySymbolRemapper() {
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

fun adjustDefaultArgumentStubs(
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
) : IrElementTransformerVoid() {

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        declaration.overriddenSymbols = declaration.overriddenSymbols.map { remappedFunctions[it] ?: it }
        return super.visitSimpleFunction(declaration)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)
        return remappedFunctions[expression.symbol]?.let { newSymbol ->
            with(expression) {
                IrCallImpl(
                    startOffset, endOffset, type,
                    newSymbol,
                    typeArgumentsCount, valueArgumentsCount, origin,
                    superQualifierSymbol // TODO
                ).apply {
                    copyTypeAndValueArgumentsFrom(expression)
                }
            }
        } ?: expression
    }
}