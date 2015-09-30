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

package org.jetbrains.kotlin.codegen.inline

import com.google.common.collect.Iterables
import org.jetbrains.org.objectweb.asm.Type

import java.util.ArrayList

//All parameters with gaps
class Parameters(val real: List<ParameterInfo>, val captured: List<CapturedParamInfo>) : Iterable<ParameterInfo> {

    private val declIndexesToActual: Array<Int?>
    private val actualDeclShifts: Array<ParameterInfo?>

    public val realArgsSizeOnStack = real.fold(0, { a, v -> a + v.type.size})
    public val capturedArgsSizeOnStack = captured.fold(0, { a, v -> a + v.type.size})

    public val argsSizeOnStack = realArgsSizeOnStack + capturedArgsSizeOnStack

    init {
        declIndexesToActual = arrayOfNulls<Int>(argsSizeOnStack)
        withIndex().forEach { it ->
            declIndexesToActual[it.value.declarationIndex] = it.index
        }

        actualDeclShifts = arrayOfNulls<ParameterInfo>(argsSizeOnStack)
        var realSize = 0
        for (i in declIndexesToActual.indices) {
            val declIndexToActual = declIndexesToActual[i]
            if (declIndexToActual != null) {
                val byDeclarationIndex = getByDeclarationIndex(i)
                actualDeclShifts[realSize] = byDeclarationIndex
                realSize += byDeclarationIndex.type.size
            }
        }
    }

    fun getByDeclarationIndex(index: Int): ParameterInfo {
        if (index < realArgsSizeOnStack) {
            return real.get(declIndexesToActual[index]!!)
        }
        return captured.get(declIndexesToActual[index]!! - real.size())
    }

    fun getByByteCodeIndex(index: Int): ParameterInfo {
        return actualDeclShifts[index]!!
    }

    fun get(index: Int): ParameterInfo {
        if (index < real.size()) {
            return real.get(index)
        }
        return captured.get(index - real.size())
    }

    override fun iterator(): Iterator<ParameterInfo> {
        return Iterables.concat(real, captured).iterator()
    }

    val capturedTypes: ArrayList<Type>
        get() {
            val result = ArrayList<Type>()
            for (info in captured) {
                result.add(info.getType())
            }
            return result
        }

    companion object {
        fun shift(capturedParams: List<CapturedParamInfo>, realSize: Int): List<CapturedParamInfo> {
            val result = ArrayList<CapturedParamInfo>()
            for (capturedParamInfo in capturedParams) {
                val newInfo = capturedParamInfo.newIndex(result.size() + realSize)
                result.add(newInfo)
            }
            return result
        }
    }
}
