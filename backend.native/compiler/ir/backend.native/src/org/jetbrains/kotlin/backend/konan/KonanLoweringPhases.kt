package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.konan.ir.ModuleIndex
import org.jetbrains.kotlin.backend.konan.lower.*
import org.jetbrains.kotlin.backend.konan.lower.ExpectDeclarationsRemoving
import org.jetbrains.kotlin.backend.konan.lower.FinallyBlocksLowering
import org.jetbrains.kotlin.backend.konan.lower.InitializersLowering
import org.jetbrains.kotlin.backend.konan.lower.loops.ForLoopsLowering
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.checkDeclarationParents
import org.jetbrains.kotlin.ir.util.patchDeclarationParents

private fun makeKonanFileLoweringPhase(
        lowering: (Context) -> FileLoweringPass,
        name: String,
        description: String,
        prerequisite: Set<AnyNamedPhase> = emptySet()
) = makeIrFilePhase(lowering, name, description, prerequisite)

private fun makeKonanModuleLoweringPhase(
        lowering: (Context) -> FileLoweringPass,
        name: String,
        description: String,
        prerequisite: Set<AnyNamedPhase> = emptySet()
) = makeIrModulePhase(lowering, name, description, prerequisite)

internal fun makeKonanFileOpPhase(
        op: (Context, IrFile) -> Unit,
        name: String,
        description: String,
        prerequisite: Set<AnyNamedPhase> = emptySet()
) = namedIrFilePhase(
        name, description, prerequisite, nlevels = 0,
        lower = object : SameTypeCompilerPhase<Context, IrFile> {
            override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: IrFile): IrFile {
                op(context, input)
                return input
            }
        }
)

internal fun makeKonanModuleOpPhase(
        op: (Context, IrModuleFragment) -> Unit,
        name: String,
        description: String,
        prerequisite: Set<AnyNamedPhase> = emptySet()
) = namedIrModulePhase(
        name, description, prerequisite, nlevels = 0,
        lower = object : SameTypeCompilerPhase<Context, IrModuleFragment> {
            override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: IrModuleFragment): IrModuleFragment {
                op(context, input)
                return input
            }
        }
)

internal val removeExpectDeclarationsPhase = makeKonanModuleLoweringPhase(
        ::ExpectDeclarationsRemoving,
        name = "RemoveExpectDeclarations",
        description = "Expect declarations removing"
)

internal val lowerBeforeInlinePhase = makeKonanModuleLoweringPhase(
        ::PreInlineLowering,
        name = "LowerBeforeInline",
        description = "Special operations processing before inlining"
)

internal val inlinePhase = namedIrModulePhase(
        lower = object : SameTypeCompilerPhase<Context, IrModuleFragment> {
            override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: IrModuleFragment): IrModuleFragment {
                FunctionInlining(context).inline(input)
                return input
            }
        },
        name = "Inline",
        description = "Functions inlining",
        prerequisite = setOf(lowerBeforeInlinePhase),
        nlevels = 0
)

internal val lowerAfterInlinePhase = makeKonanModuleOpPhase(
        { context, irModule ->
            irModule.files.forEach(PostInlineLowering(context)::lower)
            // TODO: Seems like this should be deleted in PsiToIR.
            irModule.files.forEach(ContractsDslRemover(context)::lower)
        },
        name = "LowerAfterInline",
        description = "Special operations processing after inlining"
)

internal val interopPart1Phase = makeKonanModuleLoweringPhase(
        ::InteropLoweringPart1,
        name = "InteropPart1",
        description = "Interop lowering, part 1",
        prerequisite = setOf(inlinePhase)
)

internal val patchDeclarationParents1Phase = makeKonanModuleOpPhase(
        { _, irModule -> irModule.patchDeclarationParents() },
        name = "PatchDeclarationParents1",
        description = "Patch declaration parents 1"
)

internal val checkDeclarationParentsPhase = makeKonanModuleOpPhase(
        { _, irModule -> irModule.checkDeclarationParents() },
        name = "CheckDeclarationParents",
        description = "Check declaration parents"
)

internal val validateIrModulePhase = makeKonanModuleOpPhase(
        { context, irModule -> validateIrModule(context, irModule) },
        name = "ValidateIrModule",
        description = "Validate generated module"
)

internal val moduleIndexForCodegenPhase = makeKonanModuleOpPhase(
        { context, irModule -> context.ir.moduleIndexForCodegen = ModuleIndex(irModule) },
        name = "ModuleIndexForCodeGen",
        description = "Generate module index for codegen"
)

/* IrFile phases */

internal val lateinitPhase = makeKonanFileLoweringPhase(
        ::LateinitLowering,
        name = "Lateinit",
        description = "Lateinit properties lowering",
        prerequisite = setOf(inlinePhase)
)

internal val stringConcatenationPhase = makeKonanFileLoweringPhase(
        ::StringConcatenationLowering,
        name = "StringConcatenation",
        description = "String concatenation lowering"
)

internal val enumConstructorsPhase = makeKonanFileLoweringPhase(
        ::EnumConstructorsLowering,
        name = "EnumConstructors",
        description = "Enum constructors lowering"
)

internal val initializersPhase = makeKonanFileLoweringPhase(
        ::InitializersLowering,
        name = "Initializers",
        description = "Initializers lowering",
        prerequisite = setOf(enumConstructorsPhase)
)

internal val sharedVariablesPhase = makeKonanFileLoweringPhase(
        ::SharedVariablesLowering,
        name = "SharedVariables",
        description = "Shared variable lowering",
        prerequisite = setOf(initializersPhase)
)

