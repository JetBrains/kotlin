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

import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.org.objectweb.asm.Type
import java.util.*

internal class ParametersBuilder private constructor() {
    private val valueAndHiddenParams = arrayListOf<ParameterInfo>()
    private val capturedParams = arrayListOf<CapturedParamInfo>()
    private var valueParamStart = 0

    var nextValueParameterIndex = 0
        private set

    private var nextCaptured = 0

    fun addThis(type: Type, skipped: Boolean): ParameterInfo {
        val info = ParameterInfo(type, skipped, nextValueParameterIndex, -1, valueAndHiddenParams.size)
        addParameter(info)
        return info
    }

    fun addNextParameter(type: Type, skipped: Boolean): ParameterInfo {
        return addParameter(ParameterInfo(type, skipped, nextValueParameterIndex, null, valueAndHiddenParams.size))
    }

    fun addNextValueParameter(type: Type, skipped: Boolean, remapValue: StackValue?, parameterIndex: Int): ParameterInfo {
        return addParameter(ParameterInfo(
                type, skipped, nextValueParameterIndex, remapValue,
                if (parameterIndex == -1) valueAndHiddenParams.size else parameterIndex + valueParamStart
        ))
    }

    fun addCapturedParam(original: CapturedParamInfo, newFieldName: String): CapturedParamInfo {
        val info = CapturedParamInfo(original.desc, newFieldName, original.isSkipped, nextCapturedIndex(), original.index)
        info.lambda = original.lambda
        return addCapturedParameter(info)
    }

    private fun nextCapturedIndex(): Int = nextCaptured

    fun addCapturedParam(desc: CapturedParamDesc, newFieldName: String): CapturedParamInfo {
        return addCapturedParameter(CapturedParamInfo(desc, newFieldName, false, nextCapturedIndex(), null))
    }

    fun addCapturedParamCopy(copyFrom: CapturedParamInfo): CapturedParamInfo {
        return addCapturedParameter(copyFrom.newIndex(nextCapturedIndex()))
    }

    fun addCapturedParam(
            containingLambda: CapturedParamOwner,
            fieldName: String,
            newFieldName: String,
            type: Type,
            skipped: Boolean,
            original: ParameterInfo?
    ): CapturedParamInfo {
        val info = CapturedParamInfo(
                CapturedParamDesc(containingLambda, fieldName, type), newFieldName, skipped, nextCapturedIndex(), original?.index ?: -1
        )
        if (original != null) {
            info.lambda = original.lambda
        }
        return addCapturedParameter(info)
    }

    private fun addParameter(info: ParameterInfo): ParameterInfo {
        valueAndHiddenParams.add(info)
        nextValueParameterIndex += info.getType().size
        return info
    }

    private fun addCapturedParameter(info: CapturedParamInfo): CapturedParamInfo {
        capturedParams.add(info)
        nextCaptured += info.getType().size
        return info
    }

    fun markValueParametersStart() {
        this.valueParamStart = valueAndHiddenParams.size
    }

    fun listCaptured(): List<CapturedParamInfo> {
        return Collections.unmodifiableList(capturedParams)
    }

    /*TODO use Parameters instead*/
    fun listAllParams(): List<ParameterInfo> {
        return valueAndHiddenParams + capturedParams
    }

    fun buildParameters(): Parameters {
        return Parameters(Collections.unmodifiableList(valueAndHiddenParams), Parameters.shift(listCaptured(), nextValueParameterIndex))
    }

    companion object {
        @JvmStatic
        fun newBuilder(): ParametersBuilder {
            return ParametersBuilder()
        }

        @JvmOverloads
        @JvmStatic
        fun initializeBuilderFrom(
                objectType: Type, descriptor: String, inlineLambda: LambdaInfo? = null, addThis: Boolean = true
        ): ParametersBuilder {
            val builder = newBuilder()
            if (addThis) {
                //skipped this for inlined lambda cause it will be removed
                builder.addThis(objectType, inlineLambda != null).lambda = inlineLambda
            }

            for (type in Type.getArgumentTypes(descriptor)) {
                builder.addNextParameter(type, false)
            }
            return builder
        }
    }
}
