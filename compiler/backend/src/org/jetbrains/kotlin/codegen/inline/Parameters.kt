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

import org.jetbrains.org.objectweb.asm.Type
import java.util.*

class Parameters(val real: List<ParameterInfo>, val captured: List<CapturedParamInfo>) : Iterable<ParameterInfo> {

    private val actualDeclShifts: Array<ParameterInfo?>
    private val paramToDeclByteCodeIndex: HashMap<ParameterInfo, Int> = hashMapOf()

    public val realArgsSizeOnStack = real.sumBy { it.type.size }
    public val capturedArgsSizeOnStack = captured.sumBy { it.type.size }

    public val argsSizeOnStack = realArgsSizeOnStack + capturedArgsSizeOnStack

    init {
        val declIndexesToActual = arrayOfNulls<Int>(argsSizeOnStack)
        withIndex().forEach { it ->
            declIndexesToActual[it.value.declarationIndex] = it.index
        }

        actualDeclShifts = arrayOfNulls<ParameterInfo>(argsSizeOnStack)
        var realSize = 0
        for (i in declIndexesToActual.indices) {
            val byDeclarationIndex = get(declIndexesToActual[i] ?: continue)
            actualDeclShifts[realSize] = byDeclarationIndex
            paramToDeclByteCodeIndex.put(byDeclarationIndex, realSize)
            realSize += byDeclarationIndex.type.size
        }
    }

    fun getDeclarationSlot(info : ParameterInfo): Int {
        return paramToDeclByteCodeIndex[info]!!
    }

    fun getParameterByDeclarationSlot(index: Int): ParameterInfo {
        return actualDeclShifts[index]!!
    }

    private fun get(index: Int): ParameterInfo {
        if (index < real.size()) {
            return real.get(index)
        }
        return captured.get(index - real.size())
    }

    override fun iterator(): Iterator<ParameterInfo> {
        return (real + captured).iterator()
    }

    val capturedTypes: List<Type>
        get() = captured.map {
            it.getType()
        }

    companion object {
        fun shift(capturedParams: List<CapturedParamInfo>, realSize: Int): List<CapturedParamInfo> {
            return capturedParams.withIndex().map { it.value.newIndex(it.index+ realSize) }
        }
    }
}
