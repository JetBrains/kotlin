/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeBuilder
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.types.isLong
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.findDeclaration
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

class JsIntrinsics(private val irBuiltIns: IrBuiltIns, val context: JsIrBackendContext) {

    private val externalPackageFragmentSymbol = IrExternalPackageFragmentSymbolImpl(context.internalPackageFragmentDescriptor)
    val externalPackageFragment = IrExternalPackageFragmentImpl(externalPackageFragmentSymbol)

    // TODO: Should we drop operator intrinsics in favor of IrDynamicOperatorExpression?

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
    val isCharSequenceSymbol = getInternalFunction("isCharSequence")

    val isPrimitiveArray = mapOf(
        PrimitiveType.BOOLEAN to getInternalFunction("isBooleanArray"),
        PrimitiveType.BYTE to getInternalFunction("isByteArray"),
        PrimitiveType.SHORT to getInternalFunction("isShortArray"),
        PrimitiveType.CHAR to getInternalFunction("isCharArray"),
        PrimitiveType.INT to getInternalFunction("isIntArray"),
        PrimitiveType.FLOAT to getInternalFunction("isFloatArray"),
        PrimitiveType.LONG to getInternalFunction("isLongArray"),
        PrimitiveType.DOUBLE to getInternalFunction("isDoubleArray")
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

    val jsGetContinuation = getInternalFunction("getContinuation")
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

    val longCompareToLong: IrSimpleFunction = longClassSymbol.owner.findDeclaration<IrSimpleFunction> {
        it.name == Name.identifier("compareTo") && it.valueParameters[0].type.isLong()
    }!!

    val charClassSymbol = getInternalClassWithoutPackage("kotlin.Char")
    val charConstructor = charClassSymbol.constructors.single().owner

    val uByteClassSymbol by lazy { getInternalClassWithoutPackage("kotlin.UByte") }
    val uShortClassSymbol by lazy { getInternalClassWithoutPackage("kotlin.UShort") }
    val uIntClassSymbol by lazy { getInternalClassWithoutPackage("kotlin.UInt") }
    val uLongClassSymbol by lazy { getInternalClassWithoutPackage("kotlin.ULong") }

    val unreachable = defineUnreachableIntrinsic()

    val returnIfSuspended = getInternalFunction("returnIfSuspended")
    val getContinuation = getInternalFunction("getContinuation")

    // Arrays:
    val array = context.symbolTable.referenceClass(irBuiltIns.builtIns.array)

    val primitiveArrays = PrimitiveType.values()
        .associate { context.symbolTable.referenceClass(irBuiltIns.builtIns.getPrimitiveArrayClassDescriptor(it)) to it }

    val jsArray = getInternalFunction("arrayWithFun")
    val jsFillArray = getInternalFunction("fillArrayFun")

    val jsArrayLength = unOp("jsArrayLength")
    val jsArrayGet = binOp("jsArrayGet")
    val jsArraySet = tripleOp("jsArraySet")

    val jsArrayIteratorFunction = getInternalFunction("arrayIterator")

    val jsPrimitiveArrayIteratorFunctions =
        PrimitiveType.values().associate { it to getInternalFunction("${it.typeName.asString().toLowerCase()}ArrayIterator") }

    val arrayLiteral = unOp("arrayLiteral")

    val primitiveToTypedArrayMap = EnumMap(
        mapOf(
            PrimitiveType.BYTE to "Int8",
            PrimitiveType.SHORT to "Int16",
            PrimitiveType.INT to "Int32",
            PrimitiveType.FLOAT to "Float32",
            PrimitiveType.DOUBLE to "Float64"
        )
    )

    val primitiveToSizeConstructor =
        PrimitiveType.values().associate { type ->
            type to (primitiveToTypedArrayMap[type]?.let {
                unOp("${it.toLowerCase()}Array")
            } ?: getInternalFunction("${type.typeName.asString().toLowerCase()}Array"))
        }

    val primitiveToLiteralConstructor =
        PrimitiveType.values().associate { type ->
            type to (primitiveToTypedArrayMap[type]?.let {
                unOp("${it.toLowerCase()}ArrayOf")
            } ?: getInternalFunction("${type.typeName.asString().toLowerCase()}ArrayOf"))
        }

    val arrayConcat = getInternalWithoutPackage("arrayConcat")

    val primitiveArrayConcat = getInternalWithoutPackage("primitiveArrayConcat")
    val taggedArrayCopy = getInternalWithoutPackage("taggedArrayCopy")

    val jsArraySlice = defineJsSliceIntrinsic().symbol

    val jsBind = defineJsBindIntrinsic()

    // TODO move to IntrinsifyCallsLowering
    val doNotIntrinsifyAnnotationSymbol = context.symbolTable.referenceClass(context.getJsInternalClass("DoNotIntrinsify"))

    // TODO move CharSequence-related stiff to IntrinsifyCallsLowering
    val charSequenceClassSymbol = context.symbolTable.referenceClass(context.getClass(FqName("kotlin.CharSequence")))
    val charSequenceLengthPropertyGetterSymbol =
        charSequenceClassSymbol.owner.declarations.filterIsInstance<IrProperty>().first { it.name.asString() == "length" }.getter!!.symbol
    val charSequenceGetFunctionSymbol =
        charSequenceClassSymbol.owner.declarations.filterIsInstance<IrFunction>().single { it.name.asString() == "get" }.symbol
    val charSequenceSubSequenceFunctionSymbol =
        charSequenceClassSymbol.owner.declarations.filterIsInstance<IrFunction>().single { it.name.asString() == "subSequence" }.symbol


    val jsCharSequenceGet = getInternalFunction("charSequenceGet")
    val jsCharSequenceLength = getInternalFunction("charSequenceLength")
    val jsCharSequenceSubSequence = getInternalFunction("charSequenceSubSequence")

    val jsBoxIntrinsic = getInternalFunction("boxIntrinsic")
    val jsUnboxIntrinsic = getInternalFunction("unboxIntrinsic")

    // Helpers:

    private fun getInternalFunction(name: String) =
        context.symbolTable.referenceSimpleFunction(context.getJsInternalFunction(name))

    private fun getInternalWithoutPackage(name: String) =
        context.symbolTable.referenceSimpleFunction(context.getFunctions(FqName(name)).single())

    private fun getInternalClassWithoutPackage(fqName: String) =
        context.symbolTable.referenceClass(context.getClass(FqName(fqName)))

    // TODO: unify how we create intrinsic symbols
    private fun defineObjectCreateIntrinsic(): IrSimpleFunction {
        return JsIrBuilder.buildFunction(
            "Object\$create",
            returnType = irBuiltIns.anyType,
            parent = externalPackageFragment,
            isInline = true,
            origin = JsLoweredDeclarationOrigin.JS_INTRINSICS_STUB
        ).also {
            val typeParameter = JsIrBuilder.buildTypeParameter(Name.identifier("T"), 0, true)
            typeParameter.parent = it
            typeParameter.superTypes += irBuiltIns.anyType
            it.typeParameters += typeParameter
            externalPackageFragment.declarations += it
        }
    }

    private fun defineGetJSPropertyIntrinsic() =
        JsIrBuilder.buildFunction(
            "\$getJSProperty\$",
            returnType = irBuiltIns.anyNType,
            parent = externalPackageFragment,
            origin = JsLoweredDeclarationOrigin.JS_INTRINSICS_STUB
        ).also {
            listOf("receiver", "fieldName").mapIndexedTo(it.valueParameters) { i, p ->
                JsIrBuilder.buildValueParameter(p, i, irBuiltIns.anyType).also { v -> v.parent = it }
            }
            externalPackageFragment.declarations += it
        }

    private fun defineJsBindIntrinsic() =
        JsIrBuilder.buildFunction(
            "\$jsBind\$",
            returnType = irBuiltIns.anyNType,
            parent = externalPackageFragment,
            origin = JsLoweredDeclarationOrigin.JS_INTRINSICS_STUB
        ).also {
            listOf("receiver", "target").mapIndexedTo(it.valueParameters) { i, p ->
                JsIrBuilder.buildValueParameter(p, i, irBuiltIns.anyType).also { v -> v.parent = it }
            }
            externalPackageFragment.declarations += it
        }

    private fun defineJsSliceIntrinsic(): IrSimpleFunction {
        val typeParameter = JsIrBuilder.buildTypeParameter(Name.identifier("A"), 0, true).apply {
            superTypes += irBuiltIns.anyType
        }
        val type = IrSimpleTypeBuilder().run {
            classifier = typeParameter.symbol
            buildSimpleType()
        }

        return JsIrBuilder.buildFunction(
            "slice",
            returnType = type,
            parent = externalPackageFragment,
            origin = JsLoweredDeclarationOrigin.JS_INTRINSICS_STUB
        ).also {
            it.typeParameters += typeParameter.also { t -> t.parent = it }
            it.valueParameters += JsIrBuilder.buildValueParameter("a", 0, type).also { v -> v.parent = it }
            externalPackageFragment.declarations += it
        }
    }

    private fun defineSetJSPropertyIntrinsic() =
        JsIrBuilder.buildFunction(
            "\$setJSProperty\$",
            returnType = irBuiltIns.unitType,
            parent = externalPackageFragment,
            origin = JsLoweredDeclarationOrigin.JS_INTRINSICS_STUB
        ).also {
            listOf("receiver", "fieldName", "fieldValue").mapIndexedTo(it.valueParameters) { i, p ->
                JsIrBuilder.buildValueParameter(p, i, irBuiltIns.anyType).also { v -> v.parent = it }
            }
            externalPackageFragment.declarations += it
        }

    private fun defineUnreachableIntrinsic() =
        JsIrBuilder.buildFunction(
            Namer.UNREACHABLE_NAME,
            returnType = irBuiltIns.nothingType,
            parent = externalPackageFragment,
            origin = JsLoweredDeclarationOrigin.JS_INTRINSICS_STUB
        ).also {
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
