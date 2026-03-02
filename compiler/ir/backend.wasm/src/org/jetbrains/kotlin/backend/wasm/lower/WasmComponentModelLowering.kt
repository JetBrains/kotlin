/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.utils.isDispatchReceiver
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.impl.IrAnnotationImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.validation.IrValidatorConfig
import org.jetbrains.kotlin.ir.validation.validateIr
import org.jetbrains.kotlin.ir.validation.withBasicChecks
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.zipIfSizesAreEqual
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max

// TODO maybe this whole thing should rather be in @WasmExport? But prob not, would be a bit weird for "normal" exports to implicitly support this
sealed interface WitType {
    val size: ULong
    val alignment: ULong

    fun flattened(): List<WitType>
}

// TODO better representation for things that are already flat? maybe an "isFullyFlat" in top-level iface?
interface WitTypeFullyFlattened : WitType {
    override fun flattened(): List<WitType> = listOf(this)
}

class WitPointer(val pointedToTy: WitType) : WitTypeFullyFlattened {
    // TODO wasm32 only for now, find canon abi spec specifics for this
    override val size: ULong = 4u
    override val alignment: ULong = 4u

    override fun flattened(): List<WitType> = listOf(this)

}

abstract class WitPrimitiveInt(
    sizeAndAlignment: ULong,
    override val size: ULong = sizeAndAlignment,
    override val alignment: ULong = sizeAndAlignment,
) : WitTypeFullyFlattened {
}

class U8 : WitPrimitiveInt(1u)
class U16 : WitPrimitiveInt(2u)
class U32 : WitPrimitiveInt(4u)
class U64 : WitPrimitiveInt(8u)

class S8 : WitPrimitiveInt(1u)
class S16 : WitPrimitiveInt(2u)
class S32 : WitPrimitiveInt(4u)
class S64 : WitPrimitiveInt(8u)

class WitList(val elemTy: WitType, val optionalFixedLength: ULong? = null) : WitType {
    override val size: ULong
        get() {
            if (optionalFixedLength == null)
                return 8u
            return elemTy.size * optionalFixedLength
        }
    // TODO think about getters vs by lazy vs functions vs eager init in init{} block vs direct init
    override val alignment: ULong by lazy{
        if (optionalFixedLength == null)
            return@lazy 4u
        return@lazy elemTy.alignment
    }

    override fun flattened(): List<WitType> = listOf(WitPointer(elemTy), U32())
}

class WitVariant(val cases: List<WitType>) : WitType {
    // TODO toInt() doesn't exist, just toUInt()???
    val discriminantType: WitType = when(ceil(log2(cases.size.toDouble())/8).toUInt()){
        0u -> U8()
        1u -> U8()
        2u -> U16()
        3u -> U32()
        else -> error("PANIC") //TODO proper
    }

    /*
   def elem_size_variant(cases):
  s = elem_size(discriminant_type(cases))
  s = align_to(s, max_case_alignment(cases))
  cs = 0
  for c in cases:
    if c.t is not None:
      cs = max(cs, elem_size(c.t))
  s += cs
  return align_to(s, alignment_variant(cases))
     */
    override val size: ULong = run {
        /*
        TODO this is pretty wrong so far
        val discriminantSize = this@WitVariant.discriminantType.size
        val discrimantAlign = this@WitVariant.discriminantType.alignment

        var maxCaseSize = 0uL
        for (case in cases) {
            maxCaseSize = max(maxCaseSize, case.size)
        }

        // TODO alignTo
        return@run alignTo(discriminantSize + maxCaseSize, discrimantAlign)
         */
        0uL
    }
    override val alignment: ULong = 4u
    override fun flattened(): List<WitType> = TODO("Not yet implemented")

}

class WasmComponentModelLowering(val context: WasmBackendContext) : FileLoweringPass {
    // TODO with all of the names in this file, make them so that there can't be any conflicts

