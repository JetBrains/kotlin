/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.js.expression

import org.jetbrains.kotlin.backend.js.context.IrTranslationContext
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.backend.ast.JsInvocation
import org.jetbrains.kotlin.js.backend.ast.metadata.descriptor
import org.jetbrains.kotlin.js.backend.ast.metadata.inlineStrategy
import org.jetbrains.kotlin.js.backend.ast.metadata.psiElement
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.FunctionImportedFromObject
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.inline.InlineStrategy
import org.jetbrains.kotlin.resolve.inline.InlineUtil

fun IrTranslationContext.shouldBeInlined(descriptor: CallableDescriptor): Boolean =
        !config.jsConfig.configuration.getBoolean(CommonConfigurationKeys.DISABLE_INLINE) && descriptorShouldBeInlined(descriptor)

fun descriptorShouldBeInlined(descriptor: CallableDescriptor): Boolean {
    if (descriptor is SimpleFunctionDescriptor ||
        descriptor is PropertyAccessorDescriptor ||
        descriptor is FunctionImportedFromObject) {
        return InlineUtil.isInline(descriptor)
    }

    return if (descriptor is ValueParameterDescriptor) {
        InlineUtil.isInline(descriptor.containingDeclaration) &&
        InlineUtil.isInlineParameter(descriptor as ParameterDescriptor) &&
        !descriptor.isCrossinline
    }
    else false
}

fun JsInvocation.withInlineMetadata(context: IrTranslationContext, descriptor: CallableDescriptor): JsInvocation {
    if (!context.shouldBeInlined(descriptor)) return this

    this.descriptor = descriptor
    inlineStrategy = InlineStrategy.IN_PLACE
    psiElement = psiElement
    val tag = getFunctionTag(descriptor)
    context.fragment.inlineModuleMap[tag] = context.naming.qualifiedReferences[descriptor.module]
    return this
}

fun getFunctionTag(functionDescriptor: CallableDescriptor): String {
    val descriptor = JsDescriptorUtils.findRealInlineDeclaration(functionDescriptor) as CallableDescriptor
    val moduleName = getModuleName(descriptor)
    val fqNameParent = DescriptorUtils.getFqName(descriptor).parent()
    var qualifier: String? = null

    if (!fqNameParent.isRoot) {
        qualifier = fqNameParent.asString()
    }

    val suggestedName = NameSuggestion().suggest(descriptor) ?: error("Suggested name can be null only for module descriptors: $descriptor")
    val mangledName = suggestedName.names[0]
    return listOf(moduleName, qualifier, mangledName).joinToString(".")
}

fun getModuleName(descriptor: DeclarationDescriptor): String {
    val moduleDescriptor = JsDescriptorUtils.findRealInlineDeclaration(descriptor).module
    if (descriptor.module === moduleDescriptor.builtIns.builtInsModule) return "kotlin"

    val moduleName = moduleDescriptor.name.asString()
    return moduleName.substring(1, moduleName.length - 1)
}