/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cfg.outofbound

import com.intellij.util.containers.HashMap
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.CallInstruction
import org.jetbrains.kotlin.psi.JetArrayAccessExpression
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeConstructor

public object MapUtils {
    public fun mapToString<K, V, C : Comparable<C>>(
            map: Map<K,V>,
            keyToComparable: (K) -> C,
            keyToString: (K) -> String = { it.toString() },
            valueToString: (V) -> String = { it.toString() }
    ): String {
        val mapAsString = map.toList().toSortedListBy { keyToComparable(it.first) }.fold("") { acc, keyValue ->
            "$acc${keyToString(keyValue.first)}:${valueToString(keyValue.second)},"
        }
        if (!mapAsString.isEmpty()) {
            return "{${mapAsString.take(mapAsString.length() - 1)}}"
        }
        return "{$mapAsString}"
    }

    public fun mergeMaps<K, V>(
            map1: Map<K, V>,
            map2: Map<K, V>,
            mergeCorrespondingValue: (V, V) -> V
    ): Map<K, V> {
        val resultMap = HashMap(map1)
        for ((key2, value2) in map2) {
            val value1 = resultMap[key2]
            resultMap[key2] = value1?.let { mergeCorrespondingValue(it, value2) } ?: value2
        }
        return resultMap
    }

    public fun mergeMapsIntoFirst<K, V>(
            map1: MutableMap<K, V>,
            map2: Map<K, V>,
            mergeCorrespondingValue: (V, V) -> V
    ) {
        for ((key2, value2) in map2) {
            val value1 = map1[key2]
            map1[key2] = value1?.let { mergeCorrespondingValue(it, value2) } ?: value2
        }
    }
}

public object KotlinArrayUtils {
    public fun isGenericOrPrimitiveArray(type: JetType): Boolean =
            KotlinBuiltIns.isArray(type) || KotlinBuiltIns.isPrimitiveArray(type)

    // Creation
    public val arrayOfFunctionName: String = "arrayOf"
    public val arrayConstructorName: String = "Array"
    public val primitiveArrayConstructorNames: Set<String> = PrimitiveType.values().map { it.arrayTypeName.asString() }.toSet()
}

public object KotlinListUtils {
    public fun isKotlinList(type: JetType): Boolean =
        if (type.constructor is TypeConstructor) {
            type.constructor.declarationDescriptor?.let {
                val typeName = it.fqNameSafe.asString()
                typeName == "kotlin.List" || typeName == "java.util.ArrayList"
            } ?: false
        }
        else false
    // Creation
    public val listOfFunctionName: String = "listOf"
    public val arrayListOfFunctionName: String = "arrayListOf"

    // Methods
    public val addMethodName: String = "add"
    public val addAllMethodName: String = "addAll"
    public val clearMethodName: String = "clear"
}

public object KotlinCollectionsUtils {
    // Methods
    public val sizeMethodName: String = "size"
    public val getMethodName: String = "get"
    public val setMethodName: String = "set"
}

public interface PseudoAnnotation
public object ConstructorWithElementsAsArgs : PseudoAnnotation
public data class ConstructorWithSizeAsArg(val sizeArgPosition: Int) : PseudoAnnotation
public data class IncrSizeByConstantNumberMethod(val increaseBy: Int) : PseudoAnnotation
public data class IncrSizeByPassedCollectionSizeMethod(val collectionArgPosition: Int) : PseudoAnnotation
public object DecrSizeToZeroMethod : PseudoAnnotation
public object SizeMethod : PseudoAnnotation
public object GetMethod : PseudoAnnotation
public object SetMethod : PseudoAnnotation
public object AccessOperator : PseudoAnnotation

public object CallInstructionUtils {
    public interface CallInfo
    public data class CallSignature (
            val calledName: String?,
            val receiverType: JetType?,
            val returnType: JetType?
    ): CallInfo

    public data class RawInfo (val callInstruction: CallInstruction): CallInfo

    public fun tryExtractPseudoAnnotationForCollector(instruction: CallInstruction): PseudoAnnotation? =
        tryExtractPseudoAnnotation(instruction, collectorPseudoAnnotationExtractors)