    override fun lower(irFile: IrFile) {
        val toAddToTopLevel = mutableListOf<IrFunction>()
        val toRemoveTopLevel: MutableList<IrClass> = mutableListOf()
        for (decl in irFile.declarations) {


            // The problem is the body of the <init> function of the companion object would be null, because the surrounding interface is external.
            // Theres a special change in ClassMemberGenerator.kt in fir2ir, that makes sure that external interfaces that are
            // annotated with @WitInterface/their companion objects with @WitImport are actually treated as non-external for the purposes of generating the <init> body


            // TODO frontend checks for the annotation that doesn't match this, i.e. on a non-interface, or a non-external interface
            if (decl is IrClass && decl.isInterface && decl.isExternal && decl.hasAnnotation(context.wasmSymbols.witInterface)) {

                val witIfaceAnnotation = decl.annotations.findAnnotation(context.wasmSymbols.witInterface.owner.kotlinFqName)!!
                val witIfaceName = (witIfaceAnnotation.arguments[0]!! as IrConst).value.toString()

                // check to see if there is an import companion object
                val companionObject = decl.declarations.mapNotNull { it as? IrClass }
                    .find { it.isCompanion && it.hasAnnotation(context.wasmSymbols.witImport) }
                val isImported = companionObject != null

                // basically, this lowering turns the external interfaces into normal ones
                decl.isExternal = false

                if (isImported) {
                    companionObject.isExternal = false

                    // insert function implementations into companion object
                    for (functionDecl in decl.declarations.mapNotNull { it as? IrFunction }.filterNot { it.isFakeOverride }) {
                        // add top level function
                        /*
                        val topLevelFunction: IrSimpleFunction = functionDecl.deepCopyWithSymbols(irFile) as IrSimpleFunction
                        topLevelFunction.isExternal = true
                        topLevelFunction.parent = irFile
//                        (topLevelFunction.parameters as MutableList).removeIf { it.isDispatchReceiver }
                        topLevelFunction.parameters = topLevelFunction.parameters.filter { !it.isDispatchReceiver }
                        topLevelFunction.overriddenSymbols = emptyList()
                        topLevelFunction.modality = Modality.FINAL
                         */
//                        topLevelFunction.copyFunctionSignatureFrom(functionDecl)

                        val topLevelFunction = declareWitAdapterTopLevelFunction(functionDecl, irFile, witIfaceName, true)

                        toAddToTopLevel += topLevelFunction


                        // add impl function that does abi translation and calls top level function
//                        val implFunction = functionDecl.deepCopyWithSymbols(companionObject) as IrSimpleFunction
//                        implFunction.isExternal = false

                        // ACTUALLY: instead we can just use the fake override that's already there and implement it, right?
                        val implFunction = companionObject.declarations.mapNotNull { it as? IrSimpleFunction }
                            .find { it.isFakeOverride && it.name == functionDecl.name }!! // TODO think about a better check for this than comparing names

                        companionObject.factory.stageController.restrictTo(implFunction) {
                            // TODO actual offsets? rewrite original functions offsets?
                            val irb = context.createIrBuilder(implFunction.symbol)
                            implFunction.body = irb.irBlockBody {
                                +irReturn(
                                    irCall(topLevelFunction.symbol).apply {
                                        // TODO find nice replacement
                                        insertDispatchReceiver(irGet(implFunction.dispatchReceiverParameter!!))
                                        // Forward all value parameters from implFunction to topLevelFunction
                                        implFunction.parameters.forEachIndexed { index, param ->
                                            arguments[index] = irGet(param)
                                        }
                                    }
                                )
                            }
                            // -> obviously not a fake override/external anymore
                            implFunction.isFakeOverride = false
                            implFunction.isExternal = false
                            implFunction.modality = Modality.FINAL
                            // TODO this is wrong, find/introduce a better one
                            implFunction.origin = IrDeclarationOrigin.DEFINED

//                        companionObject.declarations += implFunction

//                        println(implFunction.dump())
//                            print(companionObject.dump())
                        }
                    }
                }

                // TODO obviously allow this to be in a different file
                val correspondingExportImpl =
                    irFile.declarations.mapNotNull { it as? IrClass }.singleOrNull { it.hasAnnotation(context.wasmSymbols.witExport) }
                // TODO this is obviously also a bit strange. I wouldn't mind an @RequiresWitExport annotation on the @WitInterface
                val isExport = correspondingExportImpl != null

                if (isExport) {
                    // TODO for now, we don't allow anything in the export impl, as it's not really an object in the end
                    //      -> if anything is in here that's not a function, error
                    if (correspondingExportImpl.declarations.any { it !is IrFunction })
                        error("TODO proper error handling")
                    // TODO also need to check that every function doesn't use its dispatch parameter

                    for (implFunction in correspondingExportImpl.declarations
                        .mapNotNull { it as? IrFunction }
                        .filterNot { it.isFakeOverride }
                        .filterNot { it is IrConstructor }) {
                        // verify that the function does not use the dispatch parameter
                        checkDispatchReceiverNotUsed(implFunction)

                        // we need 2 top level functions in this case, one that is just an exact copy of the implementation
                        // TODO new try: just edit the impl function to make it a top level function.
                        //      this works, but is a bit ugly, and just somehow makes the incremental compilation happy, not sure why, fix that
//                        val topLevelImplFunction =
//                            declareCorrespondingTopLevelFunction(implFunction, irFile, implFunction.name.asString() + "toplevel_impl")
                        val topLevelImplFunction = implFunction as IrSimpleFunction
                        topLevelImplFunction.parameters = topLevelImplFunction.parameters.filter { !it.isDispatchReceiver }
                        topLevelImplFunction.visibility = DescriptorVisibilities.PUBLIC
                        topLevelImplFunction.modality = Modality.FINAL
                        topLevelImplFunction.overriddenSymbols = emptyList()
                        topLevelImplFunction.parent = irFile

                        // create top level function again, this time it needs to call the user-defined impl function
                        val topLevelExportFunction = declareWitAdapterTopLevelFunction(implFunction, irFile, witIfaceName, false)
                        // doesn't have a body yet, so create it, and call the impl function
                        topLevelExportFunction.body = context.createIrBuilder(topLevelExportFunction.symbol).irBlockBody {
                            // TODO deduplicate with the import case if possible
                            +irReturn(
                                irCall(topLevelImplFunction.symbol).apply {
                                    // TODO check that this actually makes sense
                                    topLevelExportFunction.parameters.forEach { param ->
                                        arguments.add(irGet(param))
                                    }
                                }
                            )
                        }

                        toAddToTopLevel += topLevelExportFunction
                        toAddToTopLevel += topLevelImplFunction
                    }

                    // basically delete the export impl object
                    correspondingExportImpl.declarations.clear()
                    // TODO need to correctly remove everything, also the export impl itself, this solution doesnt really work in the real world
                    assert(correspondingExportImpl.parent == irFile) { "this needs to be changed" }
                    toRemoveTopLevel += correspondingExportImpl
                }
            }
        }

        irFile.declarations.addAll(toAddToTopLevel)
        irFile.declarations.removeAll(toRemoveTopLevel)

        validateIr(
            irFile,
            context.irBuiltIns,
            IrValidatorConfig(true, true).withBasicChecks(),
            context.messageCollector,
            IrVerificationMode.ERROR
        )
    }

