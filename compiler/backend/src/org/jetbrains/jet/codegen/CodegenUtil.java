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

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.context.CalculatedClosure;
import org.jetbrains.jet.codegen.signature.BothSignatureWriter;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterKind;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;

import java.util.*;

import static org.jetbrains.asm4.Opcodes.*;

/**
 * @author abreslav
 * @author alex.tkachman
 */
public class CodegenUtil {
    public static final String RECEIVER$0 = "receiver$0";
    public static final String THIS$0 = "this$0";

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

    public static boolean isObjectLiteral(ClassDescriptor declaration, BindingContext bindingContext) {
        PsiElement psiElement = BindingContextUtils.descriptorToDeclaration(bindingContext, declaration);
        if (psiElement instanceof JetObjectDeclaration && ((JetObjectDeclaration) psiElement).isObjectLiteral()) {
            return true;
        }
        return false;
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
        InstructionAdapter iv = new InstructionAdapter(mv);
        iv.anew(Type.getObjectType(exception));
        iv.dup();
        iv.aconst(message);
        iv.invokespecial(exception, "<init>", "(Ljava/lang/String;)V");
        iv.athrow();
    }

    public static void generateMethodThrow(MethodVisitor mv, String exception, String message) {
        mv.visitCode();
        generateThrow(mv, exception, message);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    @NotNull
    public static JvmClassName getInternalClassName(FunctionDescriptor descriptor) {
        final int paramCount = descriptor.getValueParameters().size();
        if (descriptor.getReceiverParameter().exists()) {
            return JvmClassName.byInternalName("jet/ExtensionFunction" + paramCount);
        }
        else {
            return JvmClassName.byInternalName("jet/Function" + paramCount);
        }
    }

    static JvmMethodSignature erasedInvokeSignature(FunctionDescriptor fd) {

        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, false);

        signatureWriter.writeFormalTypeParametersStart();
        signatureWriter.writeFormalTypeParametersEnd();

        boolean isExtensionFunction = fd.getReceiverParameter().exists();
        int paramCount = fd.getValueParameters().size();
        if (isExtensionFunction) {
            paramCount++;
        }

        signatureWriter.writeParametersStart();

        for (int i = 0; i < paramCount; ++i) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.VALUE);
            signatureWriter.writeAsmType(JetTypeMapper.OBJECT_TYPE, true);
            signatureWriter.writeParameterTypeEnd();
        }

        signatureWriter.writeParametersEnd();

        signatureWriter.writeReturnType();
        signatureWriter.writeAsmType(JetTypeMapper.OBJECT_TYPE, true);
        signatureWriter.writeReturnTypeEnd();

        return signatureWriter.makeJvmMethodSignature("invoke");
    }

    public static boolean isConst(CalculatedClosure closure) {
        return closure.getCaptureThis() == null && closure.getCaptureReceiver() == null && closure.getCaptureVariables().isEmpty();
    }

    public static JetDelegatorToSuperCall findSuperCall(JetElement classOrObject, BindingContext bindingContext) {
        if (!(classOrObject instanceof JetClassOrObject)) {
            return null;
        }

        if (classOrObject instanceof JetClass && ((JetClass) classOrObject).isTrait()) {
            return null;
        }
        for (JetDelegationSpecifier specifier : ((JetClassOrObject) classOrObject).getDelegationSpecifiers()) {
            if (specifier instanceof JetDelegatorToSuperCall) {
                JetType superType = bindingContext.get(BindingContext.TYPE, specifier.getTypeReference());
                assert superType != null;
                ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
                assert superClassDescriptor != null;
                if (!isInterface(superClassDescriptor)) {
                    return (JetDelegatorToSuperCall) specifier;
                }
            }
        }

        return null;
    }

    public static void generateClosureFields(CalculatedClosure closure, ClassBuilder v, JetTypeMapper typeMapper) {
        final ClassifierDescriptor captureThis = closure.getCaptureThis();
        final int access = ACC_PUBLIC | ACC_SYNTHETIC | ACC_FINAL;
        if (captureThis != null) {
            v.newField(null, access, THIS$0, typeMapper.mapType(captureThis.getDefaultType(), MapTypeMode.VALUE).getDescriptor(), null,
                       null);
        }

        final ClassifierDescriptor captureReceiver = closure.getCaptureReceiver();
        if (captureReceiver != null) {
            v.newField(null, access, RECEIVER$0, typeMapper.mapType(captureReceiver.getDefaultType(), MapTypeMode.VALUE).getDescriptor(),
                       null, null);
        }

        final List<Pair<String, Type>> fields = closure.getRecordedFields();
        for (Pair<String, Type> field : fields) {
            v.newField(null, access, field.first, field.second.getDescriptor(), null, null);
        }
    }
}
