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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.binding.CalculatedClosure;
import org.jetbrains.jet.codegen.signature.BothSignatureWriter;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterKind;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.JetClassObject;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import java.util.*;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isClassObject;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;

/**
 * @author abreslav
 * @author alex.tkachman
 */
public class CodegenUtil {
    public static final String RECEIVER$0 = "receiver$0";
    public static final String THIS$0 = "this$0";

    private static final int NO_FLAG_LOCAL = 0;
    private static final int NO_FLAG_PACKAGE_PRIVATE = 0;

    @NotNull
    private static final Map<Visibility, Integer> visibilityToAccessFlag = ImmutableMap.<Visibility, Integer>builder()
            .put(Visibilities.PRIVATE, ACC_PRIVATE)
            .put(Visibilities.PROTECTED, ACC_PROTECTED)
            .put(Visibilities.PUBLIC, ACC_PUBLIC)
            .put(Visibilities.INTERNAL, ACC_PUBLIC)
            .put(Visibilities.LOCAL, NO_FLAG_LOCAL)
            .put(JavaDescriptorResolver.PACKAGE_VISIBILITY, NO_FLAG_PACKAGE_PRIVATE)
            .build();

    private static final Set<ClassDescriptor> PRIMITIVE_NUMBER_CLASSES = Sets.newHashSet(
            JetStandardLibrary.getInstance().getByte(),
            JetStandardLibrary.getInstance().getShort(),
            JetStandardLibrary.getInstance().getInt(),
            JetStandardLibrary.getInstance().getLong(),
            JetStandardLibrary.getInstance().getFloat(),
            JetStandardLibrary.getInstance().getDouble(),
            JetStandardLibrary.getInstance().getChar()
    );

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

    public static JvmMethodSignature erasedInvokeSignature(FunctionDescriptor fd) {

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
            signatureWriter.writeAsmType(OBJECT_TYPE, true);
            signatureWriter.writeParameterTypeEnd();
        }

        signatureWriter.writeParametersEnd();

        signatureWriter.writeReturnType();
        signatureWriter.writeAsmType(OBJECT_TYPE, true);
        signatureWriter.writeReturnTypeEnd();

