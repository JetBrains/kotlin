/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Symbols for builtins that are available without any context and are not specific to any backend
 * (but specific to the frontend)
 */
abstract class IrBuiltIns {
    abstract val languageVersionSettings: LanguageVersionSettings

    abstract val irFactory: IrFactory

    abstract val anyType: IrType
    abstract val anyClass: IrClassSymbol
    abstract val anyNType: IrType
    abstract val booleanType: IrType
    abstract val booleanClass: IrClassSymbol
    abstract val charType: IrType
    abstract val charClass: IrClassSymbol
    abstract val numberType: IrType
    abstract val numberClass: IrClassSymbol
    abstract val byteType: IrType
    abstract val byteClass: IrClassSymbol
    abstract val shortType: IrType
    abstract val shortClass: IrClassSymbol
    abstract val intType: IrType
    abstract val intClass: IrClassSymbol
    abstract val longType: IrType
    abstract val longClass: IrClassSymbol
    abstract val floatType: IrType
    abstract val floatClass: IrClassSymbol
    abstract val doubleType: IrType
    abstract val doubleClass: IrClassSymbol
    abstract val nothingType: IrType
    abstract val nothingClass: IrClassSymbol
    abstract val nothingNType: IrType
    abstract val unitType: IrType
    abstract val unitClass: IrClassSymbol
    abstract val stringType: IrType
    abstract val stringClass: IrClassSymbol
    abstract val charSequenceClass: IrClassSymbol

    abstract val collectionClass: IrClassSymbol
    abstract val arrayClass: IrClassSymbol
    abstract val setClass: IrClassSymbol
    abstract val listClass: IrClassSymbol
    abstract val mapClass: IrClassSymbol
    abstract val mapEntryClass: IrClassSymbol
    abstract val iterableClass: IrClassSymbol
    abstract val iteratorClass: IrClassSymbol
    abstract val listIteratorClass: IrClassSymbol
    abstract val mutableCollectionClass: IrClassSymbol
    abstract val mutableSetClass: IrClassSymbol
    abstract val mutableListClass: IrClassSymbol
    abstract val mutableMapClass: IrClassSymbol
    abstract val mutableMapEntryClass: IrClassSymbol
    abstract val mutableIterableClass: IrClassSymbol
    abstract val mutableIteratorClass: IrClassSymbol
    abstract val mutableListIteratorClass: IrClassSymbol

    abstract val comparableClass: IrClassSymbol
    abstract val throwableType: IrType
    abstract val throwableClass: IrClassSymbol
    abstract val kCallableClass: IrClassSymbol
    abstract val kPropertyClass: IrClassSymbol
    abstract val kClassClass: IrClassSymbol
    abstract val kProperty0Class: IrClassSymbol
    abstract val kProperty1Class: IrClassSymbol
    abstract val kProperty2Class: IrClassSymbol
    abstract val kMutableProperty0Class: IrClassSymbol
    abstract val kMutableProperty1Class: IrClassSymbol
    abstract val kMutableProperty2Class: IrClassSymbol
    abstract val functionClass: IrClassSymbol
    abstract val kFunctionClass: IrClassSymbol
    abstract val annotationType: IrType
    abstract val annotationClass: IrClassSymbol

    // TODO: consider removing to get rid of descriptor-related dependencies
    abstract val primitiveTypeToIrType: Map<PrimitiveType, IrType>

    abstract val primitiveIrTypes: List<IrType>
    abstract val primitiveIrTypesWithComparisons: List<IrType>
    abstract val primitiveFloatingPointIrTypes: List<IrType>

    abstract val byteArray: IrClassSymbol
    abstract val charArray: IrClassSymbol
    abstract val shortArray: IrClassSymbol
    abstract val intArray: IrClassSymbol
    abstract val longArray: IrClassSymbol
    abstract val floatArray: IrClassSymbol
    abstract val doubleArray: IrClassSymbol
    abstract val booleanArray: IrClassSymbol

    abstract val primitiveArraysToPrimitiveTypes: Map<IrClassSymbol, PrimitiveType>
    abstract val primitiveTypesToPrimitiveArrays: Map<PrimitiveType, IrClassSymbol>
    abstract val primitiveArrayElementTypes: Map<IrClassSymbol, IrType?>
    abstract val primitiveArrayForType: Map<IrType?, IrClassSymbol>

    abstract val unsignedTypesToUnsignedArrays: Map<UnsignedType, IrClassSymbol>
    abstract val unsignedArraysElementTypes: Map<IrClassSymbol, IrType?>

