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

package org.jetbrains.kotlin.resolve.jvm.calls.checkers

import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.NO_REFLECTION_IN_CLASS_PATH
import org.jetbrains.kotlin.serialization.deserialization.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.storage.get
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * If there's no Kotlin reflection implementation found in the classpath, checks that there are no usages
 * of reflection API which will fail at runtime.
 */
class ReflectionAPICallChecker(private val module: ModuleDescriptor, storageManager: StorageManager) : CallChecker {
    private val isReflectionAvailable by storageManager.createLazyValue {
        module.findClassAcrossModuleDependencies(JvmAbi.REFLECTION_FACTORY_IMPL) != null
    }

    private val kPropertyClasses by storageManager.createLazyValue {
        val reflectionTypes = ReflectionTypes(module)
        setOf(reflectionTypes.kProperty0, reflectionTypes.kProperty1, reflectionTypes.kProperty2)
    }

    override fun <F : CallableDescriptor> check(resolvedCall: ResolvedCall<F>, context: BasicCallResolutionContext) {
        if (isReflectionAvailable) return

        val descriptor = resolvedCall.getResultingDescriptor()
        val containingClass = descriptor.getContainingDeclaration() as? ClassDescriptor ?: return
        if (!ReflectionTypes.isReflectionClass(containingClass)) return

        // Skip some symbols which are supposed to work fine without kotlin-reflect.jar:
        // - 'name' on anything
        // - 'invoke' on functions (or on anything else for that matter)
        // - 'get'/'set' on properties
        val name = descriptor.getName()
        when {
            name == OperatorNameConventions.INVOKE -> return
            name.asString() == "name" -> return
            (name.asString() == "get" || name.asString() == "set") &&
            kPropertyClasses.any { kProperty -> DescriptorUtils.isSubclass(containingClass, kProperty) } -> return
        }

        context.trace.report(NO_REFLECTION_IN_CLASS_PATH.on(resolvedCall.getCall().getCallElement()))
    }
}
