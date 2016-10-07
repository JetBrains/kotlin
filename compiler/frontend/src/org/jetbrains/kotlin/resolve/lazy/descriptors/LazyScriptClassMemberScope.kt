/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.lazy.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider

class LazyScriptClassMemberScope(
        resolveSession: ResolveSession,
        declarationProvider: ClassMemberDeclarationProvider,
        private val scriptDescriptor: LazyScriptDescriptor,
        trace: BindingTrace)
: LazyClassMemberScope(resolveSession, declarationProvider, scriptDescriptor, trace) {

    override fun resolvePrimaryConstructor(): ClassConstructorDescriptor? {
        val constructor = ClassConstructorDescriptorImpl.create(
                scriptDescriptor,
                Annotations.EMPTY,
                true,
                SourceElement.NO_SOURCE
        )
        constructor.initialize(
                createScriptParameters(constructor),
                Visibilities.PUBLIC
        )
        setDeferredReturnType(constructor)
        return constructor
    }

    private fun createScriptParameters(constructor: ClassConstructorDescriptorImpl): List<ValueParameterDescriptor> {
        return scriptDescriptor.scriptDefinition.getScriptParameters(scriptDescriptor).mapIndexed { index, (name, type) ->
            ValueParameterDescriptorImpl(
                    constructor, null, index, Annotations.EMPTY, name, type,
                    /* declaresDefaultValue = */ false,
                    /* isCrossinline = */ false,
                    /* isNoinline = */ false,
                    /* isCoroutine = */ false,
                    null, SourceElement.NO_SOURCE
            )
        }
    }

    override fun createPropertiesFromPrimaryConstructorParameters(name: Name, result: MutableSet<PropertyDescriptor>) {
    }
}
