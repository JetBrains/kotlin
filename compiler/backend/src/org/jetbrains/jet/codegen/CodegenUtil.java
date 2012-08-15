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

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;

import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

/**
 * @author abreslav
 * @author alex.tkachman
 */
public class CodegenUtil {
    private CodegenUtil() {
    }

    private static final Random RANDOM = new Random(55L);

    public static boolean isInterface(DeclarationDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            final ClassKind kind = ((ClassDescriptor) descriptor).getKind();
            return kind == ClassKind.TRAIT || kind == ClassKind.ANNOTATION_CLASS;
        }
        return false;
    }

    public static boolean isInterface(JetType type) {
        return isInterface(type.getConstructor().getDeclarationDescriptor());
    }

    public static SimpleFunctionDescriptor createInvoke(FunctionDescriptor fd) {
        int arity = fd.getValueParameters().size();
        SimpleFunctionDescriptorImpl invokeDescriptor = new SimpleFunctionDescriptorImpl(
                fd.getExpectedThisObject().exists() ? JetStandardClasses.getReceiverFunction(arity) : JetStandardClasses.getFunction(arity),
                Collections.<AnnotationDescriptor>emptyList(),
                Name.identifier("invoke"),
                CallableMemberDescriptor.Kind.DECLARATION);

        invokeDescriptor.initialize(fd.getReceiverParameter().exists() ? fd.getReceiverParameter().getType() : null,
                                    fd.getExpectedThisObject(),
                                    Collections.<TypeParameterDescriptorImpl>emptyList(),
                                    fd.getValueParameters(),
                                    fd.getReturnType(),
                                    Modality.FINAL,
                                    Visibilities.PUBLIC,
                                    /*isInline = */false
        );
        return invokeDescriptor;
    }

    public static boolean isNonLiteralObject(JetClassOrObject myClass) {
        return myClass instanceof JetObjectDeclaration && !((JetObjectDeclaration) myClass).isObjectLiteral() &&
               !(myClass.getParent() instanceof JetClassObject);
    }

    public static boolean isLocalFun(DeclarationDescriptor fd, BindingContext bindingContext) {
        PsiElement psiElement = BindingContextUtils.descriptorToDeclaration(bindingContext, fd);
        if (psiElement instanceof JetNamedFunction && psiElement.getParent() instanceof JetBlockExpression) {
            return true;
        }
        return false;
    }


    public static boolean isNamedFun(DeclarationDescriptor fd, BindingContext bindingContext) {
        PsiElement psiElement = BindingContextUtils.descriptorToDeclaration(bindingContext, fd);
        if (psiElement instanceof JetNamedFunction) {
            return true;
        }
        return false;
    }

    public static String generateTmpVariableName(Collection<String> existingNames) {
        String prefix = "tmp";
        int i = RANDOM.nextInt(Integer.MAX_VALUE);
        String name = prefix + i;
        while (existingNames.contains(name)) {
            i++;
            name = prefix + i;
        }
        return name;
    }


    public static
    @NotNull
    BitSet getFlagsForVisibility(@NotNull Visibility visibility) {
        BitSet flags = new BitSet();
        if (visibility == Visibilities.INTERNAL) {
            flags.set(JvmStdlibNames.FLAG_INTERNAL_BIT);
        }
        else if (visibility == Visibilities.PRIVATE) {
            flags.set(JvmStdlibNames.FLAG_PRIVATE_BIT);
        }
        return flags;
    }

    public static void generateThrow(MethodVisitor mv, String exception, String message) {
        InstructionAdapter instructionAdapter = new InstructionAdapter(mv);
        instructionAdapter.anew(Type.getObjectType(exception));
        instructionAdapter.dup();
        instructionAdapter.aconst(message);
        instructionAdapter.invokespecial(exception, "<init>", "(Ljava/lang/String;)V");
        instructionAdapter.athrow();
    }

    public static void generateMethodThrow(MethodVisitor mv, String exception, String message) {
        mv.visitCode();
        generateThrow(mv, exception, message);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }
}