    public fun tryExtractPseudoAnnotationForAccess(instruction: CallInstruction): PseudoAnnotation? {
        val accessOperation = accessOperatorChecker(RawInfo(instruction))
        if (accessOperation != null) {
            return accessOperation
        }
        return tryExtractPseudoAnnotation(instruction, accessPseudoAnnotationExtractors)
    }

    private fun tryExtractPseudoAnnotation(
            instruction: CallInstruction,
            extractors: List<(CallInstructionUtils.CallInfo) -> PseudoAnnotation?>
    ): PseudoAnnotation? {
        val callInfo = tryExtractCallInfo(instruction)
        if(callInfo != null) {
            for (extractor in extractors) {
                val res = extractor(callInfo)
                if (res != null) {
                    return res
                }
            }
        }
        return null
    }

    private val collectorPseudoAnnotationExtractors: List<(CallInstructionUtils.CallInfo) -> PseudoAnnotation?> = listOf(
            { info -> arrayOfElementsCreationFunctionChecker(info, KotlinArrayUtils.arrayOfFunctionName) },
            { info -> listOfElementsCreationFunctionChecker(info, KotlinListUtils.listOfFunctionName) },
            { info -> listOfElementsCreationFunctionChecker(info, KotlinListUtils.arrayListOfFunctionName) },
            { info -> arrayConstructorChecker(info) },
            { info -> primitiveArrayConstructorChecker(info) },
            { info -> sizeMethodChecker(info)},
            { info -> addMethodChecker(info) },
            { info -> addAllMethodChecker(info)},
            { info -> clearMethodChecker(info) }
    )

    private val accessPseudoAnnotationExtractors: List<(CallInstructionUtils.CallInfo) -> PseudoAnnotation?> = listOf(
            { info -> checkCollectionAccessMethod(info, { it == KotlinCollectionsUtils.getMethodName }, GetMethod) },
            { info -> checkCollectionAccessMethod(info, { it == KotlinCollectionsUtils.setMethodName }, SetMethod) }
    )

    private fun tryExtractCallInfo(instruction: CallInstruction): CallInfo? =
            if (instruction.element is JetCallExpression) {
                val calledName = instruction.element.calleeExpression?.node?.text
                val receiverType = tryExtractReceiverType(instruction)
                val returnType = instruction.resolvedCall.candidateDescriptor.returnType
                CallSignature(calledName, receiverType, returnType)
            }
            else null

    private fun tryExtractReceiverType(instruction: CallInstruction): JetType? {
        val receiver = when (instruction.resolvedCall.explicitReceiverKind) {
            ExplicitReceiverKind.DISPATCH_RECEIVER -> instruction.resolvedCall.dispatchReceiver
            ExplicitReceiverKind.EXTENSION_RECEIVER -> instruction.resolvedCall.extensionReceiver
            else -> null
        }
        return if (receiver is ExpressionReceiver) receiver.type else null
    }

    private fun arrayOfElementsCreationFunctionChecker(callInfo: CallInfo, functionName: String): PseudoAnnotation? =
            checkCollectionCreationFunction(
                    callInfo,
                    { it == functionName },
                    { KotlinBuiltIns.isArray(it) },
                    ConstructorWithElementsAsArgs
            )

    private fun listOfElementsCreationFunctionChecker(callInfo: CallInfo, functionName: String): PseudoAnnotation? =
            checkCollectionCreationFunction(
                    callInfo,
                    { it == functionName },
                    { KotlinListUtils.isKotlinList(it) },
                    ConstructorWithElementsAsArgs
            )

    private fun arrayConstructorChecker(callInfo: CallInfo): PseudoAnnotation? =
            checkCollectionCreationFunction(
                    callInfo,
                    { it == KotlinArrayUtils.arrayConstructorName },
                    { KotlinBuiltIns.isArray(it) },
                    ConstructorWithSizeAsArg(0)
            )

    private fun primitiveArrayConstructorChecker(callInfo: CallInfo): PseudoAnnotation? =
            checkCollectionCreationFunction(
                    callInfo,
                    { it in KotlinArrayUtils.primitiveArrayConstructorNames },
                    { KotlinBuiltIns.isPrimitiveArray(it) },
                    ConstructorWithSizeAsArg(0)
            )

