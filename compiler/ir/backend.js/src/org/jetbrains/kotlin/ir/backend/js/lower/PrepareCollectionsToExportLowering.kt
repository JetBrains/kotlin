/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.isJsImplicitExport
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom
import org.jetbrains.kotlin.utils.memoryOptimizedPlus

private class ExportedCollectionsInfo(context: JsIrBackendContext) {
    val exportedMethodNames = setOf(
        "asJsReadonlyArrayView",
        "asJsArrayView",
        "asJsReadonlySetView",
        "asJsSetView",
        "asJsReadonlyMapView",
        "asJsMapView"
    )

    val exportableSymbols = setOf(
        context.irBuiltIns.listClass,
        context.irBuiltIns.mutableListClass,
        context.irBuiltIns.setClass,
        context.irBuiltIns.mutableSetClass,
        context.irBuiltIns.mapClass,
        context.irBuiltIns.mutableMapClass,
    )
}

// TODO: Remove the lowering and move annotations into stdlib after solving problem with tests on KLIB
class PrepareCollectionsToExportLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    private companion object {
        private val FACTORY_FOR_KOTLIN_COLLECTIONS by IrDeclarationOriginImpl.Regular
    }

    private val exportedCollectionsInfo = ExportedCollectionsInfo(context)

    private val jsStatic by lazy(LazyThreadSafetyMode.NONE) {
        context.symbols.jsStaticAnnotationSymbol.primaryConstructorSymbol
    }
    private val jsNameCtor by lazy(LazyThreadSafetyMode.NONE) {
        context.symbols.jsNameAnnotationSymbol.primaryConstructorSymbol
    }
    private val jsExportIgnoreCtor by lazy(LazyThreadSafetyMode.NONE) {
        context.symbols.jsExportIgnoreAnnotationSymbol.primaryConstructorSymbol
    }
    private val jsImplicitExportCtor by lazy(LazyThreadSafetyMode.NONE) {
        context.symbols.jsImplicitExportAnnotationSymbol.primaryConstructorSymbol
    }

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrClass && declaration.symbol in exportedCollectionsInfo.exportableSymbols) {
            declaration.addJsName()
            declaration.markWithJsImplicitExport()

            declaration.declarations.forEach {
                if (!it.shouldIncludeInInterfaceExport()) {
                    it.excludeFromJsExport()
                }
            }

            declaration.addCompanionWithJsFactoryFunction()
        }

        return null
    }

    private val typesToItsFactoryMethods = hashMapOf(
        context.irBuiltIns.listClass to FactoryMethod("fromJsArray", context.symbols.jsCreateListFrom),
        context.irBuiltIns.mutableListClass to FactoryMethod("fromJsArray", context.symbols.jsCreateMutableListFrom),
        context.irBuiltIns.setClass to FactoryMethod("fromJsSet", context.symbols.jsCreateSetFrom),
        context.irBuiltIns.mutableSetClass to FactoryMethod("fromJsSet", context.symbols.jsCreateMutableSetFrom),
        context.irBuiltIns.mapClass to FactoryMethod("fromJsMap", context.symbols.jsCreateMapFrom),
        context.irBuiltIns.mutableMapClass to FactoryMethod("fromJsMap", context.symbols.jsCreateMutableMapFrom)
    )

    private fun IrClass.addCompanionWithJsFactoryFunction() {
        val (factoryMethodName, factoryMethodForTheCollectionSymbol) =
            typesToItsFactoryMethods[symbol] ?: irError("Unexpected collection") {
                withIrEntry("this", this@addCompanionWithJsFactoryFunction)
            }

        val companionObject = context.irFactory.createClass(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = FACTORY_FOR_KOTLIN_COLLECTIONS,
            name = SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT,
            visibility = DescriptorVisibilities.PUBLIC,
            symbol = IrClassSymbolImpl(),
            kind = ClassKind.OBJECT,
            modality = Modality.FINAL,
            isCompanion = true,
        ).also { companionObject ->
            declarations.add(companionObject)

            companionObject.parent = this
            companionObject.createThisReceiverParameter()
            companionObject.thisReceiver!!.origin = FACTORY_FOR_KOTLIN_COLLECTIONS
            companionObject.excludeFromJsExport()
        }

        val factoryMethod = context.irFactory.createSimpleFunction(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            origin = FACTORY_FOR_KOTLIN_COLLECTIONS,
            name = Name.identifier(factoryMethodName),
            visibility = DescriptorVisibilities.PUBLIC,
            isInline = false,
            isExpect = false,
            returnType = null,
            modality = Modality.FINAL,
            symbol = IrSimpleFunctionSymbolImpl(),
            isTailrec = false,
            isSuspend = false,
            isOperator = false,
            isInfix = false,
            isExternal = false
        ).also {
            it.addJsStatic()
            it.parent = companionObject
            val original = factoryMethodForTheCollectionSymbol.owner
            val substitutionMap = it.copyFunctionSignatureFrom(original)
            val thisReceiver = companionObject.thisReceiver
            it.parameters =
                listOfNotNull(thisReceiver?.copyTo(it, type = thisReceiver.type.substitute(substitutionMap))) + it.nonDispatchParameters
            it.body = context.createIrBuilder(it.symbol).run {
                irBlockBody(it) {
                    +irReturn(
                        irCall(factoryMethodForTheCollectionSymbol).apply {
                            arguments.assignFrom(it.nonDispatchParameters) { parameter -> irGet(parameter) }
                        }
                    )
                }
            }
        }

        companionObject.declarations.add(factoryMethod)
        companionObject.declarations.add(
            context.irFactory.createConstructor(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = FACTORY_FOR_KOTLIN_COLLECTIONS,
                name = Name.special("<init>"),
                visibility = DescriptorVisibilities.PRIVATE,
                isInline = false,
                isExpect = false,
                returnType = companionObject.defaultType,
                IrConstructorSymbolImpl(),
                isPrimary = true
            ).also {
                it.parent = companionObject
                it.body = context.createIrBuilder(it.symbol).run {
                    irBlockBody(it) {
                        +irDelegatingConstructorCall(context.irBuiltIns.anyClass.primaryConstructorSymbol.owner)
                    }
                }
            }
        )
    }

    private fun IrDeclaration.shouldIncludeInInterfaceExport() =
        this is IrSimpleFunction && name.toString() in exportedCollectionsInfo.exportedMethodNames

    private fun IrDeclaration.excludeFromJsExport() {
        if (this is IrSimpleFunction) {
            correspondingPropertySymbol?.owner?.excludeFromJsExport()
        }
        annotations = annotations memoryOptimizedPlus JsIrBuilder.buildConstructorCall(jsExportIgnoreCtor)
    }

    private fun IrDeclarationWithName.addJsName() {
        annotations = annotations memoryOptimizedPlus JsIrBuilder.buildConstructorCall(jsNameCtor).apply {
            arguments[0] = "Kt${name.asString()}".toIrConst(context.irBuiltIns.stringType)
        }
    }

    private fun IrDeclarationWithName.addJsStatic() {
        annotations = annotations memoryOptimizedPlus JsIrBuilder.buildConstructorCall(jsStatic)
    }

    private fun IrDeclaration.markWithJsImplicitExport() {
        annotations = annotations memoryOptimizedPlus JsIrBuilder.buildConstructorCall(jsImplicitExportCtor).apply {
            arguments[0] = true.toIrConst(context.irBuiltIns.booleanType)
        }
    }

    private data class FactoryMethod(val name: String, val callee: IrSimpleFunctionSymbol)
}

/**
 * Removes `@JsImplicitExport` from unused collections if there is no strict-mode for TypeScript.
 */
class RemoveImplicitExportsFromCollections(private val context: JsIrBackendContext) : DeclarationTransformer {
    private val strictImplicitExport = context.configuration.getBoolean(JSConfigurationKeys.GENERATE_STRICT_IMPLICIT_EXPORT)
    private val jsImplicitExportCtor by lazy(LazyThreadSafetyMode.NONE) {
        context.symbols.jsImplicitExportAnnotationSymbol.primaryConstructorSymbol
    }

    private val exportedCollectionsInfo = ExportedCollectionsInfo(context)

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (
            !strictImplicitExport &&
            declaration is IrClass &&
            declaration.symbol in exportedCollectionsInfo.exportableSymbols &&
            declaration.isJsImplicitExport()
        ) {
            declaration.removeJsImplicitExport()
        }

        return null
    }

    private fun IrDeclaration.removeJsImplicitExport() {
        annotations = annotations.filter { it.symbol != jsImplicitExportCtor }
    }

}

private val IrClassSymbol.primaryConstructorSymbol: IrConstructorSymbol get() = owner.primaryConstructor!!.symbol
