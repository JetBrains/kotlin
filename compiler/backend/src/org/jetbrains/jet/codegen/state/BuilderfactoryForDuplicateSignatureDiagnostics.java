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

package org.jetbrains.jet.codegen.state;

import com.intellij.psi.PsiElement;
import com.intellij.util.containers.MultiMap;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.ClassBuilderFactory;
import org.jetbrains.jet.codegen.SignatureCollectingClassBuilderFactory;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.DiagnosticHolder;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.calls.CallResolverUtil;
import org.jetbrains.jet.lang.resolve.java.diagnostics.*;
import org.jetbrains.jet.lang.resolve.java.jvmSignature.JvmMethodSignature;

import java.util.*;

import static org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind.DELEGATION;
import static org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind.FAKE_OVERRIDE;

class BuilderfactoryForDuplicateSignatureDiagnostics extends SignatureCollectingClassBuilderFactory {

    private final JetTypeMapper typeMapper;
    private final BindingContext bindingContext;
    private final DiagnosticHolder diagnostics;

    public BuilderfactoryForDuplicateSignatureDiagnostics(
            @NotNull ClassBuilderFactory builderFactory,
            @NotNull JetTypeMapper typeMapper,
            @NotNull BindingContext bindingContext,
            @NotNull DiagnosticHolder diagnostics
    ) {
        super(builderFactory);
        this.typeMapper = typeMapper;
        this.bindingContext = bindingContext;
        this.diagnostics = diagnostics;
    }

    @Override
    protected void handleClashingSignatures(
            @NotNull ConflictingJvmDeclarationsData data
    ) {
        Collection<PsiElement> elements = new LinkedHashSet<PsiElement>();

        boolean allDelegatedToTraitImpls = KotlinPackage.all(
                data.getSignatureOrigins(),
                new Function1<JvmDeclarationOrigin, Boolean>() {
                    @Override
                    public Boolean invoke(JvmDeclarationOrigin origin) {
                        return origin.getOriginKind() == JvmDeclarationOriginKind.DELEGATION_TO_TRAIT_IMPL;
                    }
                }
        );
        for (JvmDeclarationOrigin origin : data.getSignatureOrigins()) {
            PsiElement element = origin.getElement();
            if (element == null || allDelegatedToTraitImpls) {
                element = data.getClassOrigin().getElement();
            }
            if (element != null) {
                elements.add(element);
            }
        }

        for (PsiElement element : elements) {
            diagnostics.report(ErrorsJvm.CONFLICTING_JVM_DECLARATIONS.on(element, data));
        }
    }

    @Override
    protected void onClassDone(
            @NotNull JvmDeclarationOrigin classOrigin,
            @Nullable String classInternalName,
            boolean hasDuplicateSignatures
    ) {
        DeclarationDescriptor descriptor = classOrigin.getDescriptor();
        if (!(descriptor instanceof ClassDescriptor)) return;

        ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;

        MultiMap<RawSignature, CallableMemberDescriptor> groupedBySignature = MultiMap.create();
        Queue<DeclarationDescriptor> queue =
                new LinkedList<DeclarationDescriptor>(classDescriptor.getDefaultType().getMemberScope().getAllDescriptors());
        while (!queue.isEmpty()) {
            DeclarationDescriptor member = queue.poll();
            if (member instanceof DeclarationDescriptorWithVisibility &&
                ((DeclarationDescriptorWithVisibility) member).getVisibility() == Visibilities.INVISIBLE_FAKE) {
                // a member of super is not visible: no override
                continue;
            }
            if (member instanceof CallableMemberDescriptor &&
                CallResolverUtil.isOrOverridesSynthesized((CallableMemberDescriptor) member)) {
                // if a signature clashes with a SAM-adapter or something like that, there's no harm
                continue;
            }
            if (member instanceof PropertyDescriptor) {
                PropertyDescriptor propertyDescriptor = (PropertyDescriptor) member;

                PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
                if (getter != null) {
                    queue.add(getter);
                }
                PropertySetterDescriptor setter = propertyDescriptor.getSetter();
                if (setter != null) {
                    queue.add(setter);
                }
            }
            else if (member instanceof FunctionDescriptor) {
                FunctionDescriptor functionDescriptor = (FunctionDescriptor) member;

                JvmMethodSignature methodSignature = typeMapper.mapSignature(functionDescriptor);
                RawSignature rawSignature = new RawSignature(
                        methodSignature.getAsmMethod().getName(),
                        methodSignature.getAsmMethod().getDescriptor(),
                        MemberKind.METHOD
                );
                groupedBySignature.putValue(rawSignature, functionDescriptor);
            }
        }

        signatures:
        for (Map.Entry<RawSignature, Collection<CallableMemberDescriptor>> entry : groupedBySignature.entrySet()) {
            RawSignature rawSignature = entry.getKey();
            Collection<CallableMemberDescriptor> members = entry.getValue();

            if (members.size() <= 1) continue;

            PsiElement memberElement = null;
            int nonFakeCount = 0;
            for (CallableMemberDescriptor member : members) {
                //
                if (member.getKind() != FAKE_OVERRIDE) {
                    nonFakeCount++;
                    // If there's more than one real element, the clashing signature is already reported.
                    // Only clashes between fake overrides are interesting here
                    if (nonFakeCount > 1) continue signatures;

                    if (member.getKind() != DELEGATION) {
                        // Delegates don't have declarations in the code
                        memberElement = BindingContextUtils.callableDescriptorToDeclaration(bindingContext, member);
                        if (memberElement == null && member instanceof PropertyAccessorDescriptor) {
                            memberElement = BindingContextUtils.callableDescriptorToDeclaration(
                                    bindingContext,
                                    ((PropertyAccessorDescriptor) member).getCorrespondingProperty()
                            );
                        }
                    }
                }
            }

            PsiElement elementToReportOn = memberElement == null ? classOrigin.getElement() : memberElement;
            if (elementToReportOn == null) return;

            List<JvmDeclarationOrigin> origins = KotlinPackage.map(
                    members,
                    new Function1<CallableMemberDescriptor, JvmDeclarationOrigin>() {
                        @Override
                        public JvmDeclarationOrigin invoke(CallableMemberDescriptor descriptor) {
                            return DiagnosticsPackage.OtherOrigin(descriptor);
                        }
                    });
            ConflictingJvmDeclarationsData data =
                    new ConflictingJvmDeclarationsData(classInternalName, classOrigin, rawSignature, origins);
            diagnostics.report(ErrorsJvm.ACCIDENTAL_OVERRIDE.on(elementToReportOn, data));
        }
    }
}
