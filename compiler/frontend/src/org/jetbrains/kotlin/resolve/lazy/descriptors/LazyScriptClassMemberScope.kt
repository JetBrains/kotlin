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

package org.jetbrains.kotlin.resolve.lazy.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.data.JetScriptInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.utils.toReadOnlyList
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.types.JetType
import java.util.Collections
import org.jetbrains.kotlin.descriptors.impl.ConstructorDescriptorImpl

// SCRIPT: Members of a script class
public class LazyScriptClassMemberScope protected(
        private val resolveSession: ResolveSession,
        declarationProvider: ClassMemberDeclarationProvider,
        thisClass: LazyClassDescriptor,
        trace: BindingTrace)
: LazyClassMemberScope(resolveSession, declarationProvider, thisClass, trace) {

    private val scriptResultProperty: NotNullLazyValue<PropertyDescriptor> = resolveSession.getStorageManager().createLazyValue {
        val scriptInfo = declarationProvider.getOwnerInfo() as JetScriptInfo
        createScriptResultProperty(resolveSession.getScriptDescriptor(scriptInfo.script))
    }

    override fun computeExtraDescriptors(): Collection<DeclarationDescriptor> {
        return (super.computeExtraDescriptors()
                + getProperties(Name.identifier(ScriptDescriptor.LAST_EXPRESSION_VALUE_FIELD_NAME))
                + getPropertiesForScriptParameters()).toReadOnlyList()
    }

    private fun getPropertiesForScriptParameters() = getPrimaryConstructor()!!.getValueParameters().flatMap { getProperties(it.getName()) }

    override fun getNonDeclaredProperties(name: Name, result: MutableSet<VariableDescriptor>) {
        super.getNonDeclaredProperties(name, result)

        if (name.asString() == ScriptDescriptor.LAST_EXPRESSION_VALUE_FIELD_NAME) {
            result.add(scriptResultProperty())
        }
    }

    public fun getScriptResultProperty(): PropertyDescriptor = scriptResultProperty()

    override fun createPropertiesFromPrimaryConstructorParameters(name: Name, result: MutableSet<VariableDescriptor>) {
        val scriptInfo = declarationProvider.getOwnerInfo() as JetScriptInfo

        // From primary constructor parameters
        val primaryConstructor = getPrimaryConstructor()
        if (primaryConstructor == null) return

        for (valueParameterDescriptor in primaryConstructor.getValueParameters()) {
            if (name == valueParameterDescriptor.getName()) {
                result.add(createPropertyFromScriptParameter(resolveSession.getScriptDescriptor(scriptInfo.script), valueParameterDescriptor))
            }
        }
    }

    override fun resolvePrimaryConstructor(): ConstructorDescriptor? {
        val scriptInfo = declarationProvider.getOwnerInfo() as JetScriptInfo
        val scriptDescriptor = resolveSession.getScriptDescriptor(scriptInfo.script)
        val constructor = createConstructor(scriptDescriptor, scriptDescriptor.getScriptCodeDescriptor().getValueParameters())
        setDeferredReturnType(constructor)
        return constructor
    }

    private fun createScriptResultProperty(scriptDescriptor: ScriptDescriptor): PropertyDescriptor {
        val propertyDescriptor = PropertyDescriptorImpl.create(
                scriptDescriptor.getClassDescriptor(),
                Annotations.EMPTY,
                Modality.FINAL,
                Visibilities.PUBLIC,
                false,
                Name.identifier(ScriptDescriptor.LAST_EXPRESSION_VALUE_FIELD_NAME),
                CallableMemberDescriptor.Kind.DECLARATION,
                SourceElement.NO_SOURCE
        )

        val returnType = scriptDescriptor.getScriptCodeDescriptor().getReturnType()
        assert(returnType != null) { "Return type not initialized for " + scriptDescriptor }

        propertyDescriptor.setType(
                returnType,
                listOf<TypeParameterDescriptor>(),
                scriptDescriptor.getThisAsReceiverParameter(),
                ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER
        )
        propertyDescriptor.initialize(null, null)

        return propertyDescriptor
    }

    private fun createConstructor(scriptDescriptor: ScriptDescriptor, valueParameters: List<ValueParameterDescriptor>): ConstructorDescriptorImpl {
        return ConstructorDescriptorImpl.create(
                scriptDescriptor.getClassDescriptor(),
                Annotations.EMPTY,
                true,
                SourceElement.NO_SOURCE
        ).initialize(
                listOf(),
                valueParameters,
                Visibilities.PUBLIC
        )
    }

    private fun createPropertyFromScriptParameter(scriptDescriptor: ScriptDescriptor, parameter: ValueParameterDescriptor): PropertyDescriptor {
        val propertyDescriptor = PropertyDescriptorImpl.create(
                scriptDescriptor.getClassDescriptor(),
                Annotations.EMPTY,
                Modality.FINAL,
                Visibilities.PUBLIC,
                false,
                parameter.getName(),
                CallableMemberDescriptor.Kind.DECLARATION,
                SourceElement.NO_SOURCE
        )
        propertyDescriptor.setType(
                parameter.getType(),
                listOf(),
                scriptDescriptor.getThisAsReceiverParameter(),
                ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER
        )
        propertyDescriptor.initialize(null, null)
        return propertyDescriptor
    }
}
