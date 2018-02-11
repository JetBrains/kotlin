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

class ParametersBuilder private constructor() {

    private val params = arrayListOf<ParameterInfo>()

    private var valueParamFirstIndex = 0

    var nextParameterOffset = 0
        private set

    private var nextValueParameterIndex = 0

    fun addThis(type: Type, skipped: Boolean): ParameterInfo {
        return addParameter(ParameterInfo(type, skipped, nextParameterOffset, -1, nextValueParameterIndex))
    }

    fun addNextParameter(type: Type, skipped: Boolean): ParameterInfo {
        return addParameter(ParameterInfo(type, skipped, nextParameterOffset, null, nextValueParameterIndex))
    }

    fun addNextValueParameter(type: Type, skipped: Boolean, remapValue: StackValue?, parameterIndex: Int): ParameterInfo {
        return addParameter(ParameterInfo(
                type, skipped, nextParameterOffset, remapValue,
                if (parameterIndex == -1) nextValueParameterIndex else parameterIndex + valueParamFirstIndex
        ))
    }

    fun addCapturedParam(original: CapturedParamInfo, newFieldName: String): CapturedParamInfo {
        val info = CapturedParamInfo(original.desc, newFieldName, original.isSkipped, nextParameterOffset, original.index)
        info.lambda = original.lambda
        return addParameter(info)
    }

    fun addCapturedParam(desc: CapturedParamDesc, newFieldName: String, skipInConstructor: Boolean): CapturedParamInfo {
        return addParameter(CapturedParamInfo(desc, newFieldName, false, nextParameterOffset, null, skipInConstructor, -1))
    }

    fun addCapturedParamCopy(copyFrom: CapturedParamInfo): CapturedParamInfo {
        return addParameter(copyFrom.cloneWithNewDeclarationIndex(-1))
    }

    fun addCapturedParam(
            containingLambdaType: Type,
            fieldName: String,
            newFieldName: String,
            type: Type,
            skipped: Boolean,
            original: ParameterInfo?
    ): CapturedParamInfo {
        val info = CapturedParamInfo(
                CapturedParamDesc(containingLambdaType, fieldName, type), newFieldName, skipped, nextParameterOffset, original?.index ?: -1
        )
        if (original != null) {
            info.lambda = original.lambda
        }
        return addParameter(info)
    }

    private fun <T: ParameterInfo> addParameter(info: T): T {
        params.add(info)
        nextParameterOffset += info.getType().size
        if (info !is CapturedParamInfo) {
            nextValueParameterIndex++
        }
        return info
    }

    fun markValueParametersStart() {
        this.valueParamFirstIndex = params.size
        this.nextValueParameterIndex = valueParamFirstIndex
    }

    fun listCaptured(): List<CapturedParamInfo> {
        return params.filterIsInstance<CapturedParamInfo>()
    }

    /*TODO use Parameters instead*/
    fun listAllParams(): List<ParameterInfo> {
        return params
    }

    fun buildParameters(): Parameters {
        var nextDeclarationIndex = (params.maxBy { it.declarationIndex }?.declarationIndex ?: -1) + 1

        return Parameters(params.map { param ->
            if (param is CapturedParamInfo) {
                param.cloneWithNewDeclarationIndex(nextDeclarationIndex++)
            }
            else {
                param
            }
        })
    }

    companion object {
        @JvmStatic
        fun newBuilder(): ParametersBuilder {
            return ParametersBuilder()
        }

        @JvmOverloads
        @JvmStatic
        fun initializeBuilderFrom(
                objectType: Type, descriptor: String, inlineLambda: LambdaInfo? = null
        ): ParametersBuilder {
            val builder = newBuilder()
            if (inlineLambda?.hasDispatchReceiver != false) {
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