    abstract val lessFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
    abstract val lessOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
    abstract val greaterOrEqualFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
    abstract val greaterFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
    abstract val ieee754equalsFunByOperandType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>
    abstract val booleanNotSymbol: IrSimpleFunctionSymbol
    abstract val eqeqeqSymbol: IrSimpleFunctionSymbol
    abstract val eqeqSymbol: IrSimpleFunctionSymbol
    abstract val throwCceSymbol: IrSimpleFunctionSymbol
    abstract val throwIseSymbol: IrSimpleFunctionSymbol
    abstract val andandSymbol: IrSimpleFunctionSymbol
    abstract val ororSymbol: IrSimpleFunctionSymbol
    abstract val noWhenBranchMatchedExceptionSymbol: IrSimpleFunctionSymbol
    abstract val illegalArgumentExceptionSymbol: IrSimpleFunctionSymbol
    abstract val checkNotNullSymbol: IrSimpleFunctionSymbol
    abstract val dataClassArrayMemberHashCodeSymbol: IrSimpleFunctionSymbol
    abstract val dataClassArrayMemberToStringSymbol: IrSimpleFunctionSymbol
    abstract val enumClass: IrClassSymbol

    abstract val intPlusSymbol: IrSimpleFunctionSymbol
    abstract val intTimesSymbol: IrSimpleFunctionSymbol
    abstract val intXorSymbol: IrSimpleFunctionSymbol

    abstract val extensionToString: IrSimpleFunctionSymbol
    abstract val memberToString: IrSimpleFunctionSymbol

    abstract val extensionStringPlus: IrSimpleFunctionSymbol
    abstract val memberStringPlus: IrSimpleFunctionSymbol

    abstract val arrayOf: IrSimpleFunctionSymbol
    abstract val arrayOfNulls: IrSimpleFunctionSymbol

    abstract val linkageErrorSymbol: IrSimpleFunctionSymbol

    abstract fun functionN(arity: Int): IrClass
    abstract fun kFunctionN(arity: Int): IrClass
    abstract fun suspendFunctionN(arity: Int): IrClass
    abstract fun kSuspendFunctionN(arity: Int): IrClass

    // TODO: drop variants from segments, add helper from whole fqn
    abstract fun findFunctions(name: Name, vararg packageNameSegments: String = arrayOf("kotlin")): Iterable<IrSimpleFunctionSymbol>
    abstract fun findFunctions(name: Name, packageFqName: FqName): Iterable<IrSimpleFunctionSymbol>
    abstract fun findClass(name: Name, vararg packageNameSegments: String = arrayOf("kotlin")): IrClassSymbol?
    abstract fun findClass(name: Name, packageFqName: FqName): IrClassSymbol?

    abstract fun getKPropertyClass(mutable: Boolean, n: Int): IrClassSymbol

    abstract fun findBuiltInClassMemberFunctions(builtInClass: IrClassSymbol, name: Name): Iterable<IrSimpleFunctionSymbol>

    abstract fun getNonBuiltInFunctionsByExtensionReceiver(
        name: Name, vararg packageNameSegments: String
    ): Map<IrClassifierSymbol, IrSimpleFunctionSymbol>

    abstract fun getNonBuiltinFunctionsByReturnType(
        name: Name, vararg packageNameSegments: String
    ): Map<IrClassifierSymbol, IrSimpleFunctionSymbol>

    abstract fun getBinaryOperator(name: Name, lhsType: IrType, rhsType: IrType): IrSimpleFunctionSymbol
    abstract fun getUnaryOperator(name: Name, receiverType: IrType): IrSimpleFunctionSymbol

    abstract val operatorsPackageFragment: IrExternalPackageFragment

    companion object {
        val KOTLIN_INTERNAL_IR_FQN = FqName("kotlin.internal.ir")
        val BUILTIN_OPERATOR = object : IrDeclarationOriginImpl("OPERATOR") {}
    }
}

object BuiltInOperatorNames {
    const val LESS = "less"
    const val LESS_OR_EQUAL = "lessOrEqual"
    const val GREATER = "greater"
    const val GREATER_OR_EQUAL = "greaterOrEqual"
    const val EQEQ = "EQEQ"
    const val EQEQEQ = "EQEQEQ"
    const val IEEE754_EQUALS = "ieee754equals"
    const val THROW_CCE = "THROW_CCE"
    const val THROW_ISE = "THROW_ISE"
    const val NO_WHEN_BRANCH_MATCHED_EXCEPTION = "noWhenBranchMatchedException"
    const val ILLEGAL_ARGUMENT_EXCEPTION = "illegalArgumentException"
    const val ANDAND = "ANDAND"
    const val OROR = "OROR"
}
