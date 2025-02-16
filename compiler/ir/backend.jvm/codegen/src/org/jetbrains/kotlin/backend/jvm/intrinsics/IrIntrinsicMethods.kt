/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.JvmSymbols
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isFileClass
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeAsciiOnly
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type

class IrIntrinsicMethods(val irBuiltIns: IrBuiltIns, val symbols: JvmSymbols) {
    private val kotlinFqn = StandardNames.BUILT_INS_PACKAGE_FQ_NAME
    private val kotlinJvmFqn = FqName("kotlin.jvm")
    private val kotlinJvmInternalUnsafeFqn = FqName("kotlin.jvm.internal.unsafe")
    private val kotlinReflectFqn = StandardNames.KOTLIN_REFLECT_FQ_NAME
    private val kotlinEnumsFqn = FqName("kotlin.enums")

    private val anyFqn = StandardNames.FqNames.any.toSafe()
    private val arrayFqn = StandardNames.FqNames.array.toSafe()
    private val cloneableFqn = StandardNames.FqNames.cloneable.toSafe()
    private val intFqn = StandardNames.FqNames._int.toSafe()
    private val longFqn = StandardNames.FqNames._long.toSafe()
    private val booleanFqn = StandardNames.FqNames._boolean.toSafe()
    private val kClassFqn = StandardNames.FqNames.kClass.toSafe()
    private val stringFqn = StandardNames.FqNames.string.toSafe()

