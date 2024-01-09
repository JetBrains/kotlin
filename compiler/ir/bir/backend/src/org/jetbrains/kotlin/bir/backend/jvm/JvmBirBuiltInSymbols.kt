/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend.jvm

import org.jetbrains.kotlin.backend.jvm.JvmSymbols
import org.jetbrains.kotlin.bir.backend.BirBuiltInSymbols
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.symbols.BirConstructorSymbol
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.symbols.BirSimpleFunctionSymbol
import org.jetbrains.kotlin.bir.types.BirSimpleType
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.util.Ir2BirConverter
import org.jetbrains.kotlin.storage.LockBasedStorageManager

class JvmBirBuiltInSymbols(irSymbols: JvmSymbols, converter: Ir2BirConverter) : BirBuiltInSymbols(irSymbols, converter) {
    private val storageManager = LockBasedStorageManager(this::class.java.simpleName)

    val singleArgumentInlineFunction: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.singleArgumentInlineFunction)
    val checkExpressionValueIsNotNull: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.checkExpressionValueIsNotNull)
    val checkNotNullExpressionValue: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.checkNotNullExpressionValue)
    val checkNotNull: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.checkNotNull)
    val checkNotNullWithMessage: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.checkNotNullWithMessage)
    val throwNpe: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.throwNpe)
    val throwIllegalAccessException: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.throwIllegalAccessException)
    val throwUnsupportedOperationException: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.throwUnsupportedOperationException)
    val intrinsicsKotlinClass: BirClassSymbol = converter.remapSymbol(irSymbols.intrinsicsKotlinClass)
    val enumEntries: BirClassSymbol = converter.remapSymbol(irSymbols.enumEntries)
    val createEnumEntries: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.createEnumEntries)
    val javaLangClass: BirClassSymbol = converter.remapSymbol(irSymbols.javaLangClass)
    val javaLangDeprecatedConstructorWithDeprecatedFlag: BirConstructorSymbol = converter.remapSymbol(irSymbols.javaLangDeprecatedConstructorWithDeprecatedFlag)
    val assertionErrorConstructor: BirConstructorSymbol = converter.remapSymbol(irSymbols.assertionErrorConstructor)
    val noSuchFieldErrorType: BirType = converter.remapType(irSymbols.noSuchFieldErrorType)
    val resultOfAnyType: BirType = converter.remapType(irSymbols.resultOfAnyType)
    val continuationImplClass: BirClassSymbol = converter.remapSymbol(irSymbols.continuationImplClass)
    val suspendFunctionInterface: BirClassSymbol = converter.remapSymbol(irSymbols.suspendFunctionInterface)
    val lambdaClass: BirClassSymbol = converter.remapSymbol(irSymbols.lambdaClass)
    val suspendLambdaClass: BirClassSymbol = converter.remapSymbol(irSymbols.suspendLambdaClass)
    val restrictedSuspendLambdaClass: BirClassSymbol = converter.remapSymbol(irSymbols.restrictedSuspendLambdaClass)
    val functionReference: BirClassSymbol = converter.remapSymbol(irSymbols.functionReference)
    val functionReferenceGetSignature: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.functionReferenceGetSignature)
    val functionReferenceGetName: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.functionReferenceGetName)
    val functionReferenceGetOwner: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.functionReferenceGetOwner)
    val functionReferenceImpl: BirClassSymbol = converter.remapSymbol(irSymbols.functionReferenceImpl)
    val adaptedFunctionReference: BirClassSymbol = converter.remapSymbol(irSymbols.adaptedFunctionReference)
    val funInterfaceConstructorReferenceClass: BirClassSymbol = converter.remapSymbol(irSymbols.funInterfaceConstructorReferenceClass)
    val functionN: BirClassSymbol = converter.remapSymbol(irSymbols.functionN)
    val jvmInlineAnnotation: BirClassSymbol = converter.remapSymbol(irSymbols.jvmInlineAnnotation)
    val reflection: BirClassSymbol = converter.remapSymbol(irSymbols.reflection)
    //val javaLangReflectSymbols: JvmReflectSymbols = converter.remapSymbol(irSymbols.javaLangReflectSymbols)
    val getOrCreateKotlinPackage: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.getOrCreateKotlinPackage)
    val desiredAssertionStatus: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.desiredAssertionStatus)
    val arrayOfAnyType: BirSimpleType = converter.remapSimpleType(irSymbols.arrayOfAnyType)
    val arrayOfAnyNType: BirSimpleType = converter.remapSimpleType(irSymbols.arrayOfAnyNType)
    val indyLambdaMetafactoryIntrinsic: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.indyLambdaMetafactoryIntrinsic)
    //val serializedLambda: JvmSymbols.SerializedLambdaClass = converter.remapSymbol(irSymbols.serializedLambda)
    val illegalArgumentException: BirClassSymbol = converter.remapSymbol(irSymbols.illegalArgumentException)
    val illegalArgumentExceptionCtorString: BirConstructorSymbol = converter.remapSymbol(irSymbols.illegalArgumentExceptionCtorString)
    val jvmMethodType: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.jvmMethodType)
    val jvmMethodHandle: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.jvmMethodHandle)
    val jvmIndyIntrinsic: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.jvmIndyIntrinsic)
    val jvmOriginalMethodTypeIntrinsic: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.jvmOriginalMethodTypeIntrinsic)
    val jvmDebuggerInvokeSpecialIntrinsic: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.jvmDebuggerInvokeSpecialIntrinsic)
    val nonGenericToArray: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.nonGenericToArray)
    val genericToArray: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.genericToArray)
    val jvmName: BirClassSymbol = converter.remapSymbol(irSymbols.jvmName)
    val kClassJava: BirPropertySymbol = converter.remapSymbol(irSymbols.kClassJava)
    val spreadBuilder: BirClassSymbol = converter.remapSymbol(irSymbols.spreadBuilder)
    val primitiveSpreadBuilders = irSymbols.primitiveSpreadBuilders.entries.associate {
        converter.remapType(it.key) to converter.remapSymbol<_, BirClassSymbol>(it.value)
    }
    val arraysClass: BirClassSymbol = converter.remapSymbol(irSymbols.arraysClass)
    val compareUnsignedInt: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.compareUnsignedInt)
    val divideUnsignedInt: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.divideUnsignedInt)
    val remainderUnsignedInt: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.remainderUnsignedInt)
    val toUnsignedStringInt: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.toUnsignedStringInt)
    val compareUnsignedLong: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.compareUnsignedLong)
    val divideUnsignedLong: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.divideUnsignedLong)
    val remainderUnsignedLong: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.remainderUnsignedLong)
    val toUnsignedStringLong: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.toUnsignedStringLong)
    val intPostfixIncrDecr: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.intPostfixIncrDecr)
    val intPrefixIncrDecr: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.intPrefixIncrDecr)
    val signatureStringIntrinsic: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.signatureStringIntrinsic)
    val enumValueOfFunction: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.enumValueOfFunction)
    val objectCloneFunction: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.objectCloneFunction)
    val runSuspendFunction: BirSimpleFunctionSymbol = converter.remapSymbol(irSymbols.runSuspendFunction)
    val repeatableContainer: BirClassSymbol = converter.remapSymbol(irSymbols.repeatableContainer)


    private val jvmSuspendFunctionClasses = storageManager.createMemoizedFunction { n: Int ->
        converter.remapSymbol<_, BirClassSymbol>(irSymbols.getJvmSuspendFunctionClass(n))
    }

    fun getJvmSuspendFunctionClass(parameterCount: Int): BirClassSymbol =
        jvmSuspendFunctionClasses(parameterCount)
}