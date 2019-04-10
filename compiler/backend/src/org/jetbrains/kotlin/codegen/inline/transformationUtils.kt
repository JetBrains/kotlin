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

import com.intellij.openapi.util.Pair
import org.jetbrains.kotlin.codegen.FieldInfo
import org.jetbrains.org.objectweb.asm.Type

class NewJavaField(val name: String, val type: Type, val skip: Boolean)

fun getNewFieldsToGenerate(params: List<CapturedParamInfo>): List<NewJavaField> {
    return params.filter {
        //not inlined
        it.lambda == null
    }.map {
        NewJavaField(it.newFieldName, it.type, it.isSkipInConstructor)
    }
}

fun transformToFieldInfo(lambdaType: Type, newFields: List<NewJavaField>): List<FieldInfo> {
    return newFields.map { field ->
        FieldInfo.createForHiddenField(lambdaType, field.type, field.name)
    }
}

fun filterSkipped(fields: List<NewJavaField>): List<NewJavaField> {
    return fields.filter { !it.skip }
}

fun toNameTypePair(fields: List<NewJavaField>): List<Pair<String, Type>> = fields.map { Pair(it.name, it.type) }
