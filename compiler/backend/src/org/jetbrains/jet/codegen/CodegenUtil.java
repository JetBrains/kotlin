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

import gnu.trove.THashSet;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.JetClassObject;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collections;
import java.util.Set;

/**
 * @author abreslav
 * @author alex.tkachman
 */
public class CodegenUtil {
    private CodegenUtil() {
    }

    public static boolean isInterface(DeclarationDescriptor descriptor) {
        return descriptor instanceof ClassDescriptor && ((ClassDescriptor)descriptor).getKind() == ClassKind.TRAIT;
    }

    public static boolean isInterface(JetType type) {
        return isInterface(type.getConstructor().getDeclarationDescriptor());
    }
    
    public static boolean isClassObject(DeclarationDescriptor descriptor) {
        if(descriptor instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
            if(classDescriptor.getKind() == ClassKind.OBJECT) {
                if(classDescriptor.getContainingDeclaration() instanceof ClassDescriptor) {
                    ClassDescriptor containingDeclaration = (ClassDescriptor) classDescriptor.getContainingDeclaration();
                    if(classDescriptor.getDefaultType().equals(containingDeclaration.getClassObjectType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasThis0(ClassDescriptor classDescriptor) {
        return getOuterClassDescriptor(classDescriptor) != null && !isClassObject(classDescriptor);
    }

    public static ClassDescriptor getOuterClassDescriptor(DeclarationDescriptor descriptor) {
        DeclarationDescriptor outerDescriptor = descriptor.getContainingDeclaration();
        while(outerDescriptor != null) {
            if(outerDescriptor instanceof ClassDescriptor)
                break;

            outerDescriptor = outerDescriptor.getContainingDeclaration();
        }
        return (ClassDescriptor) outerDescriptor;
    }

    public static boolean hasDerivedTypeInfoField(JetType type) {
        for (JetType jetType : type.getConstructor().getSupertypes()) {
            if(hasTypeInfoField(jetType))
                return true;
        }

        return false;
    }

    public static boolean requireTypeInfoConstructorArg(JetType type) {
        for (TypeParameterDescriptor parameter : type.getConstructor().getParameters()) {
            if(parameter.isReified())
                return true;
        }

        return false;
    }

    public static boolean hasTypeInfoField(JetType type) {
        if(isInterface(type))
            return false;

        if(requireTypeInfoConstructorArg(type))
            return true;

        return hasDerivedTypeInfoField(type);
    }

    public static NamedFunctionDescriptor createInvoke(FunctionDescriptor fd) {
        int arity = fd.getValueParameters().size();
        NamedFunctionDescriptorImpl invokeDescriptor = new NamedFunctionDescriptorImpl(
                fd.getExpectedThisObject().exists() ? JetStandardClasses.getReceiverFunction(arity) : JetStandardClasses.getFunction(arity),
                Collections.<AnnotationDescriptor>emptyList(),
                "invoke",
                CallableMemberDescriptor.Kind.DECLARATION);

        invokeDescriptor.initialize(fd.getReceiverParameter().exists() ? fd.getReceiverParameter().getType() : null,
                                   fd.getExpectedThisObject(),
                                   Collections.<TypeParameterDescriptor>emptyList(),
                                   fd.getValueParameters(),
                                   fd.getReturnType(),
                                   Modality.FINAL, Visibility.PUBLIC);
        return invokeDescriptor;
    }

    public static boolean isSubclass(ClassDescriptor subClass, ClassDescriptor superClass) {
        Set<JetType> allSuperTypes = new THashSet<JetType>();

        addSuperTypes(subClass.getDefaultType(), allSuperTypes);

        final DeclarationDescriptor superOriginal = superClass.getOriginal();

        for (JetType superType : allSuperTypes) {
            final DeclarationDescriptor descriptor = superType.getConstructor().getDeclarationDescriptor();
            if (descriptor != null && superOriginal.equals(descriptor.getOriginal())) {
                return true;
            }
        }

        return false;
    }

    public static void addSuperTypes(JetType type, Set<JetType> set) {
        set.add(type);

        for (JetType jetType : type.getConstructor().getSupertypes()) {
            addSuperTypes(jetType, set);
        }
    }

    static boolean isNonLiteralObject(JetClassOrObject myClass) {
        return myClass instanceof JetObjectDeclaration && !((JetObjectDeclaration) myClass).isObjectLiteral() &&
                !(myClass.getParent() instanceof JetClassObject);
    }

}
