/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

class JsIntrinsics(private val irBuiltIns: IrBuiltIns, val context: JsIrBackendContext) {

    private val externalPackageFragmentSymbol = IrExternalPackageFragmentSymbolImpl(context.internalPackageFragmentDescriptor)
    private val externalPackageFragment = IrExternalPackageFragmentImpl(externalPackageFragmentSymbol)

    // Equality operations:

    val jsEqeq = binOpBool("jsEqeq")
    val jsNotEq = binOpBool("jsNotEq")
    val jsEqeqeq = binOpBool("jsEqeqeq")
    val jsNotEqeq = binOpBool("jsNotEqeq")

    val jsGt = binOpBool("jsGt")
    val jsGtEq = binOpBool("jsGtEq")
    val jsLt = binOpBool("jsLt")
    val jsLtEq = binOpBool("jsLtEq")


    // Unary operations:

    val jsNot = unOpBool("jsNot")

    val jsUnaryPlus = unOp("jsUnaryPlus")
    val jsUnaryMinus = unOp("jsUnaryMinus")

    val jsPrefixInc = unOp("jsPrefixInc")
    val jsPostfixInc = unOp("jsPostfixInc")
    val jsPrefixDec = unOp("jsPrefixDec")
    val jsPostfixDec = unOp("jsPostfixDec")

    // Binary operations:

    val jsPlus = binOp("jsPlus")
    val jsMinus = binOp("jsMinus")
    val jsMult = binOp("jsMult")
    val jsDiv = binOp("jsDiv")
    val jsMod = binOp("jsMod")

    val jsPlusAssign = binOp("jsPlusAssign")
    val jsMinusAssign = binOp("jsMinusAssign")
    val jsMultAssign = binOp("jsMultAssign")
    val jsDivAssign = binOp("jsDivAssign")
    val jsModAssign = binOp("jsModAssign")

    val jsAnd = binOp("jsAnd")
    val jsOr = binOp("jsOr")


    // Bit operations:

    val jsBitAnd = binOpInt("jsBitAnd")
    val jsBitOr = binOpInt("jsBitOr")
    val jsBitXor = binOpInt("jsBitXor")
    val jsBitNot = unOpInt("jsBitNot")

    val jsBitShiftR = binOpInt("jsBitShiftR")
    val jsBitShiftRU = binOpInt("jsBitShiftRU")
    val jsBitShiftL = binOpInt("jsBitShiftL")


    // KFunction operations:

    val jsName = unOp("kCallableName", irBuiltIns.string)
    val jsPropertyGet = binOp("kPropertyGet")
    val jsPropertySet = tripleOp("kPropertySet", irBuiltIns.unit)


    // Type checks:

    val jsInstanceOf = binOpBool("jsInstanceOf")
    val jsTypeOf = unOp("jsTypeOf", irBuiltIns.string)

    // Number conversions:

    val jsNumberToByte = getInternalFunction("numberToByte")
    val jsNumberToDouble = getInternalFunction("numberToDouble")
    val jsNumberToInt = getInternalFunction("numberToInt")
    val jsNumberToShort = getInternalFunction("numberToShort")
    val jsNumberToLong = getInternalFunction("numberToLong")
    val jsNumberToChar = getInternalFunction("numberToChar")
    val jsToByte = getInternalFunction("toByte")
    val jsToShort = getInternalFunction("toShort")
    val jsToLong = getInternalFunction("toLong")


    // RTTI:

    val isInterfaceSymbol = getInternalFunction("isInterface")
    val isArraySymbol = getInternalFunction("isArray")
    //    val isCharSymbol = getInternalFunction("isChar")
    val isObjectSymbol = getInternalFunction("isObject")

    val isNumberSymbol = getInternalFunction("isNumber")
    val isComparableSymbol = getInternalFunction("isComparable")

    val isPrimitiveArray = mapOf(
        PrimitiveType.BOOLEAN to getInternalFunction("isBooleanArray"),
        PrimitiveType.BYTE to getInternalFunction("isByteArray"),
        PrimitiveType.SHORT to getInternalFunction("isShortArray"),
        PrimitiveType.CHAR to getInternalFunction("isCharArray"),
        PrimitiveType.INT to getInternalFunction("isIntArray"),
        PrimitiveType.FLOAT to getInternalFunction("isFloatArray"),
        PrimitiveType.LONG to getInternalFunction("isLongArray"),
        PrimitiveType.DOUBLE to getInternalFunction("isLongArray")
    )


    // Enum

    val enumValueOfIntrinsic = getInternalFunction("enumValueOfIntrinsic")
    val enumValuesIntrinsic = getInternalFunction("enumValuesIntrinsic")


    // Other:

