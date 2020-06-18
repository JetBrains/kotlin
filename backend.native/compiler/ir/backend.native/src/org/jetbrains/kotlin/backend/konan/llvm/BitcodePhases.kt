/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.common.phaser.CompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.PhaserState
import org.jetbrains.kotlin.backend.common.phaser.namedUnitPhase
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.GlobalHierarchyAnalysis
import org.jetbrains.kotlin.backend.konan.optimizations.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.cast

internal val contextLLVMSetupPhase = makeKonanModuleOpPhase(
        name = "ContextLLVMSetup",
        description = "Set up Context for LLVM Bitcode generation",
        op = { context, _ ->
            // Note that we don't set module target explicitly.
            // It is determined by the target of runtime.bc
            // (see Llvm class in ContextUtils)
            // Which in turn is determined by the clang flags
            // used to compile runtime.bc.
            llvmContext = LLVMContextCreate()!!
            val llvmModule = LLVMModuleCreateWithNameInContext("out", llvmContext)!!
            context.llvmModule = llvmModule
            context.debugInfo.builder = LLVMCreateDIBuilder(llvmModule)

            // we don't split path to filename and directory to provide enough level uniquely for dsymutil to avoid symbol
            // clashing, which happens on linking with libraries produced from intercepting sources.
            val filePath = context.config.outputFile.toFileAndFolder(context).path()

            context.debugInfo.compilationUnit = if (context.shouldContainLocationDebugInfo()) DICreateCompilationUnit(
                    builder = context.debugInfo.builder,
                    lang = DWARF.language(context.config),
                    File = filePath,
                    dir = "",
                    producer = DWARF.producer,
                    isOptimized = 0,
                    flags = "",
                    rv = DWARF.runtimeVersion(context.config)).cast()
            else null
        }
)

internal val createLLVMDeclarationsPhase = makeKonanModuleOpPhase(
        name = "CreateLLVMDeclarations",
        description = "Map IR declarations to LLVM",
        prerequisite = setOf(contextLLVMSetupPhase),
        op = { context, _ ->
            context.llvmDeclarations = createLlvmDeclarations(context)
            context.lifetimes = mutableMapOf()
            context.codegenVisitor = CodeGeneratorVisitor(context, context.lifetimes)
        }
)

internal val disposeLLVMPhase = namedUnitPhase(
        name = "DisposeLLVM",
        description = "Dispose LLVM",
        lower = object : CompilerPhase<Context, Unit, Unit> {
            override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Unit>, context: Context, input: Unit) {
                context.disposeLlvm()
            }
        }
)

internal val RTTIPhase = makeKonanModuleOpPhase(
        name = "RTTI",
        description = "RTTI generation",
        op = { context, irModule ->
            val visitor = RTTIGeneratorVisitor(context)
            irModule.acceptVoid(visitor)
            visitor.dispose()
        }
)

internal val generateDebugInfoHeaderPhase = makeKonanModuleOpPhase(
        name = "GenerateDebugInfoHeader",
        description = "Generate debug info header",
        op = { context, _ -> generateDebugInfoHeader(context) }
)

internal val buildDFGPhase = makeKonanModuleOpPhase(
        name = "BuildDFG",
        description = "Data flow graph building",
        op = { context, irModule ->
            context.moduleDFG = ModuleDFGBuilder(context, irModule).build()
        }
)

internal val deserializeDFGPhase = makeKonanModuleOpPhase(
        name = "DeserializeDFG",
        description = "Data flow graph deserializing",
        op = { context, _ ->
            context.externalModulesDFG = DFGSerializer.deserialize(
                    context,
                    context.moduleDFG!!.symbolTable.privateTypeIndex,
                    context.moduleDFG!!.symbolTable.privateFunIndex
            )
        }
)

internal val devirtualizationPhase = makeKonanModuleOpPhase(
        name = "Devirtualization",
        description = "Devirtualization",
        prerequisite = setOf(buildDFGPhase),
        op = { context, irModule ->
            context.devirtualizationAnalysisResult = Devirtualization.run(
                    irModule, context, context.moduleDFG!!, ExternalModulesDFG(emptyList(), emptyMap(), emptyMap(), emptyMap())
            )
        }
)

internal val ghaPhase = makeKonanModuleOpPhase(
        name = "GHAPhase",
        description = "Global hierarchy analysis",
        op = { context, irModule -> GlobalHierarchyAnalysis(context, irModule).run() }
)

internal val IrFunction.longName: String
        get() = "${(parent as? IrClass)?.name?.asString() ?: "<root>"}.${(this as? IrSimpleFunction)?.name ?: "<init>"}"

