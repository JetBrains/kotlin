/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addTypeParameter
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isLong
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.findDeclaration
import org.jetbrains.kotlin.ir.util.kotlinPackageFqn
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
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

    // Type checks:

    val jsInstanceOf = binOpBool("jsInstanceOf")
    val jsTypeOf = unOp("jsTypeOf", irBuiltIns.stringType)

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
    val isSuspendFunctionSymbol = getInternalFunction("isSuspendFunction")

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
    val jsCode = getInternalFunction("js") // js("<code>")
    val jsHashCode = getInternalFunction("hashCode")
    val jsGetObjectHashCode = getInternalFunction("getObjectHashCode")
    val jsToString = getInternalFunction("toString")
    val jsAnyToString = getInternalFunction("anyToString")
    val jsCompareTo = getInternalFunction("compareTo")
    val jsEquals = getInternalFunction("equals")

    val jsImul = getInternalFunction("imul")

    // Coroutines

    val jsCoroutineContext
        get() = context.ir.symbols.coroutineContextGetter

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

    val stringClassSymbol = getInternalClassWithoutPackage("kotlin.String")
    val stringConstructorSymbol = stringClassSymbol.constructors.single()

    val anyClassSymbol = getInternalClassWithoutPackage("kotlin.Any")
    val anyConstructorSymbol = anyClassSymbol.constructors.single()

    val jsObjectClassSymbol = getInternalClassWithoutPackage("kotlin.js.JsObject")
    val jsObjectConstructorSymbol by lazy { jsObjectClassSymbol.constructors.single() }

    val uByteClassSymbol by lazy { getInternalClassWithoutPackage("kotlin.UByte") }
    val uShortClassSymbol by lazy { getInternalClassWithoutPackage("kotlin.UShort") }
    val uIntClassSymbol by lazy { getInternalClassWithoutPackage("kotlin.UInt") }
    val uLongClassSymbol by lazy { getInternalClassWithoutPackage("kotlin.ULong") }

    val unreachable = defineUnreachableIntrinsic()

    val returnIfSuspended = getInternalFunction("returnIfSuspended")
    val getContinuation = getInternalFunction("getContinuation")

    val jsEnsureNonNull = getFunctionInKotlinPackage("ensureNotNull")

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
        PrimitiveType.values().associate { it to getInternalFunction("${it.typeName.asString().toLowerCaseAsciiOnly()}ArrayIterator") }

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
                unOp("${it.toLowerCaseAsciiOnly()}Array")
            } ?: getInternalFunction("${type.typeName.asString().toLowerCaseAsciiOnly()}Array"))
        }

    val primitiveToLiteralConstructor =
        PrimitiveType.values().associate { type ->
            type to (primitiveToTypedArrayMap[type]?.let {
                unOp("${it.toLowerCaseAsciiOnly()}ArrayOf")
            } ?: getInternalFunction("${type.typeName.asString().toLowerCaseAsciiOnly()}ArrayOf"))
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
    val charSequenceLengthPropertyGetterSymbol by lazy {
        with(charSequenceClassSymbol.owner.declarations) {
            filterIsInstance<IrProperty>().firstOrNull { it.name.asString() == "length" }?.getter ?:
            filterIsInstance<IrFunction>().first { it.name.asString() == "<get-length>" }
        }.symbol
    }
    val charSequenceGetFunctionSymbol by lazy {
        charSequenceClassSymbol.owner.declarations.filterIsInstance<IrFunction>().single { it.name.asString() == "get" }.symbol
    }
    val charSequenceSubSequenceFunctionSymbol by lazy {
        charSequenceClassSymbol.owner.declarations.filterIsInstance<IrFunction>().single { it.name.asString() == "subSequence" }.symbol
    }


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

    private fun getFunctionInKotlinPackage(name: String) =
        context.symbolTable.referenceSimpleFunction(context.getFunctions(kotlinPackageFqn.child(Name.identifier(name))).single())

    private fun getInternalClassWithoutPackage(fqName: String) =
        context.symbolTable.referenceClass(context.getClass(FqName(fqName)))

    // TODO: unify how we create intrinsic symbols
    private fun defineObjectCreateIntrinsic(): IrSimpleFunction {
        return externalPackageFragment.addFunction {
            name = Name.identifier("Object\$create")
            origin = JsLoweredDeclarationOrigin.JS_INTRINSICS_STUB
        }.apply {
            returnType = addTypeParameter("T", irBuiltIns.anyType).defaultType
        }
    }

    private fun defineJsBindIntrinsic(): IrSimpleFunction {
        return externalPackageFragment.addFunction {
            name = Name.identifier("\$jsBind\$")
            origin = JsLoweredDeclarationOrigin.JS_INTRINSICS_STUB
            returnType = irBuiltIns.anyNType
        }.apply {
            listOf("receiver", "target").forEach { addValueParameter(it, irBuiltIns.anyType) }
        }
    }

    private fun defineJsSliceIntrinsic(): IrSimpleFunction {
        return externalPackageFragment.addFunction {
            name = Name.identifier("slice")
            origin = JsLoweredDeclarationOrigin.JS_INTRINSICS_STUB
        }.apply {
            val aType = addTypeParameter("A", irBuiltIns.anyType).defaultType
            returnType = aType
            addValueParameter("a", aType)
        }
    }

    private fun defineUnreachableIntrinsic(): IrSimpleFunction {
        return externalPackageFragment.addFunction {
            name = Name.identifier(Namer.UNREACHABLE_NAME)
            origin = JsLoweredDeclarationOrigin.JS_INTRINSICS_STUB
            returnType = irBuiltIns.nothingType
        }
    }

    private fun unOp(name: String, returnType: IrType = irBuiltIns.anyNType) =
        irBuiltIns.run { defineOperator(name, returnType, listOf(anyNType)) }

    private fun unOpBool(name: String) = unOp(name, irBuiltIns.booleanType)
    private fun unOpInt(name: String) = unOp(name, irBuiltIns.intType)

    private fun binOp(name: String, returnType: IrType = irBuiltIns.anyNType) =
        irBuiltIns.run { defineOperator(name, returnType, listOf(anyNType, anyNType)) }

    private fun tripleOp(name: String, returnType: IrType = irBuiltIns.anyNType) =
        irBuiltIns.run { defineOperator(name, returnType, listOf(anyNType, anyNType, anyNType)) }

    private fun binOpBool(name: String) = binOp(name, irBuiltIns.booleanType)
    private fun binOpInt(name: String) = binOp(name, irBuiltIns.intType)
}
