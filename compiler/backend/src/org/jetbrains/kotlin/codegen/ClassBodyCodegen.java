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

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.bridges.BridgesPackage;
import org.jetbrains.kotlin.codegen.context.ClassContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.enumEntryNeedSubclass;

public abstract class ClassBodyCodegen extends MemberCodegen<JetClassOrObject> {
    protected final JetClassOrObject myClass;
    protected final OwnerKind kind;
    protected final ClassDescriptor descriptor;

    protected ClassBodyCodegen(
            @NotNull JetClassOrObject myClass,
            @NotNull ClassContext context,
            @NotNull ClassBuilder v,
            @NotNull GenerationState state,
            @Nullable MemberCodegen<?> parentCodegen
    ) {
        super(state, parentCodegen, context, myClass, v);
        this.myClass = myClass;
        this.kind = context.getContextKind();
        this.descriptor = bindingContext.get(BindingContext.CLASS, myClass);
    }

    @Override
    protected void generateBody() {
        if (kind != OwnerKind.TRAIT_IMPL) {
            //generate nested classes first and only then generate class body. It necessary to access to nested CodegenContexts
            for (JetDeclaration declaration : myClass.getDeclarations()) {
                if (shouldProcessFirst(declaration)) {
                    generateDeclaration(declaration);
                }
            }
        }

        for (JetDeclaration declaration : myClass.getDeclarations()) {
            if (!shouldProcessFirst(declaration)) {
                generateDeclaration(declaration);
            }
        }

        if (!DescriptorUtils.isTrait(descriptor)) {
            for (DeclarationDescriptor memberDescriptor : descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
                if (memberDescriptor instanceof FunctionDescriptor) {
                    FunctionDescriptor member = (FunctionDescriptor) memberDescriptor;
                    if (!member.getKind().isReal() && BridgesPackage.findTraitImplementation(member) == null) {
                        functionCodegen.generateBridges(member);
                    }
                }
            }
        }

        generatePrimaryConstructorProperties(propertyCodegen, myClass);
    }

    private static boolean shouldProcessFirst(JetDeclaration declaration) {
        return !(declaration instanceof JetProperty || declaration instanceof JetNamedFunction);
    }


    protected void generateDeclaration(JetDeclaration declaration) {
        if (declaration instanceof JetProperty || declaration instanceof JetNamedFunction) {
            genFunctionOrProperty(declaration);
        }
        else if (declaration instanceof JetClassOrObject) {
            if (declaration instanceof JetEnumEntry && !enumEntryNeedSubclass(bindingContext, (JetEnumEntry) declaration)) {
                return;
            }

            genClassOrObject((JetClassOrObject) declaration);
        }
    }

    private void generatePrimaryConstructorProperties(PropertyCodegen propertyCodegen, JetClassOrObject origin) {
        boolean isAnnotation = origin instanceof JetClass && ((JetClass) origin).isAnnotation();
        for (JetParameter p : getPrimaryConstructorParameters()) {
            if (p.hasValOrVarNode()) {
                PropertyDescriptor propertyDescriptor = bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, p);
                if (propertyDescriptor != null) {
                    if (!isAnnotation) {
                        propertyCodegen.generatePrimaryConstructorProperty(p, propertyDescriptor);
                    }
                    else {
                        propertyCodegen.generateConstructorPropertyAsMethodForAnnotationClass(p, propertyDescriptor);
                    }
                }
            }
        }
    }

    @NotNull
    protected List<JetParameter> getPrimaryConstructorParameters() {
        if (myClass instanceof JetClass) {
            return ((JetClass) myClass).getPrimaryConstructorParameters();
        }
        return Collections.emptyList();
    }

    @Nullable
    @Override
    protected ClassDescriptor classForInnerClassRecord() {
        return DescriptorUtils.isTopLevelDeclaration(descriptor) ? null : descriptor;
    }
}