internal val localFunctionsPhase = makeKonanFileOpPhase(
        op = { context, irFile ->
            LocalDelegatedPropertiesLowering().lower(irFile)
            LocalDeclarationsLowering(context).runOnFilePostfix(irFile)
        },
        name = "LocalFunctions",
        description = "Local function lowering",
        prerequisite = setOf(sharedVariablesPhase)
)

internal val tailrecPhase = makeKonanFileLoweringPhase(
        ::TailrecLowering,
        name = "Tailrec",
        description = "Tailrec lowering",
        prerequisite = setOf(localFunctionsPhase)
)

internal val defaultParameterExtentPhase = makeKonanFileOpPhase(
        { context, irFile ->
            DefaultArgumentStubGenerator(context, skipInlineMethods = false).runOnFilePostfix(irFile)
            KonanDefaultParameterInjector(context).lower(irFile)
        },
        name = "DefaultParameterExtent",
        description = "Default parameter extent lowering",
        prerequisite = setOf(tailrecPhase, enumConstructorsPhase)
)

internal val innerClassPhase = makeKonanFileLoweringPhase(
        ::InnerClassLowering,
        name = "InnerClasses",
        description = "Inner classes lowering",
        prerequisite = setOf(defaultParameterExtentPhase)
)

internal val forLoopsPhase = makeKonanFileLoweringPhase(
        ::ForLoopsLowering,
        name = "ForLoops",
        description = "For loops lowering"
)

internal val dataClassesPhase = makeKonanFileLoweringPhase(
        ::DataClassOperatorsLowering,
        name = "DataClasses",
        description = "Data classes lowering"
)

internal val builtinOperatorPhase = makeKonanFileLoweringPhase(
        ::BuiltinOperatorLowering,
        name = "BuiltinOperators",
        description = "BuiltIn operators lowering",
        prerequisite = setOf(defaultParameterExtentPhase)
)

internal val finallyBlocksPhase = makeKonanFileLoweringPhase(
        ::FinallyBlocksLowering,
        name = "FinallyBlocks",
        description = "Finally blocks lowering",
        prerequisite = setOf(initializersPhase, localFunctionsPhase, tailrecPhase)
)

internal val testProcessorPhase = makeKonanFileOpPhase(
        { context, irFile -> TestProcessor(context).process(irFile) },
        name = "TestProcessor",
        description = "Unit test processor"
)

internal val enumClassPhase = makeKonanFileOpPhase(
        { context, irFile -> EnumClassLowering(context).run(irFile) },
        name = "Enums",
        description = "Enum classes lowering",
        prerequisite = setOf(enumConstructorsPhase) // TODO: make weak dependency on `testProcessorPhase`
)

internal val delegationPhase = makeKonanFileLoweringPhase(
        ::PropertyDelegationLowering,
        name = "Delegation",
        description = "Delegation lowering"
)

internal val callableReferencePhase = makeKonanFileLoweringPhase(
        ::CallableReferenceLowering,
        name = "CallableReference",
        description = "Callable references lowering",
        prerequisite = setOf(delegationPhase) // TODO: make weak dependency on `testProcessorPhase`
)

internal val interopPart2Phase = makeKonanFileLoweringPhase(
        ::InteropLoweringPart2,
        name = "InteropPart2",
        description = "Interop lowering, part 2",
        prerequisite = setOf(localFunctionsPhase)
)

internal val varargPhase = makeKonanFileLoweringPhase(
        ::VarargInjectionLowering,
        name = "Vararg",
        description = "Vararg lowering",
        prerequisite = setOf(callableReferencePhase, defaultParameterExtentPhase)
)

internal val compileTimeEvaluatePhase = makeKonanFileLoweringPhase(
        ::CompileTimeEvaluateLowering,
        name = "CompileTimeEvaluate",
        description = "Compile time evaluation lowering",
        prerequisite = setOf(varargPhase)
)

internal val coroutinesPhase = makeKonanFileLoweringPhase(
        ::SuspendFunctionsLowering,
        name = "Coroutines",
        description = "Coroutines lowering",
        prerequisite = setOf(localFunctionsPhase, finallyBlocksPhase)
)

internal val typeOperatorPhase = makeKonanFileLoweringPhase(
        ::TypeOperatorLowering,
        name = "TypeOperators",
        description = "Type operators lowering",
        prerequisite = setOf(coroutinesPhase)
)

internal val bridgesPhase = makeKonanFileOpPhase(
        { context, irFile ->
            BridgesBuilding(context).runOnFilePostfix(irFile)
            WorkersBridgesBuilding(context).lower(irFile)
        },
        name = "Bridges",
        description = "Bridges building",
        prerequisite = setOf(coroutinesPhase)
)

internal val autoboxPhase = makeKonanFileOpPhase(
        { context, irFile ->
            // validateIrFile(context, irFile) // Temporarily disabled until moving to new IR finished.
            Autoboxing(context).lower(irFile)
        },
        name = "Autobox",
        description = "Autoboxing of primitive types",
        prerequisite = setOf(bridgesPhase, coroutinesPhase)
)

internal val returnsInsertionPhase = makeKonanFileLoweringPhase(
        ::ReturnsInsertionLowering,
        name = "ReturnsInsertion",
        description = "Returns insertion for Unit functions",
        prerequisite = setOf(autoboxPhase, coroutinesPhase, enumClassPhase)
)