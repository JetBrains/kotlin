/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.objectweb.asm.Opcodes;

import java.util.List;

public class TraitImplBodyCodegen extends ClassBodyCodegen {
    public TraitImplBodyCodegen(JetClassOrObject aClass, CodegenContext context, ClassBuilder v, GenerationState state) {
        super(aClass, context, v, state);
    }

    //todo not needed when frontend will be able to calculate properly
    static JetType getSuperClass(ClassDescriptor myClassDescr, BindingContext bindingContext) {
        JetClassOrObject myClass = (JetClassOrObject) bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, myClassDescr);
        if(myClass == null)
            return JetStandardClasses.getAnyType();
        List<JetDelegationSpecifier> delegationSpecifiers = myClass.getDelegationSpecifiers();

        for (JetDelegationSpecifier specifier : delegationSpecifiers) {
            if (specifier instanceof JetDelegatorToSuperClass || specifier instanceof JetDelegatorToSuperCall) {
                JetType superType = bindingContext.get(BindingContext.TYPE, specifier.getTypeReference());
                ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
                final PsiElement declaration = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, superClassDescriptor);
                if (declaration != null) {
                    if (declaration instanceof PsiClass) {
                        if (!((PsiClass) declaration).isInterface()) {
                            return superClassDescriptor.getDefaultType();
                        }
                    }
                    else if(declaration instanceof JetClass) {
                        if(!((JetClass) declaration).isTrait()) {
                            return superClassDescriptor.getDefaultType();
                        }
                    }
                }
            }
        }
        return JetStandardClasses.getAnyType();
    }

    @Override
    protected void generateDeclaration() {
        v.defineClass(myClass, Opcodes.V1_6,
                      Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL/*| Opcodes.ACC_SUPER*/,
                      jvmName(),
                      null,
                      "java/lang/Object",
                      new String[0]
        );
        v.visitSource(myClass.getContainingFile().getName(), null);
    }

    private String jvmName() {
        return state.getInjector().getJetTypeMapper().mapType(descriptor.getDefaultType(), MapTypeMode.TRAIT_IMPL).getInternalName();
    }
}
