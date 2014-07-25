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

import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.context.ClassContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.DiagnosticsPackage;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.Iterator;
import java.util.List;

import static org.jetbrains.kotlin.backend.common.bridges.BridgesPackage.findImplementationFromInterface;
import static org.jetbrains.kotlin.backend.common.bridges.BridgesPackage.firstSuperMethodFromKotlin;
import static org.jetbrains.kotlin.codegen.AsmUtil.writeKotlinSyntheticClassAnnotation;
import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinSyntheticClass;
import static org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.descriptorToDeclaration;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class TraitImplBodyCodegen extends ClassBodyCodegen {
    public TraitImplBodyCodegen(
            @NotNull JetClassOrObject aClass,
            @NotNull ClassContext context,
            @NotNull ClassBuilder v,
            @NotNull GenerationState state,
            @Nullable MemberCodegen<?> parentCodegen
    ) {
        super(aClass, context, v, state, parentCodegen);
    }

    @Override
    protected void generateDeclaration() {
        v.defineClass(myClass, V1_6,
                      ACC_PUBLIC | ACC_FINAL,
                      typeMapper.mapTraitImpl(descriptor).getInternalName(),
                      null,
                      "java/lang/Object",
                      ArrayUtil.EMPTY_STRING_ARRAY
        );
        v.visitSource(myClass.getContainingFile().getName(), null);
    }

    @Override
    protected void generateSyntheticParts() {
        for (DeclarationDescriptor memberDescriptor : descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
            if (!(memberDescriptor instanceof CallableMemberDescriptor)) continue;

            CallableMemberDescriptor fakeOverride = (CallableMemberDescriptor) memberDescriptor;
            if (fakeOverride.getKind().isReal()) continue;
            if (fakeOverride.getVisibility() == Visibilities.INVISIBLE_FAKE) continue;
            if (fakeOverride.getModality() == Modality.ABSTRACT) continue;

            CallableMemberDescriptor implementation = findImplementationFromInterface(fakeOverride);
            if (implementation == null) continue;

            // If implementation is located in a Java interface, it will be inherited via normal Java rules
            if (implementation instanceof JavaMethodDescriptor) continue;

            // We create a copy of the function with kind = DECLARATION so that FunctionCodegen will generate its body
            CallableMemberDescriptor copy = fakeOverride.copy(
                    fakeOverride.getContainingDeclaration(), Modality.OPEN, fakeOverride.getVisibility(),
                    CallableMemberDescriptor.Kind.DECLARATION, true
            );

            if (fakeOverride instanceof FunctionDescriptor) {
                generateDelegationToSuperTraitImpl((FunctionDescriptor) copy, (FunctionDescriptor) implementation);
            }
            else if (fakeOverride instanceof PropertyDescriptor) {
                PropertyGetterDescriptor getter = ((PropertyDescriptor) copy).getGetter();
                PropertyGetterDescriptor implGetter = ((PropertyDescriptor) implementation).getGetter();
                if (getter != null && implGetter != null) {
                    generateDelegationToSuperTraitImpl(getter, implGetter);
                }
                PropertySetterDescriptor setter = ((PropertyDescriptor) copy).getSetter();
                PropertySetterDescriptor implSetter = ((PropertyDescriptor) implementation).getSetter();
                if (setter != null && implSetter != null) {
                    generateDelegationToSuperTraitImpl(setter, implSetter);
                }
            }
        }
    }

    private void generateDelegationToSuperTraitImpl(@NotNull FunctionDescriptor descriptor, @NotNull FunctionDescriptor implementation) {
        final FunctionDescriptor delegateTo = (FunctionDescriptor) firstSuperMethodFromKotlin(descriptor, implementation);
        if (delegateTo == null) return;

        // We can't call super methods from Java 1.8 interfaces because that requires INVOKESPECIAL which is forbidden from TImpl class
        if (delegateTo instanceof JavaMethodDescriptor) return;

        functionCodegen.generateMethod(
                DiagnosticsPackage.DelegationToTraitImpl(descriptorToDeclaration(descriptor), descriptor),
                descriptor,
                new FunctionGenerationStrategy.CodegenBased<FunctionDescriptor>(state, descriptor) {
                    @Override
                    public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
                        InstructionAdapter iv = codegen.v;

                        CallableMethod method = typeMapper.mapToCallableMethod(delegateTo, true, context);
                        List<JvmMethodParameterSignature> myParameters = signature.getValueParameters();
                        List<JvmMethodParameterSignature> calleeParameters = method.getValueParameters();

                        if (myParameters.size() != calleeParameters.size()) {
                            throw new AssertionError(
                                    String.format(
                                            "Method from super interface has a different signature.\n" +
                                            "This method:\n%s\n%s\n%s\nSuper method:\n%s\n%s\n%s",
                                            callableDescriptor, signature, myParameters, delegateTo, method, calleeParameters
                                    )
                            );
                        }

                        int k = 0;
                        Iterator<JvmMethodParameterSignature> it = calleeParameters.iterator();
                        for (JvmMethodParameterSignature parameter : myParameters) {
                            Type type = parameter.getAsmType();
                            StackValue.local(k, type).put(it.next().getAsmType(), iv);
                            k += type.getSize();
                        }

                        method.genInvokeInstruction(iv);
                        StackValue.coerce(method.getReturnType(), signature.getReturnType(), iv);
                        iv.areturn(signature.getReturnType());
                    }
                }
        );
    }

    @Override
    protected void generateKotlinAnnotation() {
        writeKotlinSyntheticClassAnnotation(v, DescriptorUtils.isTopLevelOrInnerClass(descriptor)
                                               ? KotlinSyntheticClass.Kind.TRAIT_IMPL
                                               : KotlinSyntheticClass.Kind.LOCAL_TRAIT_IMPL);
    }
}
