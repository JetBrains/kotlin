/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.ir.backend.js.lower.*
import org.jetbrains.kotlin.ir.backend.js.lower.calls.CallsLowering
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.CoroutineIntrinsicLowering
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.SuspendFunctionsLowering
import org.jetbrains.kotlin.ir.backend.js.lower.inline.FunctionInlining
import org.jetbrains.kotlin.ir.backend.js.lower.inline.RemoveInlineFunctionsWithReifiedTypeParametersLowering
import org.jetbrains.kotlin.ir.backend.js.lower.inline.ReturnableBlockLowering
import org.jetbrains.kotlin.ir.backend.js.lower.inline.replaceUnboundSymbols
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.patchDeclarationParents

private fun FileLoweringPass.lower(moduleFragment: IrModuleFragment) = moduleFragment.files.forEach { lower(it) }

private fun DeclarationContainerLoweringPass.runOnFilesPostfix(files: Iterable<IrFile>) = files.forEach { runOnFilePostfix(it) }

private fun ClassLoweringPass.runOnFilesPostfix(moduleFragment: IrModuleFragment) = moduleFragment.files.forEach { runOnFilePostfix(it) }


object IrModuleStartPhase : CompilerPhase<BackendContext, IrModuleFragment> {
    override val name = "IrModuleFragment"
    override val description = "State at start of IrModuleFragment lowering"
    override val prerequisite = emptySet()
    override fun invoke(context: BackendContext, input: IrModuleFragment) = input
}

private fun makeJsPhase(
    lowering: (JsIrBackendContext, IrModuleFragment) -> Unit,
    description: String,
    name: String,
    prerequisite: Set<CompilerPhase<JsIrBackendContext, IrModuleFragment>> = emptySet()
) = makePhase(lowering, description, name, prerequisite)

private val MoveBodilessDeclarationsToSeparatePlacePhase = makeJsPhase(
    { _, module -> MoveBodilessDeclarationsToSeparatePlace().lower(module) },
    name = "MoveBodilessDeclarationsToSeparatePlace",
    description = "Move `external` and `built-in` declarations into separate place to make the following lowerings do not care about them"
)

private val ExpectDeclarationsRemovingPhase = makeJsPhase(
    { context, module -> ExpectDeclarationsRemoving(context).lower(module) },
    name = "ExpectDeclarationsRemoving",
    description = "Remove expect declaration from module fragment"
)

private val CoroutineIntrinsicLoweringPhase = makeJsPhase(
    { context, module -> CoroutineIntrinsicLowering(context).lower(module) },
    name = "CoroutineIntrinsicLowering",
    description = "Replace common coroutine intrinsics with platform specific ones"
)

private val ArrayInlineConstructorLoweringPhase = makeJsPhase(
    { context, module -> ArrayInlineConstructorLowering(context).lower(module) },
    name = "ArrayInlineConstructorLowering",
    description = "Replace array constructor with platform specific factory functions"
)

private val LateinitLoweringPhase = makeJsPhase(
    { context, module -> LateinitLowering(context).lower(module) },
    name = "LateinitLowering",
    description = "Insert checks for lateinit field references"
)

private val ModuleCopyingPhase = makeJsPhase(
    { context, module -> context.moduleFragmentCopy = module.deepCopyWithSymbols() },
    name = "ModuleCopying",
    description = "<Supposed to be removed> Copy current module to make it accessible from different one",
    prerequisite = setOf(LateinitLoweringPhase)
)

private val FunctionInliningPhase = makeJsPhase(
    { context, module ->
        FunctionInlining(context).inline(module)
        module.replaceUnboundSymbols(context)
        module.patchDeclarationParents()
    },
    name = "FunctionInliningPhase",
    description = "Perform function inlining",
    prerequisite = setOf(ModuleCopyingPhase, LateinitLoweringPhase, ArrayInlineConstructorLoweringPhase, CoroutineIntrinsicLoweringPhase)
)

