/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.impl.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ScriptReceiver;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

// SCRIPT: Script declaration descriptor
public class ScriptDescriptorImpl extends DeclarationDescriptorNonRootImpl implements ScriptDescriptor {

    private final int priority;

    private List<ValueParameterDescriptor> valueParameters;

    private final ScriptCodeDescriptor scriptCodeDescriptor = new ScriptCodeDescriptor(this);
    private final ReceiverParameterDescriptor implicitReceiver = new ReceiverParameterDescriptorImpl(this,
                                                                                                     // Putting Any here makes no sense,
                                                                                                     // it is simply copied from someplace else
                                                                                                     // during a refactoring
                                                                                                     KotlinBuiltIns.getInstance().getAnyType(),
                                                                                                     new ScriptReceiver(this));

    private final ClassDescriptorImpl classDescriptor;

    private final WritableScopeImpl classScope;
    private WritableScope scopeForBodyResolution;

    public ScriptDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            int priority,
            @NotNull JetScope scriptScope,
            @NotNull Name className
    ) {
        super(containingDeclaration, Annotations.EMPTY, NAME);
        this.priority = priority;

        classDescriptor = new ClassDescriptorImpl(containingDeclaration, className, Modality.FINAL,
                                                  Collections.singleton(KotlinBuiltIns.getInstance().getAnyType()));
        classScope = new WritableScopeImpl(scriptScope, containingDeclaration, RedeclarationHandler.DO_NOTHING, "script members");
        classScope.changeLockLevel(WritableScope.LockLevel.BOTH);
        classDescriptor.initialize(classScope, new HashSet<ConstructorDescriptor>(), null);
    }

    public void initialize(
            @NotNull JetType returnType,
            @NotNull List<? extends PropertyDescriptorImpl> properties,
            @NotNull List<? extends FunctionDescriptor> functions
    ) {
        assert valueParameters != null : "setValueParameters() must be called before this method";
        scriptCodeDescriptor.initialize(implicitReceiver, valueParameters, returnType);

        classScope.addPropertyDescriptor(createScriptResultProperty(this));

        for (PropertyDescriptorImpl property : properties) {
            classScope.addPropertyDescriptor(property);
        }

        for (FunctionDescriptor function : functions) {
            classScope.addFunctionDescriptor(function);
        }
    }

    @NotNull
    public static PropertyDescriptor createScriptResultProperty(@NotNull ScriptDescriptor scriptDescriptor) {
        PropertyDescriptorImpl propertyDescriptor = PropertyDescriptorImpl.create(scriptDescriptor.getClassDescriptor(),
                                                                                  Annotations.EMPTY,
                                                                                  Modality.FINAL,
                                                                                  Visibilities.PUBLIC,
                                                                                  false,
                                                                                  Name.identifier(LAST_EXPRESSION_VALUE_FIELD_NAME),
                                                                                  CallableMemberDescriptor.Kind.DECLARATION);
        JetType returnType = scriptDescriptor.getScriptCodeDescriptor().getReturnType();
        assert returnType != null : "Return type not initialized for " + scriptDescriptor;
        propertyDescriptor.setType(
                returnType,
                Collections.<TypeParameterDescriptor>emptyList(),
                scriptDescriptor.getThisAsReceiverParameter(),
                ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER);
        propertyDescriptor.initialize(null, null);
        return propertyDescriptor;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    @NotNull
    public ScriptCodeDescriptor getScriptCodeDescriptor() {
        return scriptCodeDescriptor;
    }

    @Override
    @NotNull
    public ReceiverParameterDescriptor getThisAsReceiverParameter() {
        return implicitReceiver;
    }

    @Override
    public DeclarationDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        throw new IllegalStateException("nothing to substitute in script");
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitScriptDescriptor(this, data);
    }

    public void setValueParameters(@NotNull List<ValueParameterDescriptor> valueParameters) {
        this.valueParameters = valueParameters;

        ConstructorDescriptorImpl constructorDescriptor = createConstructor(this, valueParameters);
        constructorDescriptor.setReturnType(classDescriptor.getDefaultType());
        classDescriptor.getConstructors().add(constructorDescriptor);
        classDescriptor.setPrimaryConstructor(constructorDescriptor);

        for (ValueParameterDescriptor valueParameter : valueParameters) {
            classScope.addPropertyDescriptor(createPropertyFromScriptParameter(this, valueParameter));
        }
    }

    @NotNull
    public static ConstructorDescriptorImpl createConstructor(
            @NotNull ScriptDescriptor scriptDescriptor, @NotNull List<ValueParameterDescriptor> valueParameters
    ) {
        return ConstructorDescriptorImpl.create(scriptDescriptor.getClassDescriptor(), Annotations.EMPTY, true)
                .initialize(
                        Collections.<TypeParameterDescriptor>emptyList(),
                        valueParameters,
                        Visibilities.PUBLIC,
                        false
                );
    }

    @NotNull
    public static PropertyDescriptor createPropertyFromScriptParameter(
            @NotNull ScriptDescriptor scriptDescriptor,
            @NotNull ValueParameterDescriptor parameter
    ) {
        PropertyDescriptorImpl propertyDescriptor = PropertyDescriptorImpl.create(scriptDescriptor.getClassDescriptor(),
                                                                                  Annotations.EMPTY,
                                                                                  Modality.FINAL,
                                                                                  Visibilities.PUBLIC,
                                                                                  false,
                                                                                  parameter.getName(),
                                                                                  CallableMemberDescriptor.Kind.DECLARATION);
        propertyDescriptor.setType(
                parameter.getType(),
                Collections.<TypeParameterDescriptor>emptyList(),
                scriptDescriptor.getThisAsReceiverParameter(), ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER);
        propertyDescriptor.initialize(null, null);
        return propertyDescriptor;
    }

    @Override
    @NotNull
    public ClassDescriptor getClassDescriptor() {
        return classDescriptor;
    }

    @Override
    @NotNull
    public WritableScope getScopeForBodyResolution() {
        return scopeForBodyResolution;
    }

    public void setScopeForBodyResolution(@NotNull WritableScope scopeForBodyResolution) {
        assert this.scopeForBodyResolution == null : "Scope for body resolution already set for " + this;
        this.scopeForBodyResolution = scopeForBodyResolution;
    }
}
