/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.ir.Ir
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.wasm.ir2wasm.JsModuleAndQualifierReference
import org.jetbrains.kotlin.backend.wasm.lower.WasmSharedVariablesManager
import org.jetbrains.kotlin.backend.wasm.utils.WasmInlineClassesUtils
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.lower.JsInnerClassesSupport
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class WasmBackendContext(
    val module: ModuleDescriptor,
    override val irBuiltIns: IrBuiltIns,
    val symbolTable: SymbolTable,
    val irModuleFragment: IrModuleFragment,
    propertyLazyInitialization: Boolean,
    override val configuration: CompilerConfiguration,
) : JsCommonBackendContext {
    override val builtIns = module.builtIns
    override val typeSystem: IrTypeSystemContext = IrTypeSystemContextImpl(irBuiltIns)
    override var inVerbosePhase: Boolean = false
    override val scriptMode = false
    override val irFactory: IrFactory = symbolTable.irFactory

    // Place to store declarations excluded from code generation
    private val excludedDeclarations = mutableMapOf<FqName, IrPackageFragment>()

    fun getExcludedPackageFragment(fqName: FqName): IrPackageFragment = excludedDeclarations.getOrPut(fqName) {
        IrExternalPackageFragmentImpl(
            DescriptorlessExternalPackageFragmentSymbol(),
            fqName
        )
    }

    override val mapping = JsMapping()

    val closureCallExports = mutableMapOf<IrSimpleType, IrSimpleFunction>()
    val kotlinClosureToJsConverters = mutableMapOf<IrSimpleType, IrSimpleFunction>()
    val jsClosureCallers = mutableMapOf<IrSimpleType, IrSimpleFunction>()
    val jsToKotlinClosures = mutableMapOf<IrSimpleType, IrSimpleFunction>()

    val jsModuleAndQualifierReferences =
        mutableSetOf<JsModuleAndQualifierReference>()

    override val coroutineSymbols =
        JsCommonCoroutineSymbols(symbolTable, module,this)

    val innerClassesSupport = JsInnerClassesSupport(mapping, irFactory)

    override val internalPackageFqn = FqName("kotlin.wasm")

    val kotlinWasmInternalPackageFqn = internalPackageFqn.child(Name.identifier("internal"))

    private val internalPackageFragmentDescriptor = EmptyPackageFragmentDescriptor(builtIns.builtInsModule, kotlinWasmInternalPackageFqn)
    // TODO: Merge with JS IR Backend context lazy file
    val internalPackageFragment by lazy {
        IrFileImpl(object : IrFileEntry {
            override val name = "<implicitDeclarations>"
            override val maxOffset = UNDEFINED_OFFSET

            override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int) =
                SourceRangeInfo(
                    "",
                    UNDEFINED_OFFSET,
                    UNDEFINED_LINE_NUMBER,
                    UNDEFINED_COLUMN_NUMBER,
                    UNDEFINED_OFFSET,
                    UNDEFINED_LINE_NUMBER,
                    UNDEFINED_COLUMN_NUMBER
                )

            override fun getLineNumber(offset: Int) = UNDEFINED_LINE_NUMBER
            override fun getColumnNumber(offset: Int) = UNDEFINED_COLUMN_NUMBER
        }, internalPackageFragmentDescriptor, irModuleFragment).also {
            irModuleFragment.files += it
        }
    }

    fun createInitFunction(identifier: String): IrSimpleFunction = irFactory.buildFun {
        name = Name.identifier(identifier)
        returnType = irBuiltIns.unitType
    }.apply {
        body = irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        internalPackageFragment.addChild(this)
    }

    val fieldInitFunction = createInitFunction("fieldInit")
    val mainCallsWrapperFunction = createInitFunction("mainCallsWrapper")

    override val sharedVariablesManager =
        WasmSharedVariablesManager(this, irBuiltIns, internalPackageFragment)

    val wasmSymbols: WasmSymbols = WasmSymbols(this@WasmBackendContext, symbolTable)
    override val reflectionSymbols: ReflectionSymbols get() = wasmSymbols.reflectionSymbols

    override val enumEntries = wasmSymbols.enumEntries
    override val createEnumEntries = wasmSymbols.createEnumEntries

    override val propertyLazyInitialization: PropertyLazyInitialization =
        PropertyLazyInitialization(enabled = propertyLazyInitialization, eagerInitialization = wasmSymbols.eagerInitialization)

    override val ir = object : Ir<WasmBackendContext>(this) {
        override val symbols: Symbols = wasmSymbols
        override fun shouldGenerateHandlerParameterForDefaultBodyFun() = true
    }

    override val inlineClassesUtils = WasmInlineClassesUtils(wasmSymbols)

    override fun log(message: () -> String) {
        /*TODO*/
        if (inVerbosePhase) print(message())
    }

    override fun report(element: IrElement?, irFile: IrFile?, message: String, isError: Boolean) {
        /*TODO*/
        print(message)
    }

    //
    // Unit test support, mostly borrowed from the JS implementation
    //

    override val suiteFun: IrSimpleFunctionSymbol?
        get() = wasmSymbols.suiteFun
    override val testFun: IrSimpleFunctionSymbol?
        get() = wasmSymbols.testFun

    private fun syntheticFile(name: String, module: IrModuleFragment): IrFile {
        return IrFileImpl(object : IrFileEntry {
            override val name = "<$name>"
            override val maxOffset = UNDEFINED_OFFSET

            override fun getSourceRangeInfo(beginOffset: Int, endOffset: Int) =
                SourceRangeInfo(
                    "",
                    UNDEFINED_OFFSET,
                    UNDEFINED_LINE_NUMBER,
                    UNDEFINED_COLUMN_NUMBER,
                    UNDEFINED_OFFSET,
                    UNDEFINED_LINE_NUMBER,
                    UNDEFINED_COLUMN_NUMBER
                )

            override fun getLineNumber(offset: Int) = UNDEFINED_LINE_NUMBER
            override fun getColumnNumber(offset: Int) = UNDEFINED_COLUMN_NUMBER
        }, internalPackageFragmentDescriptor, module).also {
            module.files += it
        }
    }

    private val testContainerFuns = mutableMapOf<IrModuleFragment, IrSimpleFunction>()

    val testEntryPoints: Collection<IrSimpleFunction>
        get() = testContainerFuns.values

    override fun createTestContainerFun(irFile: IrFile): IrSimpleFunction {
        val module = irFile.module
        return testContainerFuns.getOrPut(module) {
            val file = syntheticFile("tests", module)
            irFactory.addFunction(file) {
                name = Name.identifier("testContainer")
                returnType = irBuiltIns.unitType
                origin = JsIrBuilder.SYNTHESIZED_DECLARATION
            }.apply {
                body = irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET, emptyList())
            }
        }
    }
}
