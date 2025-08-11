/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.StandardNames.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.getPropertySetter
import org.jetbrains.kotlin.ir.util.hasShape
import org.jetbrains.kotlin.ir.util.hasTopLevelEqualFqName
import org.jetbrains.kotlin.ir.util.isTopLevel
import org.jetbrains.kotlin.ir.util.isTopLevelInPackage
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

@OptIn(InternalSymbolFinderAPI::class)
abstract class BaseSymbolsImpl(val irBuiltIns: IrBuiltIns) {
    protected val symbolFinder = irBuiltIns.symbolFinder

    protected fun findSharedVariableBoxClass(primitiveType: PrimitiveType?): PreSerializationKlibSymbols.SharedVariableBoxClassInfo {
        val suffix = primitiveType?.typeName?.asString() ?: ""
        val classId = ClassId(StandardNames.KOTLIN_INTERNAL_FQ_NAME, Name.identifier("SharedVariableBox$suffix"))
        val boxClass = symbolFinder.findClass(classId)
            ?: error("Could not find class $classId")
        return PreSerializationKlibSymbols.SharedVariableBoxClassInfo(boxClass)
    }
}

interface PreSerializationSymbols {
    companion object {
        fun isLateinitIsInitializedPropertyGetter(symbol: IrFunctionSymbol): Boolean =
            symbol is IrSimpleFunctionSymbol && symbol.owner.let { function ->
                function.name.asString() == "<get-isInitialized>" &&
                        function.isTopLevel &&
                        function.getPackageFragment().packageFqName.asString() == "kotlin" &&
                        function.hasShape(extensionReceiver = true) &&
                        function.parameters[0].type.classOrNull?.owner?.fqNameWhenAvailable?.toUnsafe() == StandardNames.FqNames.kProperty0
            }

        fun isTypeOfIntrinsic(symbol: IrFunctionSymbol): Boolean {
            return if (symbol.isBound) {
                symbol is IrSimpleFunctionSymbol && symbol.owner.let { function ->
                    function.isTopLevelInPackage("typeOf", KOTLIN_REFLECT_FQ_NAME) && function.hasShape()
                }
            } else {
                symbol.hasTopLevelEqualFqName(KOTLIN_REFLECT_FQ_NAME.asString(), "typeOf")
            }
        }
    }
}

abstract class PreSerializationSymbolsImpl(irBuiltIns: IrBuiltIns) : PreSerializationSymbols, BaseSymbolsImpl(irBuiltIns) {

}

interface PreSerializationKlibSymbols : PreSerializationSymbols {
    class SharedVariableBoxClassInfo(val klass: IrClassSymbol) {
        val constructor by lazy { klass.constructors.single() }
        val load by lazy { klass.getPropertyGetter("element")!! }
        val store by lazy { klass.getPropertySetter("element")!! }
    }

    val genericSharedVariableBox: SharedVariableBoxClassInfo
}

@OptIn(InternalSymbolFinderAPI::class)
abstract class PreSerializationKlibSymbolsImpl(irBuiltIns: IrBuiltIns) : PreSerializationKlibSymbols, PreSerializationSymbolsImpl(irBuiltIns) {
    override val genericSharedVariableBox: PreSerializationKlibSymbols.SharedVariableBoxClassInfo = findSharedVariableBoxClass(null)
}

interface PreSerializationWebSymbols : PreSerializationKlibSymbols {

}

abstract class PreSerializationWebSymbolsImpl(irBuiltIns: IrBuiltIns) : PreSerializationWebSymbols, PreSerializationKlibSymbolsImpl(irBuiltIns) {

}

interface PreSerializationJsSymbols : PreSerializationWebSymbols {}

open class PreSerializationJsSymbolsImpl(irBuiltIns: IrBuiltIns) : PreSerializationJsSymbols, PreSerializationWebSymbolsImpl(irBuiltIns) {

}

interface PreSerializationWasmSymbols : PreSerializationWebSymbols {}

open class PreSerializationWasmSymbolsImpl(irBuiltIns: IrBuiltIns) : PreSerializationWasmSymbols, PreSerializationWebSymbolsImpl(irBuiltIns) {

}

interface PreSerializationNativeSymbols : PreSerializationKlibSymbols {}

open class PreSerializationNativeSymbolsImpl(irBuiltIns: IrBuiltIns) : PreSerializationNativeSymbols, PreSerializationKlibSymbolsImpl(irBuiltIns) {

}
