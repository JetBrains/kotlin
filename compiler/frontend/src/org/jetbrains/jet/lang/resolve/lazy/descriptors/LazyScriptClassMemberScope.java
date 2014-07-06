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

package org.jetbrains.jet.lang.resolve.lazy.descriptors;

import kotlin.Function0;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.ConstructorDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.ScriptDescriptorImpl;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.data.JetScriptInfo;
import org.jetbrains.jet.lang.resolve.lazy.declarations.ClassMemberDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.storage.NotNullLazyValue;

import java.util.Collection;
import java.util.Set;

// SCRIPT: Members of a script class
public class LazyScriptClassMemberScope extends LazyClassMemberScope {

    private final NotNullLazyValue<PropertyDescriptor> scriptResultProperty;

    protected LazyScriptClassMemberScope(
            @NotNull ResolveSession _resolveSession,
            @NotNull ClassMemberDeclarationProvider _declarationProvider,
            @NotNull LazyClassDescriptor thisClass,
            @NotNull BindingTrace trace
    ) {
        super(_resolveSession, _declarationProvider, thisClass, trace);
        this.scriptResultProperty = _resolveSession.getStorageManager().createLazyValue(
                new Function0<PropertyDescriptor>() {
                    @Override
                    public PropertyDescriptor invoke() {
                        JetScriptInfo scriptInfo = (JetScriptInfo) declarationProvider.getOwnerInfo();

                        return ScriptDescriptorImpl.createScriptResultProperty(resolveSession.getScriptDescriptor(scriptInfo.getScript()));
                    }
                }
        );
    }

    @NotNull
    @Override
    protected Collection<DeclarationDescriptor> computeExtraDescriptors() {
        return KotlinPackage.plus(
                super.computeExtraDescriptors(),
                KotlinPackage.plus(
                    getProperties(Name.identifier(ScriptDescriptor.LAST_EXPRESSION_VALUE_FIELD_NAME)),
                    getPropertiesForScriptParameters()
                )
        );
    }

    private Collection<VariableDescriptor> getPropertiesForScriptParameters() {
        return KotlinPackage.flatMap(
                getPrimaryConstructor().getValueParameters(),
                new Function1<ValueParameterDescriptor, Iterable<? extends VariableDescriptor>>() {
                    @Override
                    public Iterable<? extends VariableDescriptor> invoke(ValueParameterDescriptor descriptor) {
                        return getProperties(descriptor.getName());
                    }
                }
        );
    }

    @Override
    protected void getNonDeclaredProperties(@NotNull Name name, @NotNull Set<VariableDescriptor> result) {
        super.getNonDeclaredProperties(name, result);

        if (name.asString().equals(ScriptDescriptor.LAST_EXPRESSION_VALUE_FIELD_NAME)) {
            result.add(scriptResultProperty.invoke());
        }
    }

    @NotNull
    public PropertyDescriptor getScriptResultProperty() {
        return scriptResultProperty.invoke();
    }

    @Override
    protected void createPropertiesFromPrimaryConstructorParameters(@NotNull Name name, @NotNull Set<VariableDescriptor> result) {
        JetScriptInfo scriptInfo = (JetScriptInfo) declarationProvider.getOwnerInfo();

        // From primary constructor parameters
        ConstructorDescriptor primaryConstructor = getPrimaryConstructor();
        if (primaryConstructor == null) return;

        for (ValueParameterDescriptor valueParameterDescriptor : primaryConstructor.getValueParameters()) {
            if (!name.equals(valueParameterDescriptor.getName())) continue;

            result.add(
                    ScriptDescriptorImpl.createPropertyFromScriptParameter(
                               resolveSession.getScriptDescriptor(scriptInfo.getScript()),
                               valueParameterDescriptor
                    )
            );
        }
    }

    @Override
    @Nullable
    protected ConstructorDescriptor resolvePrimaryConstructor() {
        JetScriptInfo scriptInfo = (JetScriptInfo) declarationProvider.getOwnerInfo();
        ScriptDescriptor scriptDescriptor = resolveSession.getScriptDescriptor(scriptInfo.getScript());
        ConstructorDescriptorImpl constructor = ScriptDescriptorImpl.createConstructor(scriptDescriptor,
                                                                                       scriptDescriptor.getScriptCodeDescriptor()
                                                                                               .getValueParameters());
        setDeferredReturnType(constructor);
        return constructor;
    }
}