    private val intrinsics = (
            listOf(
                Key(kotlinJvmFqn, FqName("T"), "<get-javaClass>", emptyList()) to JavaClassProperty,
                Key(kotlinJvmFqn, kClassFqn, "<get-javaObjectType>", emptyList()) to GetJavaObjectType,
                Key(kotlinJvmFqn, kClassFqn, "<get-javaPrimitiveType>", emptyList()) to GetJavaPrimitiveType,
                Key(kotlinJvmFqn, kClassFqn, "<get-java>", emptyList()) to KClassJavaProperty,
                Key(kotlinJvmInternalUnsafeFqn, null, "monitorEnter", listOf(anyFqn)) to MonitorInstruction.MONITOR_ENTER,
                Key(kotlinJvmInternalUnsafeFqn, null, "monitorExit", listOf(anyFqn)) to MonitorInstruction.MONITOR_EXIT,
                Key(kotlinJvmFqn, arrayFqn, "isArrayOf", emptyList()) to IsArrayOf,
                Key(kotlinFqn, null, "arrayOfNulls", listOf(intFqn)) to NewArray,
                Key(cloneableFqn, null, "clone", emptyList()) to Clone,
                Key(kotlinFqn, null, "enumValues", listOf()) to EnumValues,
                Key(kotlinFqn, null, "enumValueOf", listOf(stringFqn)) to EnumValueOf,
                Key(kotlinEnumsFqn, null, "enumEntries", listOf()) to EnumEntries,
                Key(kotlinFqn, stringFqn, "plus", listOf(anyFqn)) to StringPlus,
                Key(kotlinReflectFqn, null, "typeOf", listOf()) to TypeOf,
                irBuiltIns.eqeqSymbol.toKey()!! to Equals(KtTokens.EQEQ),
                irBuiltIns.eqeqeqSymbol.toKey()!! to Equals(KtTokens.EQEQEQ),
                irBuiltIns.ieee754equalsFunByOperandType[irBuiltIns.floatClass]!!.toKey()!! to Ieee754Equals(Type.FLOAT_TYPE),
                irBuiltIns.ieee754equalsFunByOperandType[irBuiltIns.doubleClass]!!.toKey()!! to Ieee754Equals(Type.DOUBLE_TYPE),
                irBuiltIns.booleanNotSymbol.toKey()!! to Not,
                irBuiltIns.noWhenBranchMatchedExceptionSymbol.toKey()!! to IrNoWhenBranchMatchedException,
                irBuiltIns.illegalArgumentExceptionSymbol.toKey()!! to IrIllegalArgumentException,
                irBuiltIns.checkNotNullSymbol.toKey()!! to IrCheckNotNull,
                irBuiltIns.andandSymbol.toKey()!! to AndAnd,
                irBuiltIns.ororSymbol.toKey()!! to OrOr,
                irBuiltIns.dataClassArrayMemberHashCodeSymbol.toKey()!! to IrDataClassArrayMemberHashCode,
                irBuiltIns.dataClassArrayMemberToStringSymbol.toKey()!! to IrDataClassArrayMemberToString,
                symbols.singleArgumentInlineFunction.toKey()!! to SingleArgumentInlineFunctionIntrinsic,
                symbols.unsafeCoerceIntrinsic.toKey()!! to UnsafeCoerce,
                symbols.signatureStringIntrinsic.toKey()!! to SignatureString,
                symbols.throwNullPointerException.toKey()!! to ThrowException(Type.getObjectType("java/lang/NullPointerException")),
                symbols.throwTypeCastException.toKey()!! to ThrowException(Type.getObjectType("kotlin/TypeCastException")),
                symbols.throwUnsupportedOperationException.toKey()!! to ThrowException(Type.getObjectType("java/lang/UnsupportedOperationException")),
                symbols.throwIllegalAccessException.toKey()!! to ThrowException(Type.getObjectType("java/lang/IllegalAccessException")),
                symbols.throwKotlinNothingValueException.toKey()!! to ThrowKotlinNothingValueException,
                symbols.jvmIndyIntrinsic.toKey()!! to JvmInvokeDynamic,
                symbols.jvmDebuggerInvokeSpecialIntrinsic.toKey()!! to JvmDebuggerInvokeSpecial,
                symbols.getClassByDescriptor.toKey()!! to GetClassByDescriptor,
                symbols.handleResultOfReflectiveAccess.toKey()!! to HandleResultOfReflectiveAccess,
                symbols.intPostfixIncrDecr.toKey()!! to IntIncr(isPrefix = false),
                symbols.intPrefixIncrDecr.toKey()!! to IntIncr(isPrefix = true)
            ) +
                    numberConversionMethods() +
                    unaryFunForPrimitives("plus", UnaryPlus) +
                    unaryFunForPrimitives("unaryPlus", UnaryPlus) +
                    unaryFunForPrimitives("minus", UnaryMinus) +
                    unaryFunForPrimitives("unaryMinus", UnaryMinus) +
                    unaryFunForPrimitives("inv", Inv) +
                    unaryFunForPrimitives("inc", INC) +
                    unaryFunForPrimitives("dec", DEC) +
                    unaryFunForPrimitives("hashCode", HashCode) +
                    binaryFunForPrimitives("equals", EXPLICIT_EQUALS, irBuiltIns.anyClass) +
                    binaryFunForPrimitivesAcrossPrimitives("rangeTo", RangeTo) +
                    binaryFunForPrimitivesAcrossPrimitives("rangeUntil", RangeUntil) +
                    binaryOp("plus", IADD) +
                    binaryOp("minus", ISUB) +
                    binaryOp("times", IMUL) +
                    binaryOp("div", IDIV) +
                    binaryOp("mod", IREM) +
                    binaryOp("rem", IREM) +
                    binaryOp("shl", ISHL) +
                    binaryOp("shr", ISHR) +
                    binaryOp("ushr", IUSHR) +
                    binaryOp("and", IAND) +
                    binaryOp("or", IOR) +
                    binaryOp("xor", IXOR) +
                    binaryFunForPrimitivesAcrossPrimitives("compareTo", CompareTo) +
                    createKeyMapping(Not, irBuiltIns.booleanClass, "not") +
                    createKeyMapping(StringGetChar, irBuiltIns.stringClass, "get", irBuiltIns.intClass) +
                    symbols.primitiveIteratorsByType.values.map { iteratorClass ->
                        createKeyMapping(IteratorNext, iteratorClass, "next")
                    } +
                    arrayMethods() +
                    primitiveComparisonIntrinsics(irBuiltIns.lessFunByOperandType, KtTokens.LT) +
                    primitiveComparisonIntrinsics(irBuiltIns.lessOrEqualFunByOperandType, KtTokens.LTEQ) +
                    primitiveComparisonIntrinsics(irBuiltIns.greaterFunByOperandType, KtTokens.GT) +
                    primitiveComparisonIntrinsics(irBuiltIns.greaterOrEqualFunByOperandType, KtTokens.GTEQ) +

                    intrinsicsThatShouldHaveBeenLowered() +
                    atomicIntrinsicsForJdk8()
            )