private val RemoveInlineFunctionsWithReifiedTypeParametersLoweringPhase = makeJsPhase(
    { _, module -> RemoveInlineFunctionsWithReifiedTypeParametersLowering().lower(module) },
    name = "RemoveInlineFunctionsWithReifiedTypeParametersLowering",
    description = "Remove Inline functions with reified parameters from context",
    prerequisite = setOf(FunctionInliningPhase)
)

private val ThrowableSuccessorsLoweringPhase = makeJsPhase(
    { context, module -> ThrowableSuccessorsLowering(context).lower(module) },
    name = "ThrowableSuccessorsLowering",
    description = "Link kotlin.Throwable and JavaScript Error together to provide proper interop between language and platform exceptions"
)

private val TailrecLoweringPhase = makeJsPhase(
    { context, module -> TailrecLowering(context).lower(module) },
    name = "TailrecLowering",
    description = "Replace `tailrec` callsites with equivalent loop"
)

private val UnitMaterializationLoweringPhase = makeJsPhase(
    { context, module -> UnitMaterializationLowering(context).lower(module) },
    name = "UnitMaterializationLowering",
    description = "Insert Unit object where it is supposed to be",
    prerequisite = setOf(TailrecLoweringPhase)
)

private val EnumClassLoweringPhase = makeJsPhase(
    { context, module -> EnumClassLowering(context).lower(module) },
    name = "EnumClassLowering",
    description = "Transform Enum Class into regular Class"
)

private val EnumUsageLoweringPhase = makeJsPhase(
    { context, module -> EnumUsageLowering(context).lower(module) },
    name = "EnumUsageLowering",
    description = "Replace enum access with invocation of corresponding function"
)

private val SharedVariablesLoweringPhase = makeJsPhase(
    { context, module -> SharedVariablesLowering(context).lower(module) },
    name = "SharedVariablesLowering",
    description = "Box captured mutable variables"
)

private val ReturnableBlockLoweringPhase = makeJsPhase(
    { context, module -> ReturnableBlockLowering(context).lower(module) },
    name = "ReturnableBlockLowering",
    description = "Replace returnable block with do-while loop",
    prerequisite = setOf(FunctionInliningPhase)
)

private val LocalDelegatedPropertiesLoweringPhase = makeJsPhase(
    { _, module -> LocalDelegatedPropertiesLowering().lower(module) },
    name = "LocalDelegatedPropertiesLowering",
    description = "Transform Local Delegated properties"
)

private val LocalDeclarationsLoweringPhase = makeJsPhase(
    { context, module -> LocalDeclarationsLowering(context).lower(module) },
    name = "LocalDeclarationsLowering",
    description = "Move local declarations into nearest declaration container",
    prerequisite = setOf(SharedVariablesLoweringPhase)
)

private val InnerClassesLoweringPhase = makeJsPhase(
    { context, module -> InnerClassesLowering(context).lower(module) },
    name = "InnerClassesLowering",
    description = "Capture outer this reference to inner class"
)

private val InnerClassConstructorCallsLoweringPhase = makeJsPhase(
    { context, module -> InnerClassConstructorCallsLowering(context).lower(module) },
    name = "InnerClassConstructorCallsLowering",
    description = "Replace inner class constructor invocation"
)

private val SuspendFunctionsLoweringPhase = makeJsPhase(
    { context, module -> SuspendFunctionsLowering(context).lower(module) },
    name = "SuspendFunctionsLowering",
    description = "Transform suspend functions into CoroutineImpl instance and build state machine",
    prerequisite = setOf(UnitMaterializationLoweringPhase, CoroutineIntrinsicLoweringPhase)
)

private val PrivateMembersLoweringPhase = makeJsPhase(
    { context, module -> PrivateMembersLowering(context).lower(module) },
    name = "PrivateMembersLowering",
    description = "Extract private members from classes"
)

private val CallableReferenceLoweringPhase = makeJsPhase(
    { context, module -> CallableReferenceLowering(context).lower(module) },
    name = "CallableReferenceLowering",
    description = "Handle callable references",
    prerequisite = setOf(
        SuspendFunctionsLoweringPhase,
        LocalDeclarationsLoweringPhase,
        LocalDelegatedPropertiesLoweringPhase,
        PrivateMembersLoweringPhase
    )
)

