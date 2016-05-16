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
import org.jetbrains.kotlin.descriptors.impl.ConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.incremental.record
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.utils.toReadOnlyList

class LazyScriptClassMemberScope(
        resolveSession: ResolveSession,
        declarationProvider: ClassMemberDeclarationProvider,
        private val scriptDescriptor: LazyScriptDescriptor,
        trace: BindingTrace)
: LazyClassMemberScope(resolveSession, declarationProvider, scriptDescriptor, trace) {

    override fun resolvePrimaryConstructor(): ConstructorDescriptor? {
        val constructor = ConstructorDescriptorImpl.create(
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

    private fun createScriptParameters(constructor: ConstructorDescriptorImpl): List<ValueParameterDescriptor> {
        return scriptDescriptor.scriptDefinition.getScriptParameters(scriptDescriptor).mapIndexed { index, scriptParameter ->
            ValueParameterDescriptorImpl(
                    constructor, null, index, Annotations.EMPTY, scriptParameter.name, scriptParameter.type,
                    /* declaresDefaultValue = */ false,
                    /* isCrossinline = */ false,
                    /* isNoinline = */ false,
                    /* isCoroutine = */ false,
                    null, SourceElement.NO_SOURCE
            )
        }
    }

    override fun computeExtraDescriptors(location: LookupLocation): Collection<DeclarationDescriptor> {
        return (super.computeExtraDescriptors(location)
                + getPropertiesForScriptParameters()).toReadOnlyList()
    }

    private fun getPropertiesForScriptParameters() = getPrimaryConstructor()!!.valueParameters.flatMap {
        getContributedVariables(it.name, NoLookupLocation.FOR_SCRIPT)
    }

    override fun createPropertiesFromPrimaryConstructorParameters(name: Name, result: MutableSet<PropertyDescriptor>) {
        val primaryConstructor = getPrimaryConstructor()!!
        for (valueParameterDescriptor in primaryConstructor.valueParameters) {
            if (name == valueParameterDescriptor.name) {
                result.add(createPropertyFromScriptParameter(scriptDescriptor, valueParameterDescriptor))
            }
        }
    }

    private fun createPropertyFromScriptParameter(
            scriptDescriptor: ScriptDescriptor,
            parameter: ValueParameterDescriptor
    ): PropertyDescriptor {
        val propertyDescriptor = PropertyDescriptorImpl.create(
                scriptDescriptor,
                Annotations.EMPTY,
                Modality.FINAL,
                Visibilities.PUBLIC,
                false,
                parameter.name,
                CallableMemberDescriptor.Kind.DECLARATION,
                SourceElement.NO_SOURCE,
                /* lateInit = */ false,
                /* isConst = */ false
        )
        propertyDescriptor.setType(
                parameter.type,
                listOf(),
                scriptDescriptor.thisAsReceiverParameter,
                null as ReceiverParameterDescriptor?
        )
        propertyDescriptor.initialize(null, null)
        return propertyDescriptor
    }

    override fun recordLookup(name: Name, from: LookupLocation) {
        c.lookupTracker.record(from, thisDescriptor, name)
    }
}
