/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.lazy.descriptors

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.descriptors.impl.ScriptDescriptorImpl
import org.jetbrains.jet.lang.resolve.BindingTrace
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession
import org.jetbrains.jet.lang.resolve.lazy.data.JetScriptInfo
import org.jetbrains.jet.lang.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.utils.toReadOnlyList

// SCRIPT: Members of a script class
public class LazyScriptClassMemberScope protected(
        resolveSession: ResolveSession,
        declarationProvider: ClassMemberDeclarationProvider,
        thisClass: LazyClassDescriptor,
        trace: BindingTrace)
: LazyClassMemberScope(resolveSession, declarationProvider, thisClass, trace) {

    private val scriptResultProperty: NotNullLazyValue<PropertyDescriptor> = resolveSession.getStorageManager().createLazyValue {
        val scriptInfo = declarationProvider.getOwnerInfo() as JetScriptInfo
        ScriptDescriptorImpl.createScriptResultProperty(resolveSession.getScriptDescriptor(scriptInfo.script))
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
                result.add(ScriptDescriptorImpl.createPropertyFromScriptParameter(resolveSession.getScriptDescriptor(scriptInfo.script), valueParameterDescriptor))
            }
        }
    }

    override fun resolvePrimaryConstructor(): ConstructorDescriptor? {
        val scriptInfo = declarationProvider.getOwnerInfo() as JetScriptInfo
        val scriptDescriptor = resolveSession.getScriptDescriptor(scriptInfo.script)
        val constructor = ScriptDescriptorImpl.createConstructor(scriptDescriptor, scriptDescriptor.getScriptCodeDescriptor().getValueParameters())
        setDeferredReturnType(constructor)
        return constructor
    }
}
