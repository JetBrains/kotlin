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
import org.jetbrains.kotlin.backend.js.util.buildJs
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.backend.ast.JsInvocation
import org.jetbrains.kotlin.js.backend.ast.JsNameRef
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.module

fun IrTranslationContext.translateAsValueReference(descriptor: DeclarationDescriptor): JsExpression {
    if (AnnotationsUtils.isNativeObject(descriptor) || AnnotationsUtils.isLibraryObject(descriptor)) {
        return getInnerReference(descriptor)
    }

    aliases[descriptor]?.let { return it }

    if (descriptor is ValueDescriptor) {
        return naming.names[descriptor].makeRef()
    }

    if (shouldTranslateAsFQN(descriptor)) {
        return naming.qualifiedReferences[descriptor]
    }

    if (descriptor is PropertyDescriptor) {
        val property = descriptor
        if (module == property.module) {
            return getInnerReference(property)
        }
        else {
            val qualifier = getInnerReference(property.containingDeclaration)
            val name = naming.names[property]
            return JsNameRef(name, qualifier)
        }
    }

    if (DescriptorUtils.isObject(descriptor) || DescriptorUtils.isEnumEntry(descriptor)) {
        if (module.descriptor != descriptor.module) {
            return getLazyReferenceToObject(descriptor as ClassDescriptor)
        }
        else {
            val functionRef = (naming.objectInnerNames[descriptor as ClassDescriptor])
            return JsInvocation(functionRef.makeRef())
        }
    }

    return getInnerReference(descriptor)
}

fun IrTranslationContext.translateAsTypeReference(descriptor: ClassDescriptor): JsExpression {
    if (AnnotationsUtils.isNativeObject(descriptor) || AnnotationsUtils.isLibraryObject(descriptor)) {
        return getInnerReference(descriptor)
    }
    if (!shouldTranslateAsFQN(descriptor)) {
        if (DescriptorUtils.isObject(descriptor) || DescriptorUtils.isEnumEntry(descriptor)) {
            if (module.descriptor != descriptor.module) {
                return getPrototypeIfNecessary(descriptor, getLazyReferenceToObject(descriptor))
            }
        }
        return getInnerReference(descriptor)
    }

    return getPrototypeIfNecessary(descriptor, naming.qualifiedReferences[descriptor])
}

private fun getPrototypeIfNecessary(descriptor: ClassDescriptor, reference: JsExpression): JsExpression =
        if (DescriptorUtils.isObject(descriptor) || DescriptorUtils.isEnumEntry(descriptor)) {
            buildJs { "Object".dotPure("getPrototypeOf").invoke(reference).pure() }
        }
        else {
            reference
        }

private fun IrTranslationContext.getLazyReferenceToObject(descriptor: ClassDescriptor): JsExpression {
    val container = descriptor.containingDeclaration
    val qualifier = getInnerReference(container)
    return JsNameRef(naming.names[descriptor], qualifier)
}

private fun IrTranslationContext.shouldTranslateAsFQN(descriptor: DeclarationDescriptor): Boolean =
        isLocalVarOrFunction(descriptor) || isPublicInlineFunction

private fun isLocalVarOrFunction(descriptor: DeclarationDescriptor): Boolean =
        descriptor.containingDeclaration is FunctionDescriptor && descriptor !is ClassDescriptor