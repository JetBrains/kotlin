/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

    fun addNextParameter(type: Type, skipped: Boolean, typeOnStack: Type = type): ParameterInfo {
        return addParameter(ParameterInfo(type, skipped, nextParameterOffset, null, nextValueParameterIndex, typeOnStack))
    }

    fun addNextValueParameter(type: Type, skipped: Boolean, remapValue: StackValue?, parameterIndex: Int): ParameterInfo {
        return addParameter(
            ParameterInfo(
                type, skipped, nextParameterOffset, remapValue,
                if (parameterIndex == -1) nextValueParameterIndex else parameterIndex + valueParamFirstIndex
            )
        )
    }

    fun addCapturedParam(original: CapturedParamInfo, newFieldName: String): CapturedParamInfo {
        return addParameter(CapturedParamInfo(original.desc, newFieldName, original.isSkipped, -1, original.index)).apply {
            functionalArgument = original.functionalArgument
        }
    }

    fun addCapturedParam(desc: CapturedParamDesc, newFieldName: String, skipInConstructor: Boolean): CapturedParamInfo {
        return addParameter(CapturedParamInfo(desc, newFieldName, false, -1, null, skipInConstructor, -1))
    }

    fun addCapturedParamCopy(copyFrom: CapturedParamInfo): CapturedParamInfo {
        return addParameter(copyFrom.cloneWithIndices(-1, -1))
    }

    fun addCapturedParam(
        containingLambdaType: Type,
        fieldName: String,
        newFieldName: String,
        type: Type,
        skipped: Boolean,
        original: ParameterInfo?
    ): CapturedParamInfo {
        val desc = CapturedParamDesc(containingLambdaType, fieldName, type)
        return addParameter(CapturedParamInfo(desc, newFieldName, skipped, -1, original?.index ?: -1)).apply {
            functionalArgument = original?.functionalArgument
        }
    }

    private fun <T : ParameterInfo> addParameter(info: T): T {
        params.add(info)
        if (info !is CapturedParamInfo) {
            nextParameterOffset += info.type.size
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
        var nextDeclarationIndex = (params.maxOfOrNull { it.declarationIndex } ?: -1) + 1
        var nextOffset = nextParameterOffset
        return Parameters(params.map { param ->
            if (param is CapturedParamInfo) {
                param.cloneWithIndices(nextOffset.also { nextOffset += param.type.size }, nextDeclarationIndex++)
            } else {
                param
            }
        })
    }

    companion object {
        @JvmStatic
        fun newBuilder(): ParametersBuilder {
            return ParametersBuilder()
        }
    }
}
