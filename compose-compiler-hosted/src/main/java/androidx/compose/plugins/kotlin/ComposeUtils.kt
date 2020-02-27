/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.descriptorUtil.module

object ComposeUtils {

    fun generateComposePackageName() = "androidx.compose"

    fun composeFqName(cname: String) = FqName("${generateComposePackageName()}.$cname")

    fun composeInternalFqName(cname: String? = null) = FqName(
        "${generateComposePackageName()}.internal${cname?.let { ".$it"} ?: ""}"
    )

    fun setterMethodFromPropertyName(name: String): String {
        return "set${name[0].toUpperCase()}${name.slice(1 until name.length)}"
    }

    fun propertyNameFromSetterMethod(name: String): String {
        return if (name.startsWith("set")) "${
            name[3].toLowerCase()
        }${name.slice(4 until name.length)}" else name
    }

    fun isSetterMethodName(name: String): Boolean {
        // use !lower to capture non-alpha chars
        return name.startsWith("set") && name.length > 3 && !name[3].isLowerCase()
    }

    fun isComposeComponent(descriptor: DeclarationDescriptor): Boolean {
        if (descriptor !is ClassDescriptor) return false
        val baseComponentDescriptor =
            descriptor.module.findClassAcrossModuleDependencies(
                ClassId.topLevel(
                    FqName(ComposeUtils.generateComposePackageName() + ".Component")
                )
            ) ?: return false
        return descriptor.isSubclassOf(baseComponentDescriptor)
    }
}

fun KtFunction.isEmitInline(bindingContext: BindingContext): Boolean {
    if (this !is KtFunctionLiteral) return false
    if (parent?.parent !is KtLambdaArgument) return false
    val call = parent?.parent?.parent as? KtCallExpression
    val resolvedCall = call?.getResolvedCall(bindingContext)
    return resolvedCall != null &&
            resolvedCall.candidateDescriptor is ComposableEmitDescriptor
}