        return signatureWriter.makeJvmMethodSignature("invoke");
    }

    public static boolean isConst(CalculatedClosure closure) {
        return closure.getCaptureThis() == null && closure.getCaptureReceiver() == null && closure.getCaptureVariables().isEmpty();
    }

    public static void generateClosureFields(CalculatedClosure closure, ClassBuilder v, JetTypeMapper typeMapper) {
        final ClassifierDescriptor captureThis = closure.getCaptureThis();
        final int access = ACC_PUBLIC | ACC_SYNTHETIC | ACC_FINAL;
        if (captureThis != null) {
            v.newField(null, access, THIS$0, typeMapper.mapType(captureThis).getDescriptor(), null,
                       null);
        }

        final ClassifierDescriptor captureReceiver = closure.getCaptureReceiver();
        if (captureReceiver != null) {
            v.newField(null, access, RECEIVER$0, typeMapper.mapType(captureReceiver).getDescriptor(),
                       null, null);
        }

        final List<Pair<String, Type>> fields = closure.getRecordedFields();
        for (Pair<String, Type> field : fields) {
            v.newField(null, access, field.first, field.second.getDescriptor(), null, null);
        }
    }

    public static <T> T peekFromStack(Stack<T> stack) {
        return stack.empty() ? null : stack.peek();
    }

    //TODO: move mapping logic to front-end java
    public static int getVisibilityAccessFlag(@NotNull MemberDescriptor descriptor) {
        Integer specialCase = specialCaseVisibility(descriptor);
        if (specialCase != null) {
            return specialCase;
        }
        Integer defaultMapping = visibilityToAccessFlag.get(descriptor.getVisibility());
        if (defaultMapping == null) {
            throw new IllegalStateException(descriptor.getVisibility() + " is not a valid visibility in backend.");
        }
        return defaultMapping;
    }

    @Nullable
    private static Integer specialCaseVisibility(@NotNull MemberDescriptor memberDescriptor) {
        DeclarationDescriptor containingDeclaration = memberDescriptor.getContainingDeclaration();
        if (isInterface(containingDeclaration)) {
            return ACC_PUBLIC;
        }
        Visibility memberVisibility = memberDescriptor.getVisibility();
        if (memberVisibility != Visibilities.PRIVATE) {
            return null;
        }
        if (isClassObject(containingDeclaration)) {
            return NO_FLAG_PACKAGE_PRIVATE;
        }
        if (memberDescriptor instanceof ConstructorDescriptor) {
            ClassKind kind = ((ClassDescriptor) containingDeclaration).getKind();
            if (kind == ClassKind.OBJECT) {
                //TODO: should be NO_FLAG_PACKAGE_PRIVATE
                // see http://youtrack.jetbrains.com/issue/KT-2700
                return ACC_PUBLIC;
            }
            else if (kind == ClassKind.ENUM_ENTRY) {
                return NO_FLAG_PACKAGE_PRIVATE;
            }
            else if (kind == ClassKind.ENUM_CLASS) {
                //TODO: should be ACC_PRIVATE
                // see http://youtrack.jetbrains.com/issue/KT-2680
                return ACC_PROTECTED;
            }
        }
        if (containingDeclaration instanceof NamespaceDescriptor) {
            return ACC_PUBLIC;
        }
        return null;
    }

    public static Type unboxType(final Type type) {
        JvmPrimitiveType jvmPrimitiveType = JvmPrimitiveType.getByWrapperAsmType(type);
        if (jvmPrimitiveType != null) {
            return jvmPrimitiveType.getAsmType();
        }
        else {
            throw new UnsupportedOperationException("Unboxing: " + type);
        }
    }

    public static Type boxType(Type asmType) {
        JvmPrimitiveType jvmPrimitiveType = JvmPrimitiveType.getByAsmType(asmType);
        if (jvmPrimitiveType != null) {
            return jvmPrimitiveType.getWrapper().getAsmType();
        }
        else {
            return asmType;
        }
    }

    public static boolean isIntPrimitive(Type type) {
        return type == Type.INT_TYPE || type == Type.SHORT_TYPE || type == Type.BYTE_TYPE || type == Type.CHAR_TYPE;
    }

    public static boolean isNumberPrimitive(Type type) {
        return isIntPrimitive(type) || type == Type.FLOAT_TYPE || type == Type.DOUBLE_TYPE || type == Type.LONG_TYPE;
    }

    public static boolean isPrimitive(Type type) {
        return type.getSort() != Type.OBJECT && type.getSort() != Type.ARRAY;
    }

    public static boolean isPrimitiveNumberClassDescriptor(DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof ClassDescriptor)) {
            return false;
        }
        return PRIMITIVE_NUMBER_CLASSES.contains(descriptor);
    }

    public static Type correctElementType(Type type) {
        String internalName = type.getInternalName();
        assert internalName.charAt(0) == '[';
        return Type.getType(internalName.substring(1));
    }

    @Nullable
    public static String getLocalNameForObject(JetObjectDeclaration object) {
        PsiElement parent = object.getParent();
        if (parent instanceof JetClassObject) {
            return JvmAbi.CLASS_OBJECT_CLASS_NAME;
        }

        return null;
    }

    public static JetType getSuperClass(ClassDescriptor classDescriptor) {
        final List<ClassDescriptor> superclassDescriptors = DescriptorUtils.getSuperclassDescriptors(classDescriptor);
        for (ClassDescriptor descriptor : superclassDescriptors) {
            if (descriptor.getKind() != ClassKind.TRAIT) {
                return descriptor.getDefaultType();
            }
        }
        return JetStandardClasses.getAnyType();
    }

    public static <T extends CallableMemberDescriptor> T unwrapFakeOverride(T member) {
        while (member.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            //noinspection unchecked
            member = (T) member.getOverriddenDescriptors().iterator().next();
        }
        return member;
    }

    public static void checkMustGenerateCode(CallableMemberDescriptor descriptor) {
        if (descriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            throw new IllegalStateException("must not generate code for fake overrides");
        }
        if (descriptor.getKind() == CallableMemberDescriptor.Kind.SYNTHESIZED) {
            throw new IllegalStateException("code generation for synthesized members should be handled separately");
        }
    }

    public static void initSingletonField(PsiElement element, Type classAsmType, ClassBuilder builder, InstructionAdapter iv) {
        iv.anew(classAsmType);
        iv.dup();
        iv.invokespecial(classAsmType.getInternalName(), "<init>", "()V");
        iv.putstatic(classAsmType.getInternalName(), JvmAbi.INSTANCE_FIELD, classAsmType.getDescriptor());
    }
}