internal val dcePhase = makeKonanModuleOpPhase(
        name = "DCEPhase",
        description = "Dead code elimination",
        prerequisite = setOf(devirtualizationPhase),
        op = { context, _ ->
            val externalModulesDFG = ExternalModulesDFG(emptyList(), emptyMap(), emptyMap(), emptyMap())

            val callGraph = CallGraphBuilder(
                    context, context.moduleDFG!!,
                    externalModulesDFG,
                    context.devirtualizationAnalysisResult!!,
                    true
            ).build()

            val referencedFunctions = mutableSetOf<IrFunction>()
            for (node in callGraph.directEdges.values) {
                if (!node.symbol.isGlobalInitializer)
                    referencedFunctions.add(node.symbol.irFunction ?: error("No IR for: ${node.symbol}"))
                node.callSites.forEach {
                    assert (!it.isVirtual) { "There should be no virtual calls in the call graph, but was: ${it.actualCallee}" }
                    referencedFunctions.add(it.actualCallee.irFunction ?: error("No IR for: ${it.actualCallee}"))
                }
            }

            context.irModule!!.acceptChildrenVoid(object: IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitFunction(declaration: IrFunction) {
                    // TODO: Generalize somehow, not that graceful.
                    if (declaration.name == OperatorNameConventions.INVOKE
                            && declaration.parent.let { it is IrClass && it.defaultType.isFunction() }) {
                        referencedFunctions.add(declaration)
                    }
                    super.visitFunction(declaration)
                }

                override fun visitConstructor(declaration: IrConstructor) {
                    // TODO: NativePointed is the only inline class for which the field's type and
                    //       the constructor parameter's type are different.
                    //       Thus we need to conserve the constructor no matter if it was actually referenced somehow or not.
                    //       See [IrTypeInlineClassesSupport.getInlinedClassUnderlyingType] why.
                    if (declaration.parentAsClass.name.asString() == InteropFqNames.nativePointedName && declaration.isPrimary)
                        referencedFunctions.add(declaration)
                    super.visitConstructor(declaration)
                }
            })

            context.irModule!!.transformChildrenVoid(object: IrElementTransformerVoid() {
                override fun visitFile(declaration: IrFile): IrFile {
                    declaration.declarations.removeAll {
                        (it is IrFunction && !referencedFunctions.contains(it))
                    }
                    return super.visitFile(declaration)
                }

                override fun visitClass(declaration: IrClass): IrStatement {
                    if (declaration == context.ir.symbols.nativePointed)
                        return super.visitClass(declaration)
                    declaration.declarations.removeAll {
                        (it is IrFunction && it.isReal && !referencedFunctions.contains(it))
                    }
                    return super.visitClass(declaration)
                }

                override fun visitProperty(declaration: IrProperty): IrStatement {
                    if (declaration.getter.let { it != null && it.isReal && !referencedFunctions.contains(it) }) {
                        declaration.getter = null
                    }
                    if (declaration.setter.let { it != null && it.isReal && !referencedFunctions.contains(it) }) {
                        declaration.setter = null
                    }
                    return super.visitProperty(declaration)
                }
            })

            context.referencedFunctions = referencedFunctions
        }
)

internal val escapeAnalysisPhase = makeKonanModuleOpPhase(
        // Disabled by default !!!!
        name = "EscapeAnalysis",
        description = "Escape analysis",
        prerequisite = setOf(buildDFGPhase, deserializeDFGPhase), // TODO: Requires devirtualization.
        op = { context, _ ->
            context.externalModulesDFG?.let { externalModulesDFG ->
                val callGraph = CallGraphBuilder(
                        context, context.moduleDFG!!,
                        externalModulesDFG,
                        context.devirtualizationAnalysisResult!!,
                        false
                ).build()
                EscapeAnalysis.computeLifetimes(
                        context, context.moduleDFG!!, externalModulesDFG, callGraph, context.lifetimes
                )
            }
        }
)

internal val localEscapeAnalysisPhase = makeKonanModuleOpPhase(
        name = "LocalEscapeAnalysis",
        description = "Local escape analysis",
        prerequisite = setOf(buildDFGPhase, devirtualizationPhase),
        op = { context, _ ->
            LocalEscapeAnalysis.computeLifetimes(context, context.moduleDFG!!, context.lifetimes)
        }
)

internal val serializeDFGPhase = makeKonanModuleOpPhase(
        name = "SerializeDFG",
        description = "Data flow graph serializing",
        prerequisite = setOf(buildDFGPhase), // TODO: Requires escape analysis.
        op = { context, _ ->
            DFGSerializer.serialize(context, context.moduleDFG!!)
        }
)

internal val codegenPhase = makeKonanModuleOpPhase(
        name = "Codegen",
        description = "Code generation",
        op = { context, irModule ->
            irModule.acceptVoid(context.codegenVisitor)
        }
)

internal val finalizeDebugInfoPhase = makeKonanModuleOpPhase(
        name = "FinalizeDebugInfo",
        description = "Finalize debug info",
        op = { context, _ ->
            if (context.shouldContainAnyDebugInfo()) {
                DIFinalize(context.debugInfo.builder)
            }
        }
)

internal val cStubsPhase = makeKonanModuleOpPhase(
        name = "CStubs",
        description = "C stubs compilation",
        op = { context, _ -> produceCStubs(context) }
)

internal val linkBitcodeDependenciesPhase = makeKonanModuleOpPhase(
        name = "LinkBitcodeDependencies",
        description = "Link bitcode dependencies",
        op = { context, _ -> linkBitcodeDependencies(context) }
)

internal val bitcodeOptimizationPhase = makeKonanModuleOpPhase(
        name = "BitcodeOptimization",
        description = "Optimize bitcode",
        op = { context, _ -> runLlvmOptimizationPipeline(context) }
)

internal val produceOutputPhase = namedUnitPhase(
        name = "ProduceOutput",
        description = "Produce output",
        lower = object : CompilerPhase<Context, Unit, Unit> {
            override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Unit>, context: Context, input: Unit) {
                produceOutput(context)
            }
        }
)

internal val verifyBitcodePhase = makeKonanModuleOpPhase(
        name = "VerifyBitcode",
        description = "Verify bitcode",
        op = { context, _ -> context.verifyBitCode() }
)

internal val printBitcodePhase = makeKonanModuleOpPhase(
        name = "PrintBitcode",
        description = "Print bitcode",
        op = { context, _ ->
            if (context.shouldPrintBitCode()) {
                context.printBitCode()
            }
        }
)