    val jsObjectCreate = defineObjectCreateIntrinsic() // Object.create
    val jsGetJSField = defineGetJSPropertyIntrinsic() // till we don't have dynamic type we use intrinsic which sets a field with any name
    val jsSetJSField = defineSetJSPropertyIntrinsic() // till we don't have dynamic type we use intrinsic which sets a field with any name
    val jsCode = getInternalFunction("js") // js("<code>")
    val jsHashCode = getInternalFunction("hashCode")
    val jsGetObjectHashCode = getInternalFunction("getObjectHashCode")
    val jsToString = getInternalFunction("toString")
    val jsAnyToString = getInternalFunction("anyToString")
    val jsCompareTo = getInternalFunction("compareTo")
    val jsEquals = getInternalFunction("equals")

    val jsImul = getInternalFunction("imul")

    // Coroutines

    val jsCoroutineContext = context.symbolTable.referenceSimpleFunction(context.coroutineContextProperty.getter!!)

    val jsGetContinuation = context.run {
        val f = getInternalFunctions("getContinuation")
        symbolTable.referenceSimpleFunction(f.single())
    }
    val jsGetKClass = getInternalWithoutPackage("getKClass")
    val jsGetKClassFromExpression = getInternalWithoutPackage("getKClassFromExpression")
    val jsClass = getInternalFunction("jsClass")

    val jsNumberRangeToNumber = getInternalFunction("numberRangeToNumber")
    val jsNumberRangeToLong = getInternalFunction("numberRangeToLong")

    val longClassSymbol = getInternalClassWithoutPackage("kotlin.Long")

    val longToDouble = context.symbolTable.referenceSimpleFunction(
        context.getClass(FqName("kotlin.Long")).unsubstitutedMemberScope.findSingleFunction(
            Name.identifier("toDouble")
        )
    )
    val longToFloat = context.symbolTable.referenceSimpleFunction(
        context.getClass(FqName("kotlin.Long")).unsubstitutedMemberScope.findSingleFunction(
            Name.identifier("toFloat")
        )
    )

    val charClassSymbol = getInternalClassWithoutPackage("kotlin.Char")
    val charConstructor = charClassSymbol.constructors.single().owner

    val uByteClassSymbol = getInternalClassWithoutPackage("kotlin.UByte")
    val uShortClassSymbol = getInternalClassWithoutPackage("kotlin.UShort")
    val uIntClassSymbol = getInternalClassWithoutPackage("kotlin.UInt")
    val uLongClassSymbol = getInternalClassWithoutPackage("kotlin.ULong")

    val unreachable = defineUnreachableIntrinsic()

    val returnIfSuspended = getInternalFunction("returnIfSuspended")
    val getContinuation = getInternalFunction("getContinuation")

    // Arrays:
    val array = context.symbolTable.referenceClass(irBuiltIns.builtIns.array)

    val primitiveArrays = PrimitiveType.values().associate { context.symbolTable.referenceClass(irBuiltIns.builtIns.getPrimitiveArrayClassDescriptor(it)) to it }

    val jsArray = getInternalFunction("arrayWithFun")
    val jsFillArray = getInternalFunction("fillArrayFun")

    val jsArrayLength = unOp("jsArrayLength")
    val jsArrayGet = binOp("jsArrayGet")
    val jsArraySet = tripleOp("jsArraySet")

    val jsArrayIteratorFunction = getInternalFunction("arrayIterator")

    val jsPrimitiveArrayIteratorFunctions =
        PrimitiveType.values().associate { it to getInternalFunction("${it.typeName.asString().toLowerCase()}ArrayIterator") }

    val arrayLiteral = unOp("arrayLiteral").symbol

    val primitiveToTypedArrayMap = EnumMap(mapOf(
        PrimitiveType.BYTE to "Int8",
        PrimitiveType.SHORT to "Int16",
        PrimitiveType.INT to "Int32",
        PrimitiveType.FLOAT to "Float32",
        PrimitiveType.DOUBLE to "Float64"))

    val primitiveToSizeConstructor =
        PrimitiveType.values().associate { type ->
            type to (primitiveToTypedArrayMap[type]?.let {
                unOp("${it.toLowerCase()}Array").symbol
            } ?: getInternalFunction("${type.typeName.asString().toLowerCase()}Array"))
        }

    val primitiveToLiteralConstructor =
        PrimitiveType.values().associate { type ->
            type to (primitiveToTypedArrayMap[type]?.let {
                unOp("${it.toLowerCase()}ArrayOf").symbol
            } ?: getInternalFunction("${type.typeName.asString().toLowerCase()}ArrayOf"))
        }

    val arrayConcat = getInternalWithoutPackage("arrayConcat")

    val primitiveArrayConcat = getInternalWithoutPackage("primitiveArrayConcat")

    val jsArraySlice = unOp("slice")

