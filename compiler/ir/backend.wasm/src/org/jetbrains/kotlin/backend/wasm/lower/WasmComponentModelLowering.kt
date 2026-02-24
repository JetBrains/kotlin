/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.utils.isDispatchReceiver
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.impl.IrAnnotationImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyFunctionSignatureFrom
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.util.parentsWithSelf
import org.jetbrains.kotlin.ir.util.setDeclarationsParent
import org.jetbrains.kotlin.ir.validation.IrValidatorConfig
import org.jetbrains.kotlin.ir.validation.validateIr
import org.jetbrains.kotlin.ir.validation.withBasicChecks
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class WasmComponentModelLowering(val context: WasmBackendContext) : FileLoweringPass {
//    override fun lower(irBody: IrBody, container: IrDeclaration) {
//        // wanna find functions inside `@WitInterface external interface`s
//        if (container is IrClass && container.kind == ClassKind.INTERFACE && container.hasAnnotation(context.wasmSymbols.witInterface)) {
//            println("hiiii")
//        }
//    }

    // TODO with all of the names in this file, make them so that there can't be any conflicts

    override fun lower(irFile: IrFile) {
        val toAddToTopLevel = mutableListOf<IrFunction>()
        val toRemoveTopLevel: MutableList<IrClass> = mutableListOf()
        for (decl in irFile.declarations) {
            // TODO frontend checks for the annotation that doesn't match this, i.e. on a non-interface, or a non-external interface
            if (decl is IrClass && decl.isInterface && decl.isExternal && decl.hasAnnotation(context.wasmSymbols.witInterface)) {

                val witIfaceAnnotation = decl.annotations.findAnnotation(context.wasmSymbols.witInterface.owner.kotlinFqName)!!
                val witIfaceName = (witIfaceAnnotation.arguments[0]!! as IrConst).value.toString()

                // check to see if there is an import companion object
                val companionObject = decl.declarations.mapNotNull { it as? IrClass }
                    .find { it.isCompanion && it.hasAnnotation(context.wasmSymbols.witImport) }
                val isImported = companionObject != null

                // TODO do this properly, probably just all the time, once the imports have their definitions
//                if (!isImported)
                decl.isExternal = false

                if (isImported) {
                    // insert function implementations into companion object
                    for (functionDecl in decl.declarations.mapNotNull { it as? IrFunction }.filterNot { it.isFakeOverride }) {
                        // add top level function
                        /*
                        val topLevelFunction: IrSimpleFunction = functionDecl.deepCopyWithSymbols(irFile) as IrSimpleFunction
                        topLevelFunction.isExternal = true
                        topLevelFunction.parent = irFile
                        // TODO correctly handle non-this dispatch receivers
                        // TODO none of these methods work at all, not even for the this dispatch receiver
//                        (topLevelFunction.parameters as MutableList).removeIf { it.isDispatchReceiver }
                        topLevelFunction.parameters = topLevelFunction.parameters.filter { !it.isDispatchReceiver }
                        topLevelFunction.overriddenSymbols = emptyList()
                        topLevelFunction.modality = Modality.FINAL
                         */
                        // TODO edit name to remove possible conflicts
                        // TODO signature problems
//                        topLevelFunction.copyFunctionSignatureFrom(functionDecl)

                        // TODO the restrictTo is unknown/not validated inc comp black magic to make the IC find the signature. Should kinda rather be irFile, but that's not a declaration
                        val topLevelFunction = declareWitAdapterTopLevelFunction(functionDecl, irFile, witIfaceName, true)

                        toAddToTopLevel += topLevelFunction


                        // add impl function that does abi translation and calls top level function
                        val implFunction = functionDecl.deepCopyWithSymbols(companionObject)
                        implFunction.isExternal = false
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

                        println(implFunction.dump())
                    }
                }

                // TODO obviously allow this to be in a different file
                val correspondingExportImpl =
                    irFile.declarations.mapNotNull { it as? IrClass }.singleOrNull { it.hasAnnotation(context.wasmSymbols.witExport) }
                // TODO this is obviously also a bit strange. I wouldn't mind a an @RequiresWitExport annotation on the @WitInterface
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
                        // TODO new try: just edit the impl function to make it a top level function
//                        val topLevelImplFunction =
//                            declareCorrespondingTopLevelFunction(implFunction, irFile, implFunction.name.asString() + "toplevel_impl")
                        val topLevelImplFunction = implFunction as IrSimpleFunction
                        topLevelImplFunction.parameters = topLevelImplFunction.parameters.filter { !it.isDispatchReceiver }
                        topLevelImplFunction.visibility = DescriptorVisibilities.PUBLIC
                        topLevelImplFunction.modality = Modality.FINAL
                        topLevelImplFunction.overriddenSymbols = emptyList()
                        topLevelImplFunction.parent = irFile

                        // TODO this is just a stub
//                        topLevelImplFunction.body = context.createIrBuilder(topLevelImplFunction.symbol).irBlockBody {
//
//                        }
                        // TODO enough?
//                        topLevelImplFunction.body = implFunction.body?.deepCopyWithSymbols(topLevelImplFunction)

                        // create top level function again, this time it needs to call the user-defined impl function
                        val topLevelExportFunction = declareWitAdapterTopLevelFunction(implFunction, irFile, witIfaceName, false)
                        // doesn't have a body yet, so create it, and call the impl function
                        // TODO remove, this is just for the cli export
                        topLevelExportFunction.returnType = context.irBuiltIns.intType
                        topLevelExportFunction.body = context.createIrBuilder(topLevelExportFunction.symbol).irBlockBody {
                            // TODO deduplicate with the import case if possible
                            /*
                            +irReturn(
                                irCall(topLevelImplFunction.symbol).apply {
                                    // TODO check that this actually makes sense
                                    topLevelExportFunction.parameters.forEach { param ->
                                        arguments.add(irGet(param))
                                    }
                                }
                            )
                             */
                            +irCall(topLevelImplFunction.symbol).apply {
                                // TODO check that this actually makes sense
                                topLevelExportFunction.parameters.forEach { param ->
                                    arguments.add(irGet(param))
                                }
                            }
                            +irReturn(IrConstImpl.int(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.intType, 0))
                        }

                        toAddToTopLevel += topLevelExportFunction
                        toAddToTopLevel += topLevelImplFunction
                    }

                    // basically delete the export impl object
                    correspondingExportImpl.declarations.clear()
                    // TODO need to correctly remove everything, also the export impl itself
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

    // TODO for now, this doesnt fill the body of the function
    private fun declareWitAdapterTopLevelFunction(
        functionBlueprint: IrFunction,
        irFile: IrFile,
        witIfaceName: String,
        isImport: Boolean,
    ): IrSimpleFunction {
        // TODO this needs to do type translation in the parameter and return types obviously
        val topLevelFunction = declareCorrespondingTopLevelFunction(
            functionBlueprint,
            irFile,
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
                    kebabCaseFromLowerCamelCase(functionBlueprint.name.asString())
                )
            } else { //is export
                // TODO reduce code dup
                arguments[0] = IrConstImpl.string(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    context.irBuiltIns.stringType,
                    // TODO more robust version of this
//                    kebabCaseFromLowerCamelCase(functionBlueprint.name.asString())
                    // TODO obviously do this correctl
                    "wasi:cli/run@0.2.9#run"
//                    "cm32p2|_ex_wasi:cli/run@0.2.9|run"
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
        topLevelFunction.overriddenSymbols = emptyList()

        topLevelFunction.parent = irFile
//        topLevelFunction.copyFunctionSignatureFrom(functionBlueprint)
        topLevelFunction.parameters = topLevelFunction.parameters.filter { !it.isDispatchReceiver }
        return topLevelFunction
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
                    error("@WitExport implementation function '${function.name}' must not use 'this' dispatch receiver")
                }
                super.visitGetValue(expression, data)
            }
        })
    }
}
