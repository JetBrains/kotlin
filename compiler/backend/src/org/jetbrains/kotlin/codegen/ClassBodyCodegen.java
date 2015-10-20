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
import org.jetbrains.kotlin.backend.common.bridges.ImplKt;
import org.jetbrains.kotlin.codegen.context.ClassContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.enumEntryNeedSubclass;

public abstract class ClassBodyCodegen extends MemberCodegen<KtClassOrObject> {
    protected final KtClassOrObject myClass;
    protected final OwnerKind kind;
    protected final ClassDescriptor descriptor;

    protected ClassBodyCodegen(
            @NotNull KtClassOrObject myClass,
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
        if (kind != OwnerKind.DEFAULT_IMPLS) {
            //generate nested classes first and only then generate class body. It necessary to access to nested CodegenContexts
            for (KtDeclaration declaration : myClass.getDeclarations()) {
                if (shouldProcessFirst(declaration)) {
                    generateDeclaration(declaration);
                }
            }
        }

        for (KtDeclaration declaration : myClass.getDeclarations()) {
            if (!shouldProcessFirst(declaration)) {
                generateDeclaration(declaration);
            }
        }

        if (!DescriptorUtils.isInterface(descriptor)) {
            for (DeclarationDescriptor memberDescriptor : descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
                if (memberDescriptor instanceof CallableMemberDescriptor) {
                    CallableMemberDescriptor member = (CallableMemberDescriptor) memberDescriptor;
                    if (!member.getKind().isReal() && ImplKt.findTraitImplementation(member) == null) {
                        if (member instanceof FunctionDescriptor) {
                            functionCodegen.generateBridges((FunctionDescriptor) member);
                        }
                        else if (member instanceof PropertyDescriptor) {
                            PropertyGetterDescriptor getter = ((PropertyDescriptor) member).getGetter();
                            if (getter != null) {
                                functionCodegen.generateBridges(getter);
                            }
                            PropertySetterDescriptor setter = ((PropertyDescriptor) member).getSetter();
                            if (setter != null) {
                                functionCodegen.generateBridges(setter);
                            }
                        }
                    }
                }
            }
        }

        generatePrimaryConstructorProperties();
    }

    private static boolean shouldProcessFirst(KtDeclaration declaration) {
        return !(declaration instanceof KtProperty || declaration instanceof KtNamedFunction);
    }

    protected void generateDeclaration(KtDeclaration declaration) {
        if (declaration instanceof KtProperty || declaration instanceof KtNamedFunction) {
            genFunctionOrProperty(declaration);
        }
        else if (declaration instanceof KtClassOrObject) {
            if (declaration instanceof KtEnumEntry && !enumEntryNeedSubclass(bindingContext, (KtEnumEntry) declaration)) {
                return;
            }

            genClassOrObject((KtClassOrObject) declaration);
        }
    }

    private void generatePrimaryConstructorProperties() {
        boolean isAnnotation = descriptor.getKind() == ClassKind.ANNOTATION_CLASS;
        for (KtParameter p : getPrimaryConstructorParameters()) {
            if (p.hasValOrVar()) {
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
    protected List<KtParameter> getPrimaryConstructorParameters() {
        if (myClass instanceof KtClass) {
            return ((KtClass) myClass).getPrimaryConstructorParameters();
        }
        return Collections.emptyList();
    }

    @Nullable
    @Override
    protected ClassDescriptor classForInnerClassRecord() {
        return DescriptorUtils.isTopLevelDeclaration(descriptor) ? null : descriptor;
    }
}
