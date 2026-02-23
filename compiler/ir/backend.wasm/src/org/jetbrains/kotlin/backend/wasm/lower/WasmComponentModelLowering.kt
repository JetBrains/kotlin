/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.utils.isDispatchReceiver
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.impl.IrAnnotationImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyFunctionSignatureFrom
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.load.java.descriptors.copyValueParameters
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
                    for (function in decl.declarations.mapNotNull { it as? IrFunction }.filterNot { it.isFakeOverride }) {
                        // add top level function
                        /*
                        val topLevelFunction: IrSimpleFunction = function.deepCopyWithSymbols(irFile) as IrSimpleFunction
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
//                        topLevelFunction.copyFunctionSignatureFrom(function)

                        // TODO the restrictTo is unknown/not validated inc comp black magic to make the IC find the signature. Should kinda rather be irFile, but that's not a declaration
                        val topLevelFunction = context.irFactory.stageController.restrictTo(function) {
                            context.irFactory.buildFun {
                                name = Name.identifier(function.name.asString() + "_imported")
                                returnType = function.returnType
                                visibility = DescriptorVisibilities.PUBLIC
                                modality = Modality.FINAL
                                isExternal = true
                                // origin = IrDeclarationOrigin.DEFINED // optional, but good practice
                            }
                        }

                        topLevelFunction.parent = irFile
                        topLevelFunction.copyFunctionSignatureFrom(function)
                        topLevelFunction.parameters = topLevelFunction.parameters.filter { !it.isDispatchReceiver }

                        topLevelFunction.annotations += IrAnnotationImpl.fromSymbolOwner(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                            // TODO single() ?
                            context.symbols.wasmImport.constructors.single().owner.returnType,
                            context.symbols.wasmImport.constructors.single(),
                            // TODO find right thing here
                            0
                        ).apply {
                            arguments[0] = IrConstImpl.string(
                                UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.stringType, witIfaceName
                            )
                            arguments[1] = IrConstImpl.string(
                                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                context.irBuiltIns.stringType,
                                function.name.asString() // TODO prob needs to be kebab case'd
                            )
                        }

                        toAddToTopLevel += topLevelFunction


                        // add impl function that does abi translation and calls top level function
                        val implFunction = function.deepCopyWithSymbols(companionObject)
                        implFunction.isExternal = false
                        // TODO rewrite offsets?
                        val irb = context.createIrBuilder(
                            implFunction.symbol,
                            implFunction.startOffset,
                            implFunction.endOffset
                        )
                        implFunction.body = irb.irBlockBody {
                            // TODO abi transl
                        }

                        // TODO add to companion object
//                        companionObject.addFunction("abc", function.name, function.)
                    }
                }

                // TODO exports
            }
        }

        irFile.declarations.addAll(toAddToTopLevel)
    }
}