    // TODO reorganize these top level function helpers

    // TODO for now, this doesnt fill the body of the function, think about whether to keep it that way
    private fun declareWitAdapterTopLevelFunction(
        functionBlueprint: IrFunction,
        irFile: IrFile,
        witIfaceName: String,
        isImport: Boolean,
    ): IrSimpleFunction {
        // TODO this needs to do abi type translation in the parameter and return types obviously
        val topLevelFunction = declareCorrespondingTopLevelFunction(
            functionBlueprint,
            irFile,
            // TODO decide on final name; edit name to remove possible conflicts, maybe use some special characters like <>
            functionBlueprint.name.asString() + if (isImport) "_imported" else "_exported"
        )

        topLevelFunction.isExternal = isImport // for imports, its external, for exports, its defined

        val annotationSymbolToApply = if (isImport) context.wasmSymbols.wasmImport else context.wasmSymbols.wasmExport

        topLevelFunction.annotations += IrAnnotationImpl.fromSymbolOwner(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            // TODO single() ?
            annotationSymbolToApply.constructors.single().owner.returnType, // TODO what about this following thing instead?
//                            context.wasmSymbols.witImport.owner.defaultType,
            annotationSymbolToApply.constructors.single(),
            // TODO find right thing here
            0
        ).apply {
            if (isImport) {
                arguments[0] = IrConstImpl.string(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.stringType, witIfaceName
                )
                arguments[1] = IrConstImpl.string(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    context.irBuiltIns.stringType,
                    // TODO more robust version of this
                    // TODO research if this is actually right, or if the naming convention is more nuanced
                    kebabCaseFromLowerCamelCase(functionBlueprint.name.asString())
                )
            } else { //is export
                // TODO reduce code dup
                arguments[0] = IrConstImpl.string(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    context.irBuiltIns.stringType,
                    // TODO more robust version of this
                    wasmExportNameForWitIfaceFunction(witIfaceName, functionBlueprint.name.asString())
                )
            }
        }
        return topLevelFunction
    }

