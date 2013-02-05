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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.ScriptNameUtil;
import org.jetbrains.jet.lang.resolve.name.FqName;
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

public class ScriptDescriptor extends DeclarationDescriptorNonRootImpl {
    private static final Name NAME = Name.special("<script>");

    private final int priority;

    private JetType returnType;
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

    public ScriptDescriptor(@Nullable DeclarationDescriptor containingDeclaration, int priority, JetScript script, JetScope scriptScope) {
        super(containingDeclaration, Collections.<AnnotationDescriptor>emptyList(), NAME);
        this.priority = priority;

        String className = ScriptNameUtil.classNameForScript((JetFile) script.getContainingFile()).replace('/','.');

        classDescriptor = new ClassDescriptorImpl(
                containingDeclaration,
                Collections.<AnnotationDescriptor>emptyList(),
                Modality.FINAL,
                new FqName(className).shortName());
        classScope = new WritableScopeImpl(scriptScope, containingDeclaration, RedeclarationHandler.DO_NOTHING, "script members");
        classScope.changeLockLevel(WritableScope.LockLevel.BOTH);
        classDescriptor.initialize(
                false,
                Collections.<TypeParameterDescriptor>emptyList(),
                Collections.singletonList(KotlinBuiltIns.getInstance().getAnyType()),
                classScope,
                new HashSet<ConstructorDescriptor>(),
                null,
                false);
    }

    public void initialize(@NotNull JetType returnType, JetScript declaration, BindingContext bindingContext) {
        this.returnType = returnType;
        scriptCodeDescriptor.initialize(implicitReceiver, valueParameters, returnType);

        PropertyDescriptorImpl propertyDescriptor = new PropertyDescriptorImpl(classDescriptor,
                                                               Collections.<AnnotationDescriptor>emptyList(),
                                                               Modality.FINAL,
                                                               Visibilities.PUBLIC,
                                                               false,
                                                               Name.identifier(ScriptNameUtil.LAST_EXPRESSION_VALUE_FIELD_NAME),
                                                               CallableMemberDescriptor.Kind.DECLARATION);
        propertyDescriptor.setType(
                returnType,
                Collections.<TypeParameterDescriptor>emptyList(),
                classDescriptor.getThisAsReceiverParameter(),
                ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER);
        propertyDescriptor.initialize(null, null);
        classScope.addPropertyDescriptor(propertyDescriptor);

        for (JetDeclaration jetDeclaration : declaration.getDeclarations()) {
            if(jetDeclaration instanceof JetProperty) {
                VariableDescriptor descriptor = bindingContext.get(BindingContext.VARIABLE, jetDeclaration);
                assert descriptor != null;
                initializeWithDefaultGetterSetter((PropertyDescriptorImpl) descriptor);
                classScope.addPropertyDescriptor(descriptor);
            }
            else if(jetDeclaration instanceof JetNamedFunction) {
                SimpleFunctionDescriptor descriptor = bindingContext.get(BindingContext.FUNCTION, jetDeclaration);
                assert descriptor != null;
                SimpleFunctionDescriptor copy =
                        descriptor.copy(classDescriptor, descriptor.getModality(), descriptor.getVisibility(), CallableMemberDescriptor.Kind.DECLARATION, false);
                classScope.addFunctionDescriptor(copy);
            }
        }
    }

    public static void initializeWithDefaultGetterSetter(PropertyDescriptorImpl propertyDescriptor) {
        PropertyGetterDescriptorImpl getter = propertyDescriptor.getGetter();
        if(getter == null) {
            getter = propertyDescriptor.getVisibility() != Visibilities.PRIVATE ? DescriptorResolver.createDefaultGetter(propertyDescriptor) : null;
            if(getter != null)
                getter.initialize(propertyDescriptor.getType());
        }

        PropertySetterDescriptor setter = propertyDescriptor.getSetter();
        if(setter == null) {
            setter = propertyDescriptor.isVar() ? DescriptorResolver.createDefaultSetter(propertyDescriptor) : null;
        }
        propertyDescriptor.initialize(getter, setter);
    }

    public int getPriority() {
        return priority;
    }

    @NotNull
    public JetType getReturnType() {
        return returnType;
    }

    @NotNull
    public List<ValueParameterDescriptor> getValueParameters() {
        return valueParameters;
    }

    @NotNull
    public ScriptCodeDescriptor getScriptCodeDescriptor() {
        return scriptCodeDescriptor;
    }

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
        ConstructorDescriptorImpl constructorDescriptor =
                new ConstructorDescriptorImpl(classDescriptor, Collections.<AnnotationDescriptor>emptyList(), true)
                        .initialize(Collections.<TypeParameterDescriptor>emptyList(), valueParameters, Visibilities.PUBLIC);
        constructorDescriptor.setReturnType(classDescriptor.getDefaultType());

        classDescriptor.getConstructors().add(constructorDescriptor);
        classDescriptor.setPrimaryConstructor(constructorDescriptor);

        for (ValueParameterDescriptor parameter : valueParameters) {
            PropertyDescriptorImpl propertyDescriptor = new PropertyDescriptorImpl(classDescriptor,
                                                                   Collections.<AnnotationDescriptor>emptyList(),
                                                                   Modality.FINAL,
                                                                   Visibilities.PUBLIC,
                                                                   false,
                                                                   parameter.getName(),
                                                                   CallableMemberDescriptor.Kind.DECLARATION);
            propertyDescriptor.setType(
                    parameter.getType(),
                    Collections.<TypeParameterDescriptor>emptyList(),
                    classDescriptor.getThisAsReceiverParameter(), ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER);
            //PropertyGetterDescriptor getter = DescriptorResolver.createDefaultGetter(propertyDescriptor);
            //getter.initialize(propertyDescriptor.getType());
            propertyDescriptor.initialize(null, null);
            classScope.addPropertyDescriptor(propertyDescriptor);
        }
    }

    public ClassDescriptor getClassDescriptor() {
        return classDescriptor;
    }
}
