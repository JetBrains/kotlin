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

class CapturedParamInfo : ParameterInfo {
    val desc: CapturedParamDesc
    val newFieldName: String
    val isSkipInConstructor: Boolean

    //Now used only for bound function reference receiver
    var isSynthetic: Boolean = false

    val originalFieldName: String
        get() = desc.fieldName

    val containingLambdaName: String
        get() = desc.containingLambdaName

    constructor(desc: CapturedParamDesc, newFieldName: String, skipped: Boolean, index: Int, remapIndex: Int) : super(
        desc.type,
        skipped,
        index,
        remapIndex,
        -1
    ) {
        this.desc = desc
        this.newFieldName = newFieldName
        this.isSkipInConstructor = false
    }

    constructor(
        desc: CapturedParamDesc,
        newFieldName: String,
        skipped: Boolean,
        index: Int,
        remapIndex: StackValue?,
        skipInConstructor: Boolean,
        declarationIndex: Int
    ) : super(desc.type, skipped, index, remapIndex, declarationIndex) {
        this.desc = desc
        this.newFieldName = newFieldName
        this.isSkipInConstructor = skipInConstructor
    }

    fun cloneWithNewDeclarationIndex(newDeclarationIndex: Int): CapturedParamInfo {
        val result = CapturedParamInfo(
            desc, newFieldName, isSkipped, index, remapValue, isSkipInConstructor, newDeclarationIndex
        )
        result.functionalArgument = functionalArgument
        result.isSynthetic = isSynthetic
        return result
    }

    companion object {

        fun isSynthetic(info: ParameterInfo): Boolean {
            return info is CapturedParamInfo && info.isSynthetic
        }
    }
}
