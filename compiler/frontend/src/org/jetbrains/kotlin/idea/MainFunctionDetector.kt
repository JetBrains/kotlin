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

package org.jetbrains.kotlin.idea

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.types.Variance

class MainFunctionDetector {
    private val getFunctionDescriptor: (KtNamedFunction) -> FunctionDescriptor

    /** Assumes that the function declaration is already resolved and the descriptor can be found in the `bindingContext`.  */
    constructor(bindingContext: BindingContext) {
        this.getFunctionDescriptor = { function ->
            bindingContext.get(BindingContext.FUNCTION, function) ?: throw IllegalStateException("No descriptor resolved for " + function + " " + function.text)
        }
    }

    constructor(functionResolver: (KtNamedFunction) -> FunctionDescriptor) {
        this.getFunctionDescriptor = functionResolver
    }

    fun hasMain(declarations: List<KtDeclaration>): Boolean {
        return findMainFunction(declarations) != null
    }

    fun isMain(function: KtNamedFunction): Boolean {
        if (function.isLocal) {
            return false
        }

        if (function.valueParameters.size != 1 || !function.typeParameters.isEmpty()) {
            return false
        }

        /* Psi only check for kotlin.jvm.jvmName annotation */
        if ("main" != function.name && !hasAnnotationWithExactNumberOfArguments(function, 1)) {
            return false
        }

        /* Psi only check for kotlin.jvm.jvmStatic annotation */
        if (!function.isTopLevel && !hasAnnotationWithExactNumberOfArguments(function, 0)) {
            return false
        }

        return isMain(getFunctionDescriptor(function))
    }

    fun getMainFunction(files: Collection<KtFile>): KtNamedFunction? {
        for (file in files) {
            val mainFunction = findMainFunction(file.declarations)
            if (mainFunction != null) {
                return mainFunction
            }
        }
        return null
    }

    private fun findMainFunction(declarations: List<KtDeclaration>): KtNamedFunction? {
        for (declaration in declarations) {
            if (declaration is KtNamedFunction) {
                if (isMain(declaration)) {
                    return declaration
                }
            }
        }
        return null
    }

    companion object {

        fun isMain(descriptor: DeclarationDescriptor): Boolean {
            if (descriptor !is FunctionDescriptor) return false

            if (getJVMFunctionName(descriptor) != "main") {
                return false
            }

            val parameters = descriptor.valueParameters
            if (parameters.size != 1 || !descriptor.typeParameters.isEmpty()) return false

            val parameter = parameters[0]
            val parameterType = parameter.type
            if (!KotlinBuiltIns.isArray(parameterType)) return false

            val typeArguments = parameterType.arguments
            if (typeArguments.size != 1) return false

            val typeArgument = typeArguments[0].type
            if (!KotlinBuiltIns.isString(typeArgument)) {
                return false
            }
            if (typeArguments[0].projectionKind === Variance.IN_VARIANCE) {
                return false
            }

            if (DescriptorUtils.isTopLevelDeclaration(descriptor)) return true

            val containingDeclaration = descriptor.containingDeclaration
            return containingDeclaration is ClassDescriptor
                   && containingDeclaration.kind.isSingleton
                   && descriptor.hasJvmStaticAnnotation()
        }

        private fun getJVMFunctionName(functionDescriptor: FunctionDescriptor): String {
            val platformName = DescriptorUtils.getJvmName(functionDescriptor)
            if (platformName != null) {
                return platformName
            }

            return functionDescriptor.name.asString()
        }

        private fun hasAnnotationWithExactNumberOfArguments(function: KtNamedFunction, number: Int): Boolean {
            for (entry in function.annotationEntries) {
                if (entry.valueArguments.size == number) {
                    return true
                }
            }

            return false
        }
    }
}