    private fun sizeMethodChecker(callInfo: CallInstructionUtils.CallInfo): PseudoAnnotation? =
            checkMethodCallOnCollection(
                    callInfo,
                    { it == KotlinCollectionsUtils.sizeMethodName },
                    { KotlinBuiltIns.isInt(it) },
                    SizeMethod
            )

    private fun addMethodChecker(callInfo: CallInstructionUtils.CallInfo): PseudoAnnotation? =
            checkMethodCallOnCollection(
                    callInfo,
                    { it == KotlinListUtils.addMethodName },
                    { KotlinBuiltIns.isBoolean(it) || KotlinBuiltIns.isUnit(it) },
                    IncrSizeByConstantNumberMethod(1)
            )

    private fun addAllMethodChecker(callInfo: CallInstructionUtils.CallInfo): PseudoAnnotation? =
            checkMethodCallOnCollection(
                    callInfo,
                    { it == KotlinListUtils.addAllMethodName },
                    { KotlinBuiltIns.isBoolean(it) || KotlinBuiltIns.isUnit(it) },
                    IncrSizeByPassedCollectionSizeMethod(0) // todo: method addAll(index, collection) should be tested
            )

    private fun clearMethodChecker(callInfo: CallInstructionUtils.CallInfo): PseudoAnnotation? =
            checkMethodCallOnCollection(
                    callInfo,
                    { it == KotlinListUtils.clearMethodName },
                    { KotlinBuiltIns.isUnit(it) },
                    DecrSizeToZeroMethod
            )

    private inline fun checkCollectionCreationFunction(
            callInfo: CallInstructionUtils.CallInfo,
            isExpectedFunctionName: (String) -> Boolean,
            isExpectedReturnType: (JetType) -> Boolean,
            onSuccess: PseudoAnnotation
    ): PseudoAnnotation? =
        if (callInfo is CallInstructionUtils.CallSignature &&
            callInfo.calledName != null && isExpectedFunctionName(callInfo.calledName) &&
            callInfo.returnType != null && isExpectedReturnType(callInfo.returnType)
        ) {
            onSuccess
        }
        else null

    private inline fun checkMethodCallOnCollection(
            callInfo: CallInstructionUtils.CallInfo,
            isExpectedMethodName: (String) -> Boolean,
            isExpectedReturnType: (JetType) -> Boolean,
            onSuccess: PseudoAnnotation
    ): PseudoAnnotation? =
            if (callInfo is CallInstructionUtils.CallSignature &&
                callInfo.calledName != null && isExpectedMethodName(callInfo.calledName) &&
                callInfo.receiverType != null && receiverIsCollection(callInfo.receiverType) &&
                callInfo.returnType != null && isExpectedReturnType(callInfo.returnType)
            ) {
                onSuccess
            }
            else null

    private fun checkCollectionAccessMethod(
            callInfo: CallInstructionUtils.CallInfo,
            isExpectedMethodName: (String) -> Boolean,
            onSuccess: PseudoAnnotation
    ): PseudoAnnotation? =
            if (callInfo is CallInstructionUtils.CallSignature &&
                callInfo.calledName != null && isExpectedMethodName(callInfo.calledName) &&
                callInfo.receiverType != null && receiverIsCollection(callInfo.receiverType)
            ) {
                onSuccess
            }
            else null

    private fun accessOperatorChecker(rawInfo: RawInfo): PseudoAnnotation? {
        val isAccessOperation = rawInfo.callInstruction.element is JetArrayAccessExpression ||
                                rawInfo.callInstruction.element is JetBinaryExpression &&
                                rawInfo.callInstruction.element.left is JetArrayAccessExpression
        val receiverType = tryExtractReceiverType(rawInfo.callInstruction)
        if (isAccessOperation && receiverType != null && receiverIsCollection(receiverType)) {
            return AccessOperator
        }
        else return null
    }

    private fun receiverIsCollection(receiverType: JetType): Boolean =
            (KotlinArrayUtils.isGenericOrPrimitiveArray(receiverType) || KotlinListUtils.isKotlinList(receiverType))
}