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

import java.util.ArrayList
import java.util.Collections

class ParametersBuilder {

    private val params = ArrayList<ParameterInfo>()
    private val capturedParams = ArrayList<CapturedParamInfo>()

    var nextValueParameterIndex = 0
        private set
    private var nextCaptured = 0

    fun addThis(type: Type, skipped: Boolean): ParameterInfo {
        val info = ParameterInfo(type, skipped, nextValueParameterIndex, -1)
        addParameter(info)
        return info
    }

    fun addNextParameter(type: Type, skipped: Boolean, remapValue: StackValue?): ParameterInfo {
        return addParameter(ParameterInfo(type, skipped, nextValueParameterIndex, remapValue))
    }

    fun addCapturedParam(
            original: CapturedParamInfo,
            newFieldName: String): CapturedParamInfo {
        val info = CapturedParamInfo(original.desc, newFieldName, original.isSkipped, nextCaptured, original.getIndex())
        info.setLambda(original.getLambda())
        return addCapturedParameter(info)
    }

    fun addCapturedParam(
            desc: CapturedParamDesc,
            newFieldName: String): CapturedParamInfo {
        val info = CapturedParamInfo(desc, newFieldName, false, nextCaptured, null)
        return addCapturedParameter(info)
    }

    fun addCapturedParamCopy(
            copyFrom: CapturedParamInfo): CapturedParamInfo {
        val info = copyFrom.newIndex(nextCaptured)
        return addCapturedParameter(info)
    }

    fun addCapturedParam(
            containingLambda: CapturedParamOwner,
            fieldName: String,
            type: Type,
            skipped: Boolean,
            original: ParameterInfo?): CapturedParamInfo {
        val info = CapturedParamInfo(CapturedParamDesc.createDesc(containingLambda, fieldName, type), skipped, nextCaptured,
                                     if (original != null) original.getIndex() else -1)
        if (original != null) {
            info.setLambda(original.getLambda())
        }
        return addCapturedParameter(info)
    }

    private fun addParameter(info: ParameterInfo): ParameterInfo {
        params.add(info)
        nextValueParameterIndex += info.getType().size
        return info
    }

    private fun addCapturedParameter(info: CapturedParamInfo): CapturedParamInfo {
        capturedParams.add(info)
        nextCaptured += info.getType().size
        return info
    }

    fun listNotCaptured(): List<ParameterInfo> {
        return Collections.unmodifiableList(params)
    }

    fun listCaptured(): List<CapturedParamInfo> {
        return Collections.unmodifiableList(capturedParams)
    }

    fun listAllParams(): List<ParameterInfo> {
        val list = ArrayList(params)
        list.addAll(capturedParams)
        return list
    }

    private fun buildWithStubs(): List<ParameterInfo> {
        return Parameters.addStubs(listNotCaptured())
    }

    private fun buildCapturedWithStubs(): List<CapturedParamInfo> {
        return Parameters.shiftAndAddStubs(listCaptured(), nextValueParameterIndex)
    }

    fun buildParameters(): Parameters {
        return Parameters(buildWithStubs(), buildCapturedWithStubs())
    }

    companion object {

        @JvmStatic fun newBuilder(): ParametersBuilder {
            return ParametersBuilder()
        }

        @JvmOverloads @JvmStatic fun initializeBuilderFrom(objectType: Type, descriptor: String, inlineLambda: LambdaInfo? = null): ParametersBuilder {
            val builder = newBuilder()
            //skipped this for inlined lambda cause it will be removed
            builder.addThis(objectType, inlineLambda != null).setLambda(inlineLambda)

            val types = Type.getArgumentTypes(descriptor)
            for (type in types) {
                builder.addNextParameter(type, false, null)
            }
            return builder
        }
    }
}
