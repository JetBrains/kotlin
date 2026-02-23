/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.utils.isDispatchReceiver
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
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
import org.jetbrains.kotlin.name.Name

class WasmComponentModelLowering(val context: WasmBackendContext) : FileLoweringPass {
//    override fun lower(irBody: IrBody, container: IrDeclaration) {
//        // wanna find functions inside `@WitInterface external interface`s
//        if (container is IrClass && container.kind == ClassKind.INTERFACE && container.hasAnnotation(context.wasmSymbols.witInterface)) {
//            println("hiiii")
//        }
//    }

    override fun lower(irFile: IrFile) {
        var toAddToTopLevel = mutableListOf<IrFunction>()
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
                        val topLevelFunction = declareWitInteractionTopLevelFunction(functionDecl, irFile, witIfaceName, true)

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
                    for (implFunction in correspondingExportImpl.declarations.mapNotNull { it as? IrFunction }
                        .filterNot { it.isFakeOverride }) {
                        // create top level function again, this time it needs to call the user-defined impl function
                        val topLevelFunction = declareWitInteractionTopLevelFunction(implFunction, irFile, witIfaceName)
                        // doesn't have a body yet, so create it, and call the impl function
                        topLevelFunction.body = context.createIrBuilder(topLevelFunction.symbol).irBlockBody {
                            // TODO deduplicate with the import case
                            +irReturn(
                                irCall(implFunction.symbol).apply {
                                    insertDispatchReceiver(irGet(topLevelFunction.dispatchReceiverParameter!!))
                                    topLevelFunction.parameters.forEach { param ->
                                        arguments.add(irGet(param))
                                    }
                                }
                            )
                        }

                        toAddToTopLevel += topLevelFunction
                    }
                }
            }
        }

        irFile.declarations.addAll(toAddToTopLevel)
    }

    // TODO for now, this doesnt fill the body of the function
    private fun declareWitInteractionTopLevelFunction(
        functionBlueprint: IrFunction,
        irFile: IrFile,
        witIfaceName: String,
        isImport: Boolean = false,
    ): IrSimpleFunction {
        // TODO this needs to do type translation in the parameter and return types obviously
        val topLevelFunction = context.irFactory.stageController.restrictTo(functionBlueprint) {
            context.irFactory.buildFun {
                name = Name.identifier(functionBlueprint.name.asString() + if (isImport) "_imported" else "_exported")
                returnType = functionBlueprint.returnType
                visibility = DescriptorVisibilities.PUBLIC
                modality = Modality.FINAL
                isExternal = isImport // for imports, its external, for exports, its defined
                // origin = IrDeclarationOrigin.DEFINED // optional, but good practice
            }
        }

        topLevelFunction.parent = irFile
        topLevelFunction.copyFunctionSignatureFrom(functionBlueprint)
        topLevelFunction.parameters = topLevelFunction.parameters.filter { !it.isDispatchReceiver }

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
            arguments[0] = IrConstImpl.string(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.stringType, witIfaceName
            )
            arguments[1] = IrConstImpl.string(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                context.irBuiltIns.stringType,
                // TODO more robust version of this
                kebabCaseFromLowerCamelCase(functionBlueprint.name.asString())
            )
        }
        return topLevelFunction
    }

    // TODO probably find a more robust alternative, maybe pass the fn names from wit-bindgen to here somehow
    private fun kebabCaseFromLowerCamelCase(lowerCamelCase: String): String {
        assert(lowerCamelCase.get(0).isLowerCase()) { "using this function wrong, probably shouldn't be using it at all" }

        val sb = StringBuilder()

        for (c in lowerCamelCase) {
            if (c.isUpperCase()) {
                sb.append('-')
                sb.append(c.lowercaseChar())
            } else {
                sb.append(c)
            }
        }

        return sb.toString()
    }
}