    // TODO move to IntrinsifyCallsLowering
    val doNotIntrinsifyAnnotationSymbol = context.symbolTable.referenceClass(context.getInternalClass("DoNotIntrinsify"))

    // TODO move CharSequence-related stiff to IntrinsifyCallsLowering
    val charSequenceClassSymbol = context.symbolTable.referenceClass(context.getClass(FqName("kotlin.CharSequence")))
    val charSequenceLengthPropertyGetterSymbol = charSequenceClassSymbol.owner.declarations.filterIsInstance<IrProperty>().first { it.name.asString() == "length" }.getter!!.symbol
    val charSequenceGetFunctionSymbol = charSequenceClassSymbol.owner.declarations.filterIsInstance<IrFunction>().single { it.name.asString() == "get"}.symbol
    val charSequenceSubSequenceFunctionSymbol = charSequenceClassSymbol.owner.declarations.filterIsInstance<IrFunction>().single { it.name.asString() == "subSequence"}.symbol


    val jsCharSequenceGet = getInternalFunction("charSequenceGet")
    val jsCharSequenceLength = getInternalFunction("charSequenceLength")
    val jsCharSequenceSubSequence = getInternalFunction("charSequenceSubSequence")

    // Helpers:

    private fun getInternalFunction(name: String) =
        context.symbolTable.referenceSimpleFunction(context.getInternalFunctions(name).single())

    private fun getInternalWithoutPackage(name: String) =
        context.symbolTable.referenceSimpleFunction(context.getFunctions(FqName(name)).single())

    private fun getInternalClassWithoutPackage(fqName: String) =
        context.symbolTable.referenceClass(context.getClass(FqName(fqName)))

    // TODO: unify how we create intrinsic symbols
    private fun defineObjectCreateIntrinsic() =
        JsIrBuilder.buildFunction("Object\$create", isInline = true, origin = JsLoweredDeclarationOrigin.JS_INTRINSICS_STUB).also {
            val typeParameter = JsIrBuilder.buildTypeParameter(Name.identifier("T"), 0, true)
            val anyType = irBuiltIns.anyType
            typeParameter.parent = it
            typeParameter.superTypes += anyType
            it.typeParameters += typeParameter
            it.returnType = anyType
            it.parent = externalPackageFragment
            externalPackageFragment.declarations += it
        }

    private fun defineGetJSPropertyIntrinsic() =
        JsIrBuilder.buildFunction("\$getJSProperty\$", origin = JsLoweredDeclarationOrigin.JS_INTRINSICS_STUB).also {
            it.returnType = irBuiltIns.anyNType
            listOf("receiver", "fieldName").mapIndexedTo(it.valueParameters) { i, p ->
                JsIrBuilder.buildValueParameter(p, i, irBuiltIns.anyType).also { v -> v.parent = it }
            }
            it.parent = externalPackageFragment
            externalPackageFragment.declarations += it
        }

    private fun defineSetJSPropertyIntrinsic() =
        JsIrBuilder.buildFunction("\$setJSProperty\$", origin = JsLoweredDeclarationOrigin.JS_INTRINSICS_STUB).also {
            it.returnType = irBuiltIns.unitType
            listOf("receiver", "fieldName", "fieldValue").mapIndexedTo(it.valueParameters) { i, p ->
                JsIrBuilder.buildValueParameter(p, i, irBuiltIns.anyType).also { v -> v.parent = it }
            }
            it.parent = externalPackageFragment
            externalPackageFragment.declarations += it
        }

    private fun defineUnreachableIntrinsic() =
        JsIrBuilder.buildFunction(Namer.UNREACHABLE_NAME, origin = JsLoweredDeclarationOrigin.JS_INTRINSICS_STUB).also {
            it.returnType = irBuiltIns.nothingType
            it.parent = externalPackageFragment
            externalPackageFragment.declarations += it
        }

    private fun unOp(name: String, returnType: KotlinType = irBuiltIns.anyN) =
        irBuiltIns.run { defineOperator(name, returnType, listOf(anyN)) }

    private fun unOpBool(name: String) = unOp(name, irBuiltIns.bool)
    private fun unOpInt(name: String) = unOp(name, irBuiltIns.int)

    private fun binOp(name: String, returnType: KotlinType = irBuiltIns.anyN) =
        irBuiltIns.run { defineOperator(name, returnType, listOf(anyN, anyN)) }

    private fun tripleOp(name: String, returnType: KotlinType = irBuiltIns.anyN) =
        irBuiltIns.run { defineOperator(name, returnType, listOf(anyN, anyN, anyN)) }

    private fun binOpBool(name: String) = binOp(name, irBuiltIns.bool)
    private fun binOpInt(name: String) = binOp(name, irBuiltIns.int)
}
