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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import org.jetbrains.kotlin.descriptors.Visibility

public class KotlinMutableMethodDescriptor(override val original: KotlinMethodDescriptor): KotlinMethodDescriptor by original {
    private val parameters: MutableList<KotlinParameterInfo> = original.getParameters()

    override var receiver: KotlinParameterInfo? = original.receiver
        set(value: KotlinParameterInfo?) {
            if (value != null && value !in parameters) {
                parameters.add(value)
            }
            field = value
        }

    public fun addParameter(parameter: KotlinParameterInfo) {
        parameters.add(parameter)
    }

    public fun removeParameter(index: Int) {
        val paramInfo = parameters.remove(index)
        if (paramInfo == receiver) {
            receiver = null
        }
    }

    public fun renameParameter(index: Int, newName: String) {
        parameters[index].setName(newName)
    }

    public fun clearParameters() {
        parameters.clear()
        receiver = null
    }

    public fun clearNonReceiverParameters() {
        parameters.clear()
        receiver?.let { parameters.add(it) }
    }

    override fun getVisibility(): Visibility? {
        return original.getVisibility()
    }
}