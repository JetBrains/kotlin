/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.JvmSymbols
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type

class IrIntrinsicMethods(val irBuiltIns: IrBuiltIns, val symbols: JvmSymbols) {

    private val KOTLIN_INTERNAL_IR = FqName("kotlin.internal.ir")
    private val KOTLIN_JVM = FqName("kotlin.jvm")
    private val KOTLIN_JVM_INTERNAL_UNSAFE = FqName("kotlin.jvm.internal.unsafe")

    val andandSymbol = irBuiltIns.run { defineOperator(OperatorNames.ANDAND, bool, listOf(bool, bool)) }

    private val intrinsicsMap = (
            listOf(
                Key(KOTLIN_JVM, FqName("T"),"<get-javaClass>", emptyList()) to JavaClassProperty,
                Key(
                    KOTLIN_JVM,
                    KotlinBuiltIns.FQ_NAMES.kClass.toSafe(),
                    "<get-java>",
                    emptyList()
                ) to KClassJavaProperty,
                Key(
                    KOTLIN_JVM_INTERNAL_UNSAFE,
                    null,
                    "monitorEnter",
                    listOf(KotlinBuiltIns.FQ_NAMES.any.toSafe())
                ) to MonitorInstruction.MONITOR_ENTER,
                Key(
                    KOTLIN_JVM_INTERNAL_UNSAFE,
                    null,
                    "monitorExit",
                    listOf(KotlinBuiltIns.FQ_NAMES.any.toSafe())
                ) to MonitorInstruction.MONITOR_EXIT,
                Key(
                    KOTLIN_JVM,
                    KotlinBuiltIns.FQ_NAMES.array.toSafe(),
                    "isArrayOf",
                    emptyList()
                ) to IsArrayOf,
                symbols.arrayOf.toKey() to ArrayOf,
                Key(
                    KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME,
                    KotlinBuiltIns.FQ_NAMES.any.toSafe(),
                    "toString",
                    emptyList()
                ) to ToString,
                Key(
                    KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME,
                    null,
                    "arrayOfNulls",
                    listOf(KotlinBuiltIns.FQ_NAMES._int.toSafe())
                ) to NewArray,
                Key(
                    KotlinBuiltIns.FQ_NAMES.cloneable.toSafe(),
                    null,
                    "clone",
                    emptyList()
                ) to Clone,
                Key(
                    KOTLIN_INTERNAL_IR,
                    null,
                    OperatorNames.ANDAND,
                    listOf(KotlinBuiltIns.FQ_NAMES._boolean.toSafe(), KotlinBuiltIns.FQ_NAMES._boolean.toSafe())
                ) to AndAnd,
                irBuiltIns.eqeqSymbol.toKey()!! to Equals(KtTokens.EQEQ),
                irBuiltIns.eqeqeqSymbol.toKey()!! to Equals(KtTokens.EQEQEQ),
                irBuiltIns.ieee754equalsFunByOperandType[irBuiltIns.float]!!.toKey()!! to Ieee754Equals(Type.FLOAT_TYPE),
                irBuiltIns.ieee754equalsFunByOperandType[irBuiltIns.double]!!.toKey()!! to Ieee754Equals(Type.DOUBLE_TYPE),
                irBuiltIns.booleanNotSymbol.toKey()!! to Not,
                irBuiltIns.enumValueOfSymbol.toKey()!! to IrEnumValueOf,
                irBuiltIns.noWhenBranchMatchedExceptionSymbol.toKey()!! to IrNoWhenBranchMatchedException,
                irBuiltIns.illegalArgumentExceptionSymbol.toKey()!! to IrIllegalArgumentException,
                irBuiltIns.throwNpeSymbol.toKey()!! to ThrowNPE
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
                    unaryFunForPrimitives("toString", ToString) +
                    binaryFunForPrimitives("equals", EQUALS) +
                    symbols.primitiveArrayOfByType.values.map { it.toKey() to ArrayOf } +
                    binaryFunForPrimitives("rangeTo", RangeTo) +
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
                    binaryFunForPrimitives("compareTo", CompareTo) +
                    methodWithArity(irBuiltIns.booleanClass, "not", 0, Not) +
                    methodWithArity(irBuiltIns.stringClass, "get", 1, StringGetChar) +
                    symbols.primitiveIteratorsByType.values.flatMap { iteratorClass ->
                        methodWithArity(iteratorClass, "next", 0, IteratorNext)
                    } +
                    arrayMethods() +
                    primitiveComparisonIntrinsics(irBuiltIns.lessFunByOperandType, KtTokens.LT) +
                    primitiveComparisonIntrinsics(irBuiltIns.lessOrEqualFunByOperandType, KtTokens.LTEQ) +
                    primitiveComparisonIntrinsics(irBuiltIns.greaterFunByOperandType, KtTokens.GT) +
                    primitiveComparisonIntrinsics(irBuiltIns.greaterOrEqualFunByOperandType, KtTokens.GTEQ)
    ).toMap()

    fun getIntrinsic(symbol: IrFunctionSymbol): IntrinsicMethod? = intrinsicsMap[symbol.toKey()]

    private fun unaryFunForPrimitives(name: String, intrinsic: IntrinsicMethod): List<Pair<Key, IntrinsicMethod>> =
        PrimitiveType.values().flatMap { type ->
            methodWithArity(irBuiltIns.primitiveTypeToIrType[type]!!.classOrNull!!, name, 0, intrinsic)
        }

    private fun binaryFunForPrimitives(name: String, intrinsic: IntrinsicMethod): List<Pair<Key, IntrinsicMethod>> =
        PrimitiveType.values().flatMap { type ->
            methodWithArity(irBuiltIns.primitiveTypeToIrType[type]!!.classOrNull!!, name, 1, intrinsic)
        }

    private fun binaryOp(methodName: String, opcode: Int) = binaryFunForPrimitives(methodName, BinaryOp(opcode))

    private fun numberConversionMethods(): List<Pair<Key, IntrinsicMethod>> =
        PrimitiveType.NUMBER_TYPES.flatMap { type ->
            numberConversionMethods(irBuiltIns.primitiveTypeToIrType[type]!!.classOrNull!!)
        } + numberConversionMethods(irBuiltIns.numberClass)

    private fun arrayMethods(): List<Pair<Key, IntrinsicMethod>> =
        symbols.primitiveArrays.values.flatMap { arrayMethods(it) } + arrayMethods(symbols.array)

    data class Key(val owner: FqName, val receiverParameterTypeName: FqName?, val name: String, val valueParameterTypeNames: List<FqName?>)

    companion object {

        internal val INTRINSICS_CLASS_NAME = "kotlin/jvm/internal/Intrinsics"
        private val INC = Increment(1)

        private val DEC = Increment(-1)
        private val EQUALS = Equals(KtTokens.EQEQ)

        private fun IrFunctionSymbol.toKey(): Key? {
            return Key(
                owner.parent.safeAs<IrClass>()?.fqNameWhenAvailable ?: owner.parent.safeAs<IrPackageFragment>()?.fqName ?: return null,
                getParameterFqName(owner.extensionReceiverParameter),
                owner.name.asString(),
                owner.valueParameters.map(::getParameterFqName)
            )
        }

        private fun getParameterFqName(parameter: IrValueParameter?): FqName? =
            parameter?.type?.classifierOrNull?.owner?.let {
                when (it) {
                    is IrClass -> it.fqNameWhenAvailable
                    is IrTypeParameter -> FqName(it.name.asString())
                    else -> null
                }
            }

        private fun methodWithArity(klass: IrClassSymbol, name: String, arity: Int, intrinsic: IntrinsicMethod): List<Pair<Key, IntrinsicMethod>> =
            klass.owner.functions.filter {
                it.name.asString() == name && it.valueParameters.size == arity
            }.map {
                it.symbol.toKey()!! to intrinsic
            }.toList()

        private fun numberConversionMethods(numberClass: IrClassSymbol) =
            OperatorConventions.NUMBER_CONVERSIONS.flatMap { method ->
                methodWithArity(numberClass, method.asString(), 0, NumberCast)
            }

        private fun arrayMethods(arrayClass: IrClassSymbol) = listOf(
            arrayClass.constructors.single { it.owner.valueParameters.size == 2 }.toKey()!! to ArrayConstructor,
            arrayClass.owner.properties.single { it.name.asString() == "size" }.getter!!.symbol.toKey()!! to ArraySize
        ) +
                methodWithArity(arrayClass, "set", 2, ArraySet) +
                methodWithArity(arrayClass, "get", 1, ArrayGet) +
                methodWithArity(arrayClass, "clone", 0, Clone) +
                methodWithArity(arrayClass, "iterator", 0, ArrayIterator)

        private fun primitiveComparisonIntrinsics(typeToIrFun: Map<SimpleType, IrSimpleFunctionSymbol>, operator: KtSingleValueToken) =
            typeToIrFun.map { (type, irFunSymbol) ->
                irFunSymbol.toKey()!! to PrimitiveComparison(type, operator)
            }
    }

    private object OperatorNames {
        const val ANDAND = "ANDAND"
    }
}
