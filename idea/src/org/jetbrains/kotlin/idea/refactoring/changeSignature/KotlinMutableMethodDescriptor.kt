/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import org.jetbrains.kotlin.descriptors.Visibility

class KotlinMutableMethodDescriptor(override val original: KotlinMethodDescriptor) : KotlinMethodDescriptor by original {
    private val parameters: MutableList<KotlinParameterInfo> = original.parameters

    override var receiver: KotlinParameterInfo? = original.receiver
        set(value: KotlinParameterInfo?) {
            if (value != null && value !in parameters) {
                parameters.add(value)
            }
            field = value
        }

    fun addParameter(parameter: KotlinParameterInfo) {
        parameters.add(parameter)
    }

    fun addParameter(index: Int, parameter: KotlinParameterInfo) {
        parameters.add(index, parameter)
    }

    fun removeParameter(index: Int) {
        val paramInfo = parameters.removeAt(index)
        if (paramInfo == receiver) {
            receiver = null
        }
    }

    fun renameParameter(index: Int, newName: String) {
        parameters[index].name = newName
    }

    fun clearNonReceiverParameters() {
        parameters.clear()
        receiver?.let { parameters.add(it) }
    }

    override fun getVisibility(): Visibility? {
        return original.visibility
    }
}