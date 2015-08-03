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
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.types.JetType

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

public object JetExpressionUtils {
    public fun tryGetCalledName(callExpression: JetCallExpression): String? =
            callExpression.calleeExpression?.node?.text
}

public object InstructionUtils {
    public fun isExpectedReturnType(instruction: CallInstruction, isExpectedType: (JetType) -> Boolean): Boolean {
        return instruction.resolvedCall
                       .candidateDescriptor
                       .returnType
                       ?.let { isExpectedType(it) }
               ?: false
    }
}

public object KotlinArrayUtils {
    public fun isGenericOrPrimitiveArray(type: JetType): Boolean =
            KotlinBuiltIns.isArray(type) || KotlinBuiltIns.isPrimitiveArray(type)

    public fun receiverIsArray(instruction: CallInstruction): Boolean {
        val callReceiver = instruction.resolvedCall.dispatchReceiver
        return if (callReceiver is ExpressionReceiver)
            KotlinArrayUtils.isGenericOrPrimitiveArray(callReceiver.type)
        else false
    }

    // Array creation
    public val arrayOfFunctionName: String = "arrayOf"
    public val arrayConstructorName: String = "Array"
    public val primitiveArrayConstructorNames: Set<String> = PrimitiveType.values().map { it.arrayTypeName.asString() }.toSet()

    // Array methods
    public val sizeMethodNameOfArray: String = "size"
}