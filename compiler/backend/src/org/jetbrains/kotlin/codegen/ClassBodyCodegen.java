/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.backend.common.bridges.ImplKt;
import org.jetbrains.kotlin.codegen.context.ClassContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.synthetics.SyntheticClassOrObjectDescriptor;
import org.jetbrains.kotlin.psi.synthetics.SyntheticClassOrObjectDescriptorKt;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.enumEntryNeedSubclass;

public abstract class ClassBodyCodegen extends MemberCodegen<KtPureClassOrObject> {
    @NotNull
    public final KtPureClassOrObject myClass;

    @NotNull
    public final OwnerKind kind;

    @NotNull
    public final ClassDescriptor descriptor;

    protected ClassBodyCodegen(
            @NotNull KtPureClassOrObject myClass,
            @NotNull ClassContext context,
            @NotNull ClassBuilder v,
            @NotNull GenerationState state,
            @Nullable MemberCodegen<?> parentCodegen
    ) {
        super(state, parentCodegen, context, myClass, v);
        this.myClass = myClass;
        this.kind = context.getContextKind();
        this.descriptor = SyntheticClassOrObjectDescriptorKt.findClassDescriptor(myClass, bindingContext);
    }

    @Override
    protected void generateBody() {
        List<KtObjectDeclaration> companions = new ArrayList<>();
        if (kind != OwnerKind.DEFAULT_IMPLS && kind != OwnerKind.ERASED_INLINE_CLASS) {
            //generate nested classes first and only then generate class body. It necessary to access to nested CodegenContexts
            for (KtDeclaration declaration : myClass.getDeclarations()) {
                if (shouldProcessFirst(declaration)) {
                    //Generate companions after class body generation (need to record all synthetic accessors)
                    if (declaration instanceof KtObjectDeclaration && ((KtObjectDeclaration) declaration).isCompanion()) {
                        companions.add((KtObjectDeclaration) declaration);
                        CodegenUtilKt.populateCompanionBackingFieldNamesToOuterContextIfNeeded((KtObjectDeclaration) declaration, context, state);
                    }
                    else {
                        generateDeclaration(declaration);
                    }
                }
            }
        }

        for (KtDeclaration declaration : myClass.getDeclarations()) {
            if (!shouldProcessFirst(declaration)) {
                generateDeclaration(declaration);
            }
        }

        boolean generateNonClassMembers = shouldGenerateNonClassMembers();

        if (generateNonClassMembers) {
            generatePrimaryConstructorProperties();
            generateConstructors();
            generateDefaultImplsIfNeeded();
            generateErasedInlineClassIfNeeded();
            generateUnboxMethodForInlineClass();
        }

        // Generate _declared_ companions
        for (KtObjectDeclaration companion : companions) {
            genClassOrObject(companion);
        }

        // Generate synthetic nested classes
        Collection<DeclarationDescriptor> classifiers = descriptor
                .getUnsubstitutedMemberScope()
                .getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS, MemberScope.Companion.getALL_NAME_FILTER());
        for (DeclarationDescriptor memberDescriptor : classifiers) {
            if (memberDescriptor instanceof SyntheticClassOrObjectDescriptor) {
                genSyntheticClassOrObject((SyntheticClassOrObjectDescriptor) memberDescriptor);
            }
        }

        if (generateNonClassMembers) {
            generateBridges();
        }
    }

    private void generateBridges() {
        if (DescriptorUtils.isInterface(descriptor)) {
            return;
        }
        for (DeclarationDescriptor memberDescriptor : DescriptorUtils.getAllDescriptors(descriptor.getDefaultType().getMemberScope())) {
            if (memberDescriptor instanceof CallableMemberDescriptor) {
                CallableMemberDescriptor member = (CallableMemberDescriptor) memberDescriptor;
                if (!member.getKind().isReal() && ImplKt.findInterfaceImplementation(member) == null) {
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

    private boolean shouldGenerateNonClassMembers() {
        return !(myClass instanceof KtClassOrObject) ||
               state.getGenerateDeclaredClassFilter().shouldGenerateClassMembers((KtClassOrObject) myClass);
    }

    protected void generateConstructors() {}

    protected void generateDefaultImplsIfNeeded() {}

    protected void generateErasedInlineClassIfNeeded() {}

    protected void generateUnboxMethodForInlineClass() {}

    private static boolean shouldProcessFirst(KtDeclaration declaration) {
        return !(declaration instanceof KtProperty || declaration instanceof KtNamedFunction);
    }

    protected void generateDeclaration(KtDeclaration declaration) {
        if (declaration instanceof KtProperty || declaration instanceof KtNamedFunction || declaration instanceof KtTypeAlias) {
            if (shouldGenerateNonClassMembers()) {
                genSimpleMember(declaration);
            }
        }
        else if (declaration instanceof KtClassOrObject) {
            if (declaration instanceof KtEnumEntry && !enumEntryNeedSubclass(bindingContext, (KtEnumEntry) declaration)) {
                return;
            }

            genClassOrObject((KtClassOrObject) declaration);
        }
    }

    private void generatePrimaryConstructorProperties() {
        ClassConstructorDescriptor constructor = CollectionsKt.firstOrNull(descriptor.getConstructors());
        if (constructor == null) return;

        boolean isAnnotation = descriptor.getKind() == ClassKind.ANNOTATION_CLASS;
        FunctionDescriptor expectedAnnotationConstructor =
                isAnnotation && constructor.isActual()
                ? CodegenUtil.findExpectedFunctionForActual(constructor)
                : null;

        for (KtParameter p : getPrimaryConstructorParameters()) {
            if (p.hasValOrVar()) {
                PropertyDescriptor propertyDescriptor = bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, p);
                if (propertyDescriptor != null) {
                    if (isAnnotation) {
                        propertyCodegen.generateConstructorPropertyAsMethodForAnnotationClass(
                                p, propertyDescriptor, expectedAnnotationConstructor
                        );
                    }
                    else {
                        propertyCodegen.generatePrimaryConstructorProperty(p, propertyDescriptor);
                    }
                }
            }
        }
    }

    @NotNull
    public List<KtParameter> getPrimaryConstructorParameters() {
        if (myClass instanceof KtClass) {
            return myClass.getPrimaryConstructorParameters();
        }
        return Collections.emptyList();
    }

    @Nullable
    @Override
    protected ClassDescriptor classForInnerClassRecord() {
        return InnerClassConsumer.Companion.classForInnerClassRecord(descriptor, false);
    }
}
