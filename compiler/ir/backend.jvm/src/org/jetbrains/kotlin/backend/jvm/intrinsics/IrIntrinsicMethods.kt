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
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type

class IrIntrinsicMethods(val irBuiltIns: IrBuiltIns, val symbols: JvmSymbols) {
    private val kotlinJvm = FqName("kotlin.jvm")
    private val kotlinJvmInternalUnsafe = FqName("kotlin.jvm.internal.unsafe")

    private val intrinsicsMap = (
            listOf(
                Key(kotlinJvm, FqName("T"), "<get-javaClass>", emptyList()) to JavaClassProperty,
                Key(
                    kotlinJvm,
                    KotlinBuiltIns.FQ_NAMES.kClass.toSafe(),
                    "<get-java>",
                    emptyList()
                ) to KClassJavaProperty,
                Key(
                    kotlinJvmInternalUnsafe,
                    null,
                    "access\$monitorEnter\$0",
                    listOf(KotlinBuiltIns.FQ_NAMES.any.toSafe())
                ) to MonitorInstruction.MONITOR_ENTER,
                Key(
                    kotlinJvmInternalUnsafe,
                    null,
                    "access\$monitorExit\$1",
                    listOf(KotlinBuiltIns.FQ_NAMES.any.toSafe())
                ) to MonitorInstruction.MONITOR_EXIT,
                Key(
                    kotlinJvm,
                    KotlinBuiltIns.FQ_NAMES.array.toSafe(),
                    "isArrayOf",
                    emptyList()
                ) to IsArrayOf,
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
                irBuiltIns.eqeqSymbol.toKey()!! to Equals(KtTokens.EQEQ),
                irBuiltIns.eqeqeqSymbol.toKey()!! to Equals(KtTokens.EQEQEQ),
                irBuiltIns.ieee754equalsFunByOperandType[irBuiltIns.floatClass]!!.toKey()!! to Ieee754Equals(Type.FLOAT_TYPE),
                irBuiltIns.ieee754equalsFunByOperandType[irBuiltIns.doubleClass]!!.toKey()!! to Ieee754Equals(Type.DOUBLE_TYPE),
                irBuiltIns.booleanNotSymbol.toKey()!! to Not,
                irBuiltIns.enumValueOfSymbol.toKey()!! to IrEnumValueOf,
                irBuiltIns.noWhenBranchMatchedExceptionSymbol.toKey()!! to IrNoWhenBranchMatchedException,
                irBuiltIns.illegalArgumentExceptionSymbol.toKey()!! to IrIllegalArgumentException,
                irBuiltIns.andandSymbol.toKey()!! to AndAnd,
                irBuiltIns.ororSymbol.toKey()!! to OrOr,
                symbols.unsafeCoerceIntrinsic.toKey()!! to UnsafeCoerce,
                symbols.signatureStringIntrinsic.toKey()!! to SignatureString,
                symbols.reassignParameterIntrinsic.toKey()!! to ReassignParameter
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
                    binaryFunForPrimitives("equals", EQUALS, irBuiltIns.anyClass) +
                    binaryFunForPrimitivesAcrossPrimitives("rangeTo", RangeTo) +
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
                    primitiveComparisonIntrinsics(irBuiltIns.greaterOrEqualFunByOperandType, KtTokens.GTEQ)
            ).toMap()

    private val PrimitiveType.symbol
        get() = irBuiltIns.primitiveTypeToIrType[this]!!.classOrNull!!

    fun getIntrinsic(symbol: IrFunctionSymbol): IntrinsicMethod? = intrinsicsMap[symbol.toKey()]

    private fun unaryFunForPrimitives(name: String, intrinsic: IntrinsicMethod): List<Pair<Key, IntrinsicMethod>> =
        PrimitiveType.values().map { type ->
            createKeyMapping(intrinsic, type.symbol, name)
        }

    private fun binaryFunForPrimitivesAcrossPrimitives(name: String, intrinsic: IntrinsicMethod): List<Pair<Key, IntrinsicMethod>> =
        PrimitiveType.values().flatMap { parameter ->
            binaryFunForPrimitives(name, intrinsic, parameter.symbol)
        }


    private fun binaryFunForPrimitives(
        name: String,
        intrinsic: IntrinsicMethod,
        parameter: IrClassifierSymbol
    ): List<Pair<Key, IntrinsicMethod>> =
        PrimitiveType.values().map { type ->
            createKeyMapping(
                intrinsic,
                type.symbol,
                name,
                parameter
            )
        }

    private fun binaryOp(methodName: String, opcode: Int) = binaryFunForPrimitivesAcrossPrimitives(methodName, BinaryOp(opcode))

    private fun numberConversionMethods(): List<Pair<Key, IntrinsicMethod>> =
        PrimitiveType.NUMBER_TYPES.flatMap { type ->
            numberConversionMethods(type.symbol)
        } + numberConversionMethods(irBuiltIns.numberClass)

    private fun arrayMethods(): List<Pair<Key, IntrinsicMethod>> =
        symbols.primitiveArrays.flatMap { (key, value) ->
            arrayMethods(
                key.symbol,
                value
            )
        } + arrayMethods(symbols.array.owner.typeParameters.single().symbol, symbols.array)

    private fun arrayMethods(elementClass: IrClassifierSymbol, arrayClass: IrClassSymbol) =
        listOf(
            createKeyMapping(ArraySize, arrayClass, "<get-size>"),
            createKeyMapping(NewArray, arrayClass, "<init>", irBuiltIns.intClass),
            createKeyMapping(ArraySet, arrayClass, "set", irBuiltIns.intClass, elementClass),
            createKeyMapping(ArrayGet, arrayClass, "get", irBuiltIns.intClass),
            createKeyMapping(Clone, arrayClass, "clone"),
            createKeyMapping(ArrayIterator, arrayClass, "iterator")
        )

    data class Key(val owner: FqName, val receiverParameterTypeName: FqName?, val name: String, val valueParameterTypeNames: List<FqName?>)

    companion object {

        internal val INTRINSICS_CLASS_NAME = "kotlin/jvm/internal/Intrinsics"
        private val INC = Increment(1)

        private val DEC = Increment(-1)
        private val EQUALS = Equals(KtTokens.EQEQ)

        private fun IrFunctionSymbol.toKey(): Key? {
            val parent = owner.parent
            val ownerFqName = when {
                parent is IrClass && parent.origin == IrDeclarationOrigin.FILE_CLASS ->
                    (parent.parent as IrPackageFragment).fqName
                parent is IrClass -> parent.fqNameWhenAvailable ?: return null
                parent is IrPackageFragment -> parent.fqName
                else -> return null
            }
            return Key(
                ownerFqName,
                getParameterFqName(owner.extensionReceiverParameter),
                owner.name.asString(),
                owner.valueParameters.map(::getParameterFqName)
            )
        }

        private fun getParameterFqName(parameter: IrValueParameter?): FqName? =
            getParameterFqName(parameter?.type?.classifierOrNull)

        private fun getParameterFqName(parameter: IrClassifierSymbol?): FqName? =
            parameter?.owner?.let {
                when (it) {
                    is IrClass -> it.fqNameWhenAvailable
                    is IrTypeParameter -> FqName(it.name.asString())
                    else -> null
                }
            }

        private fun createKeyMapping(
            intrinsic: IntrinsicMethod,
            klass: IrClassSymbol,
            name: String,
            vararg args: IrClassifierSymbol
        ): Pair<Key, IntrinsicMethod> =
            Key(klass.owner.fqNameWhenAvailable!!, null, name, args.map {
                getParameterFqName(it)
            }) to intrinsic

        private fun numberConversionMethods(numberClass: IrClassSymbol) =
            OperatorConventions.NUMBER_CONVERSIONS.map { method ->
                createKeyMapping(NumberCast, numberClass, method.asString())
            }


        private fun primitiveComparisonIntrinsics(
            typeToIrFun: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>,
            operator: KtSingleValueToken
        ): List<Pair<Key, PrimitiveComparison>> =
            typeToIrFun.map { (type, irFunSymbol) ->
                irFunSymbol.toKey()!! to PrimitiveComparison(type.descriptor.defaultType, operator)
            }
    }
}
