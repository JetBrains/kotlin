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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.completion.handlers.*
import org.jetbrains.kotlin.idea.util.fuzzyReturnType
import org.jetbrains.kotlin.types.JetType
import java.util.*

class InsertHandlerProvider(expectedInfosCalculator: () -> Collection<ExpectedInfo>) {
    private val expectedInfos by lazy { expectedInfosCalculator() }

    public fun insertHandler(descriptor: DeclarationDescriptor): InsertHandler<LookupElement> {
        return when (descriptor) {
            is FunctionDescriptor -> {
                val needTypeArguments = needTypeArguments(descriptor)
                val parameters = descriptor.valueParameters
                when (parameters.size()) {
                    0 -> KotlinFunctionInsertHandler(needTypeArguments, inputValueArguments = false)

                    1 -> {
                        val parameterType = parameters.single().getType()
                        if (KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(parameterType)) {
                            val parameterCount = KotlinBuiltIns.getParameterTypeProjectionsFromFunctionType(parameterType).size()
                            if (parameterCount <= 1) {
                                // otherwise additional item with lambda template is to be added
                                return KotlinFunctionInsertHandler(needTypeArguments, inputValueArguments = false, lambdaInfo = GenerateLambdaInfo(parameterType, false))
                            }
                        }
                        KotlinFunctionInsertHandler(needTypeArguments, inputValueArguments = true)
                    }

                    else -> KotlinFunctionInsertHandler(needTypeArguments, inputValueArguments = true)
                }
            }

            is PropertyDescriptor -> KotlinPropertyInsertHandler

            is ClassifierDescriptor -> KotlinClassifierInsertHandler

            else -> BaseDeclarationInsertHandler()
        }
    }

    private fun needTypeArguments(function: FunctionDescriptor): Boolean {
        if (function.typeParameters.isEmpty()) return false

        val originalFunction = function.original
        val typeParameters = originalFunction.typeParameters

        val potentiallyInferred = HashSet<TypeParameterDescriptor>()

        fun addPotentiallyInferred(type: JetType) {
            val descriptor = type.constructor.declarationDescriptor as? TypeParameterDescriptor
            if (descriptor != null && descriptor in typeParameters) {
                potentiallyInferred.add(descriptor)
            }

            if (KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(type) && KotlinBuiltIns.getParameterTypeProjectionsFromFunctionType(type).size() <= 1) {
                // do not rely on inference from input of function type with one or no arguments - use only return type of functional type
                addPotentiallyInferred(KotlinBuiltIns.getReturnTypeFromFunctionType(type))
                return
            }

            for (argument in type.arguments) {
                if (!argument.isStarProjection) { // otherwise we can fall into infinite recursion
                    addPotentiallyInferred(argument.type)
                }
            }
        }

        originalFunction.extensionReceiverParameter?.type?.let { addPotentiallyInferred(it) }
        originalFunction.valueParameters.forEach { addPotentiallyInferred(it.type) }

        fun allTypeParametersPotentiallyInferred() = originalFunction.typeParameters.all { it in potentiallyInferred }

        if (allTypeParametersPotentiallyInferred()) return false

        val returnType = originalFunction.returnType
        // check that there is an expected type and return value from the function can potentially match it
        if (returnType != null) {
            addPotentiallyInferred(returnType)

            if (allTypeParametersPotentiallyInferred() && expectedInfos.any { it.fuzzyType?.checkIsSuperTypeOf(originalFunction.fuzzyReturnType()!!) != null }) {
                return false
            }
        }

        return true
    }
}