    private fun declareCorrespondingTopLevelFunction(
        functionBlueprint: IrFunction,
        irFile: IrFile,
        newName: String,
    ): IrSimpleFunction {
        // TODO the restrictTo is unknown/not validated inc comp black magic to make the IC find the signature. Should kinda rather be irFile, but that's not a declaration
        // TODO would be nicer if I could use the non-copy version in the end, I think
//        val topLevelFunction = context.irFactory.stageController.restrictTo(functionBlueprint) {
//            context.irFactory.buildFun {
//                name = Name.identifier(newName)
//                returnType = functionBlueprint.returnType
//                visibility = DescriptorVisibilities.PUBLIC
//                modality = Modality.FINAL
//                // origin = IrDeclarationOrigin.DEFINED // optional, but good practice
//            }
//        }
        val topLevelFunction = context.irFactory.stageController.restrictTo(functionBlueprint) {
            functionBlueprint.deepCopyWithSymbols(irFile) as IrSimpleFunction
        }
        topLevelFunction.name = Name.identifier(newName)
        topLevelFunction.visibility = DescriptorVisibilities.PUBLIC
        topLevelFunction.modality = Modality.FINAL
        // only needed for deepcopy case:
        topLevelFunction.overriddenSymbols = emptyList()

        topLevelFunction.parent = irFile
        // only needed for non-deepcopy case:
//        topLevelFunction.copyFunctionSignatureFrom(functionBlueprint)
        // TODO correctly handle non-this dispatch receivers
        // TODO decide on .remove vs this
        topLevelFunction.parameters = topLevelFunction.parameters.filter { !it.isDispatchReceiver }
        return topLevelFunction
    }

    // TODO this is just a bytecodealliance "convention" that "just works" with the existing tooling right now
    //      see https://github.com/WebAssembly/component-model/issues/422 / https://github.com/WebAssembly/component-model/pull/378
    private fun wasmExportNameForWitIfaceFunction(witIfaceName: String, functionName: String): String {
        return "$witIfaceName#$functionName"
    }

    // TODO probably find a more robust alternative, maybe pass the fn names from wit-bindgen to here somehow
    private fun kebabCaseFromLowerCamelCase(lowerCamelCase: String): String {
        assert(lowerCamelCase[0].isLowerCase()) { "using this function wrong, probably shouldn't be using it at all" }

        return buildString {
            for (c in lowerCamelCase) {
                if (c.isUpperCase()) {
                    append('-')
                    append(c.lowercaseChar())
                } else {
                    append(c)
                }
            }
        }
    }

    // TODO this needs to be a frontend check later
    private fun checkDispatchReceiverNotUsed(function: IrFunction) {
        assert(function.dispatchReceiverParameter != null) { "function must have a dispatch receiver" }

        // TODO verify that this is correct
        function.body?.acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement, data: Nothing?) {
                element.acceptChildrenVoid(this)
            }

            override fun visitGetValue(expression: IrGetValue, data: Nothing?) {
                if (expression.symbol == function.dispatchReceiverParameter!!.symbol) {
                    error("@WitExport implementation function '${function.name}' must not use 'this' dispatch receiver, as the Kotlin `object` is only conceptual and will be compiled out")
                }
                super.visitGetValue(expression, data)
            }
        })
    }
}