private val DefaultArgumentStubGeneratorPhase = makeJsPhase(
    { context, module -> JsDefaultArgumentStubGenerator(context).lower(module) },
    name = "DefaultArgumentStubGenerator",
    description = "Generate synthetic stubs for functions with default parameter values"
)

private val DefaultParameterInjectorPhase = makeJsPhase(
    { context, module -> DefaultParameterInjector(context).lower(module) },
    name = "DefaultParameterInjector",
    description = "Replace callsite with default parameters with corresponding stub function",
    prerequisite = setOf(CallableReferenceLoweringPhase, InnerClassesLoweringPhase)
)

private val DefaultParameterCleanerPhase = makeJsPhase(
    { context, module -> DefaultParameterCleaner(context).lower(module) },
    name = "DefaultParameterCleaner",
    description = "Clean default parameters up"
)

private val JsDefaultCallbackGeneratorPhase = makeJsPhase(
    { context, module -> JsDefaultCallbackGenerator(context).lower(module) },
    name = "JsDefaultCallbackGenerator",
    description = "Build binding for super calls with default parameters"
)

private val VarargLoweringPhase = makeJsPhase(
    { context, module -> VarargLowering(context).lower(module) },
    name = "VarargLowering",
    description = "Lower vararg arguments",
    prerequisite = setOf(CallableReferenceLoweringPhase)
)

private val PropertiesLoweringPhase = makeJsPhase(
    { _, module -> PropertiesLowering().lower(module) },
    name = "PropertiesLowering",
    description = "Move fields and accessors out from its property"
)

private val InitializersLoweringPhase = makeJsPhase(
    { context, module -> InitializersLowering(context, JsLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER, false).lower(module) },
    name = "InitializersLowering",
    description = "Merge init block and field initializers into [primary] constructor",
    prerequisite = setOf(EnumClassLoweringPhase)
)

private val MultipleCatchesLoweringPhase = makeJsPhase(
    { context, module -> MultipleCatchesLowering(context).lower(module) },
    name = "MultipleCatchesLowering",
    description = "Replace multiple catches with single one"
)

private val BridgesConstructionPhase = makeJsPhase(
    { context, module -> BridgesConstruction(context).lower(module) },
    name = "BridgesConstruction",
    description = "Generate bridges",
    prerequisite = setOf(SuspendFunctionsLoweringPhase)
)

private val TypeOperatorLoweringPhase = makeJsPhase(
    { context, module -> TypeOperatorLowering(context).lower(module) },
    name = "TypeOperatorLowering",
    description = "Lower IrTypeOperator with corresponding logic",
    prerequisite = setOf(BridgesConstructionPhase, RemoveInlineFunctionsWithReifiedTypeParametersLoweringPhase)
)

private val SecondaryConstructorLoweringPhase = makeJsPhase(
    { context, module -> SecondaryConstructorLowering(context).lower(module) },
    name = "SecondaryConstructorLoweringPhase",
    description = "Generate static functions for each secondary constructor",
    prerequisite = setOf(InnerClassesLoweringPhase)
)

private val SecondaryFactoryInjectorLoweringPhase = makeJsPhase(
    { context, module -> SecondaryFactoryInjectorLowering(context).lower(module) },
    name = "SecondaryFactoryInjectorLoweringPhase",
    description = "Replace usage of secondary constructor with corresponding static function",
    prerequisite = setOf(InnerClassesLoweringPhase)
)

private val InlineClassLoweringPhase = makeJsPhase(
    { context, module ->
        InlineClassLowering(context).run {
            inlineClassDeclarationLowering.runOnFilesPostfix(module)
            inlineClassUsageLowering.lower(module)
        }
    },
    name = "InlineClassLowering",
    description = "Handle inline classes"
)