    private val intrinsicsMap = hashMapOf<String, MutableMap<FqName?, MutableMap<Key, IntrinsicMethod>>>()

    init {
        @Suppress("ReplacePutWithAssignment")
        for ((key, intrinsic) in intrinsics) {
            intrinsicsMap.getOrPut(key.name) { hashMapOf() }
                .getOrPut(key.receiverParameterTypeName) { hashMapOf() }
                .put(key, intrinsic)
        }
    }

    private fun intrinsicsThatShouldHaveBeenLowered() =
        (symbols.primitiveTypesToPrimitiveArrays.map { (_, primitiveClassSymbol) ->
            val name = primitiveClassSymbol.owner.name.asString()
            // IntArray -> intArrayOf
            val arrayOfFunName = name.decapitalizeAsciiOnly() + "Of"
            Key(kotlinFqn, null, arrayOfFunName, listOf(primitiveClassSymbol.owner.fqNameWhenAvailable))
        } + listOf(
            Key(kotlinFqn, anyFqn, "toString", emptyList()),
            Key(kotlinFqn, null, "arrayOf", listOf(arrayFqn)),
            Key(stringFqn, null, "plus", listOf(anyFqn)),
        )).map { it to IntrinsicShouldHaveBeenLowered }

    private val PrimitiveType.symbol
        get() = irBuiltIns.primitiveTypeToIrType[this]!!.classOrNull!!

    fun getIntrinsic(symbol: IrFunctionSymbol): IntrinsicMethod? {
        val function = symbol.owner
        val name = function.name.asString()
        val byName = intrinsicsMap[name] ?: return null
        val receiverFqName = function.computeExtensionReceiverFqName()
        val byReceiver = byName[receiverFqName] ?: return null
        val ownerFqName = function.computeOwnerFqName() ?: return null
        return byReceiver[Key(ownerFqName, receiverFqName, name, function.computeValueParameterFqNames())]
    }

    private fun unaryFunForPrimitives(name: String, intrinsic: IntrinsicMethod): List<Pair<Key, IntrinsicMethod>> =
        PrimitiveType.entries.map { type ->
            createKeyMapping(intrinsic, type.symbol, name)
        }

    private fun binaryFunForPrimitivesAcrossPrimitives(name: String, intrinsic: IntrinsicMethod): List<Pair<Key, IntrinsicMethod>> =
        PrimitiveType.entries.flatMap { parameter ->
            binaryFunForPrimitives(name, intrinsic, parameter.symbol)
        }


    private fun binaryFunForPrimitives(
        name: String,
        intrinsic: IntrinsicMethod,
        parameter: IrClassifierSymbol,
    ): List<Pair<Key, IntrinsicMethod>> =
        PrimitiveType.entries.map { type ->
            createKeyMapping(intrinsic, type.symbol, name, parameter)
        }

    private fun binaryOp(methodName: String, opcode: Int) = binaryFunForPrimitivesAcrossPrimitives(methodName, BinaryOp(opcode))

    private fun numberConversionMethods(): List<Pair<Key, IntrinsicMethod>> =
        PrimitiveType.NUMBER_TYPES.flatMap { type ->
            OperatorConventions.NUMBER_CONVERSIONS.map { method ->
                createKeyMapping(NumberCast, type.symbol, method.asString())
            }
        }

    private fun arrayMethods(): List<Pair<Key, IntrinsicMethod>> =
        symbols.primitiveArraysToPrimitiveTypes.flatMap { (array, primitiveType) -> arrayMethods(primitiveType.symbol, array) } +
                arrayMethods(symbols.array.owner.typeParameters.single().symbol, symbols.array)