private val AutoboxingTransformerPhase = makeJsPhase(
    { context, module -> AutoboxingTransformer(context).lower(module) },
    name = "AutoboxingTransformer",
    description = "Insert box/unbox intrinsics"
)

private val BlockDecomposerLoweringPhase = makeJsPhase(
    { context, module ->
        BlockDecomposerLowering(context).lower(module)
        module.patchDeclarationParents()
    },
    name = "BlockDecomposerLowering",
    description = "Transform statement-like-expression nodes into pure-statement to make it easily transform into JS",
    prerequisite = setOf(TypeOperatorLoweringPhase, SuspendFunctionsLoweringPhase)
)

private val ClassReferenceLoweringPhase = makeJsPhase(
    { context, module -> ClassReferenceLowering(context).lower(module) },
    name = "ClassReferenceLowering",
    description = "Handle class references"
)

private val PrimitiveCompanionLoweringPhase = makeJsPhase(
    { context, module -> PrimitiveCompanionLowering(context).lower(module) },
    name = "PrimitiveCompanionLowering",
    description = "Replace common companion object access with platform one"
)

private val ConstLoweringPhase = makeJsPhase(
    { context, module -> ConstLowering(context).lower(module) },
    name = "ConstLowering",
    description = "Wrap Long and Char constants into constructor invocation"
)

private val CallsLoweringPhase = makeJsPhase(
    { context, module -> CallsLowering(context).lower(module) },
    name = "CallsLowering",
    description = "Handle intrinsics"
)

object IrModuleEndPhase : CompilerPhase<BackendContext, IrModuleFragment> {
    override val name = "IrModuleFragment"
    override val description = "State at end of IrModuleFragment lowering"
    override val prerequisite = emptySet()
    override fun invoke(context: BackendContext, input: IrModuleFragment) = input
}

private val IrToJsPhase = makeJsPhase(
    { context, module -> context.jsProgram = IrModuleToJsTransformer(context).let { module.accept(it, null) } },
    name = "IrModuleToJsTransformer",
    description = "Generate JsAst from IrTree"
)

val jsPhases = listOf(
    IrModuleStartPhase,
    MoveBodilessDeclarationsToSeparatePlacePhase,
    ExpectDeclarationsRemovingPhase,
    CoroutineIntrinsicLoweringPhase,
    ArrayInlineConstructorLoweringPhase,
    LateinitLoweringPhase,
    ModuleCopyingPhase,
    FunctionInliningPhase,
    RemoveInlineFunctionsWithReifiedTypeParametersLoweringPhase,
    ThrowableSuccessorsLoweringPhase,
    TailrecLoweringPhase,
    UnitMaterializationLoweringPhase,
    EnumClassLoweringPhase,
    EnumUsageLoweringPhase,
    SharedVariablesLoweringPhase,
    ReturnableBlockLoweringPhase,
    LocalDelegatedPropertiesLoweringPhase,
    LocalDeclarationsLoweringPhase,
    InnerClassesLoweringPhase,
    InnerClassConstructorCallsLoweringPhase,
    SuspendFunctionsLoweringPhase,
    PrivateMembersLoweringPhase,
    CallableReferenceLoweringPhase,
    DefaultArgumentStubGeneratorPhase,
    DefaultParameterInjectorPhase,
    DefaultParameterCleanerPhase,
    JsDefaultCallbackGeneratorPhase,
    VarargLoweringPhase,
    PropertiesLoweringPhase,
    InitializersLoweringPhase,
    MultipleCatchesLoweringPhase,
    BridgesConstructionPhase,
    TypeOperatorLoweringPhase,
    SecondaryConstructorLoweringPhase,
    SecondaryFactoryInjectorLoweringPhase,
    ClassReferenceLoweringPhase,
    InlineClassLoweringPhase,
    AutoboxingTransformerPhase,
    BlockDecomposerLoweringPhase,
    PrimitiveCompanionLoweringPhase,
    ConstLoweringPhase,
    CallsLoweringPhase,
    IrModuleEndPhase,
    IrToJsPhase
)