    private fun atomicIntrinsicsForJdk8(): List<Pair<Key, IntrinsicMethod>> =
        listOf(
            Key(StandardNames.FqNames.atomicInt, null, "compareAndExchange", listOf(intFqn, intFqn)) to AtomicCompareAndExchange(Type.INT),
            Key(StandardNames.FqNames.atomicLong, null, "compareAndExchange", listOf(longFqn, longFqn)) to AtomicCompareAndExchange(Type.LONG),
            Key(StandardNames.FqNames.atomicBoolean, null, "compareAndExchange", listOf(booleanFqn, booleanFqn)) to AtomicCompareAndExchange(Type.BOOLEAN),
            Key(StandardNames.FqNames.atomicReference, null, "compareAndExchange", listOf(FqName("T"), FqName("T"))) to AtomicCompareAndExchange(Type.OBJECT),

            Key(StandardNames.FqNames.atomicIntArray, null, "compareAndExchangeAt", listOf(intFqn, intFqn, intFqn)) to AtomicArrayCompareAndExchange(Type.INT),
            Key(StandardNames.FqNames.atomicLongArray, null, "compareAndExchangeAt", listOf(intFqn, longFqn, longFqn)) to AtomicArrayCompareAndExchange(Type.LONG),
            Key(StandardNames.FqNames.atomicArray, null, "compareAndExchangeAt", listOf(intFqn, FqName("T"), FqName("T"))) to AtomicArrayCompareAndExchange(Type.OBJECT),
        )

    private fun arrayMethods(elementClass: IrClassifierSymbol, arrayClass: IrClassSymbol) =
        listOf(
            createKeyMapping(ArraySize, arrayClass, "<get-size>"),
            createKeyMapping(NewArray, arrayClass, "<init>", irBuiltIns.intClass),
            createKeyMapping(ArraySet, arrayClass, "set", irBuiltIns.intClass, elementClass),
            createKeyMapping(ArrayGet, arrayClass, "get", irBuiltIns.intClass),
            createKeyMapping(Clone, arrayClass, "clone"),
            createKeyMapping(ArrayIterator, arrayClass, "iterator")
        )

    private fun primitiveComparisonIntrinsics(
        typeToIrFun: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>,
        operator: KtSingleValueToken,
    ): List<Pair<Key, PrimitiveComparison>> =
        PrimitiveType.entries.mapNotNull { primitiveType ->
            val irPrimitiveClassifier = irBuiltIns.primitiveTypeToIrType[primitiveType]!!.classifierOrFail
            val irFunSymbol = typeToIrFun[irPrimitiveClassifier] ?: return@mapNotNull null
            irFunSymbol.toKey()!! to PrimitiveComparison(primitiveType, operator)
        }

    data class Key(val owner: FqName, val receiverParameterTypeName: FqName?, val name: String, val valueParameterTypeNames: List<FqName?>)

    companion object {
        private val INC = Increment(1)
        private val DEC = Increment(-1)
        private val EXPLICIT_EQUALS = ExplicitEquals()

        private fun IrFunctionSymbol.toKey(): Key? = owner.toKey()

        private fun IrFunction.toKey(): Key? {
            return Key(
                computeOwnerFqName() ?: return null,
                computeExtensionReceiverFqName(),
                name.asString(),
                computeValueParameterFqNames()
            )
        }

        private fun IrFunction.computeOwnerFqName(): FqName? {
            val ownerFqName = when (val parent = parent) {
                is IrClass -> {
                    if (parent.isFileClass) (parent.parent as IrPackageFragment).packageFqName
                    else parent.fqNameWhenAvailable
                }
                is IrPackageFragment -> parent.packageFqName
                else -> null
            }
            return ownerFqName
        }

        private fun IrFunction.computeExtensionReceiverFqName(): FqName? =
            computeParameterFqName(extensionReceiverParameter)

        private fun computeParameterFqName(parameter: IrValueParameter?): FqName? =
            computeParameterFqName(parameter?.type?.classifierOrNull)

        private fun computeParameterFqName(parameter: IrClassifierSymbol?): FqName? =
            parameter?.owner?.let {
                when (it) {
                    is IrClass -> it.fqNameWhenAvailable
                    is IrTypeParameter -> FqName(it.name.asString())
                    else -> null
                }
            }

        private fun IrFunction.computeValueParameterFqNames(): List<FqName?> =
            valueParameters.map(::computeParameterFqName)

        private fun createKeyMapping(
            intrinsic: IntrinsicMethod,
            klass: IrClassSymbol,
            name: String,
            vararg args: IrClassifierSymbol,
        ): Pair<Key, IntrinsicMethod> =
            Key(klass.owner.fqNameWhenAvailable!!, null, name, args.map { computeParameterFqName(it) }) to
                    intrinsic
    }
}
