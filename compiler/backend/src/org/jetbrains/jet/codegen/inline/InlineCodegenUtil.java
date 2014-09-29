/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.inline;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.PackageContext;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.descriptors.serialization.JavaProtoBuf;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedSimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.kotlin.DeserializedResolverUtils;
import org.jetbrains.jet.lang.resolve.kotlin.PackagePartClassUtils;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileFinder;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.org.objectweb.asm.*;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.tree.*;
import org.jetbrains.org.objectweb.asm.util.Textifier;
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.ListIterator;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getFqName;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isTrait;

public class InlineCodegenUtil {
    public static final int API = Opcodes.ASM5;
    public static final String INVOKE = "invoke";

    public static final String CAPTURED_FIELD_PREFIX = "$";

    public static final String THIS$0 = "this$0";

    public static final String RECEIVER$0 = "receiver$0";

    public static final String NON_LOCAL_RETURN = "$$$$$NON_LOCAL_RETURN$$$$$";

    public static final String ROOT_LABEL = "$$$$$ROOT$$$$$";
    public static final String INLINE_MARKER_CLASS_NAME = "kotlin/jvm/internal/InlineMarker";
    public static final String INLINE_MARKER_BEFORE_METHOD_NAME = "beforeInlineCall";
    public static final String INLINE_MARKER_AFTER_METHOD_NAME = "afterInlineCall";

    @Nullable
    public static MethodNode getMethodNode(
            byte[] classData,
            final String methodName,
            final String methodDescriptor
    ) throws ClassNotFoundException, IOException {
        ClassReader cr = new ClassReader(classData);
        final MethodNode[] methodNode = new MethodNode[1];
        cr.accept(new ClassVisitor(API) {

            @Override
            public MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String desc, String signature, String[] exceptions) {
                if (methodName.equals(name) && methodDescriptor.equals(desc)) {
                    return methodNode[0] = new MethodNode(access, name, desc, signature, exceptions);
                }
                return null;
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        return methodNode[0];
    }


    @NotNull
    public static VirtualFile getVirtualFileForCallable(@NotNull DeserializedSimpleFunctionDescriptor deserializedDescriptor, @NotNull GenerationState state) {
        VirtualFile file;
        DeclarationDescriptor parentDeclaration = deserializedDescriptor.getContainingDeclaration();
        if (parentDeclaration instanceof PackageFragmentDescriptor) {
            ProtoBuf.Callable proto = deserializedDescriptor.getProto();
            if (!proto.hasExtension(JavaProtoBuf.implClassName)) {
                throw new IllegalStateException("Function in namespace should have implClassName property in proto: " + deserializedDescriptor);
            }
            Name name = deserializedDescriptor.getNameResolver().getName(proto.getExtension(JavaProtoBuf.implClassName));
            FqName packagePartFqName =
                    PackageClassUtils.getPackageClassFqName(((PackageFragmentDescriptor) parentDeclaration).getFqName()).parent().child(
                            name);
            file = findVirtualFileWithHeader(state.getProject(), packagePartFqName);
        } else {
            file = findVirtualFileContainingDescriptor(state.getProject(), deserializedDescriptor);
        }

        if (file == null) {
            throw new IllegalStateException("Couldn't find declaration file for " + deserializedDescriptor.getName());
        }

        return file;
    }

    @Nullable
    public static VirtualFile findVirtualFileWithHeader(@NotNull Project project, @NotNull FqName containerFqName) {
        VirtualFileFinder fileFinder = VirtualFileFinder.SERVICE.getInstance(project);
        return fileFinder.findVirtualFileWithHeader(containerFqName);
    }

    @Nullable
    public static VirtualFile findVirtualFile(@NotNull Project project, @NotNull String internalName) {
        VirtualFileFinder fileFinder = VirtualFileFinder.SERVICE.getInstance(project);
        return fileFinder.findVirtualFile(internalName);
    }

    //TODO: navigate to inner classes
    @Nullable
    public static FqName getContainerFqName(@NotNull DeclarationDescriptor referencedDescriptor) {
        ClassOrPackageFragmentDescriptor
                containerDescriptor = DescriptorUtils.getParentOfType(referencedDescriptor, ClassOrPackageFragmentDescriptor.class, false);
        if (containerDescriptor instanceof PackageFragmentDescriptor) {
            return PackageClassUtils.getPackageClassFqName(getFqName(containerDescriptor).toSafe());
        }
        if (containerDescriptor instanceof ClassDescriptor) {
            FqName fqName = DeserializedResolverUtils.kotlinFqNameToJavaFqName(getFqName(containerDescriptor));
            if (isTrait(containerDescriptor)) {
                return fqName.parent().child(Name.identifier(fqName.shortName() + JvmAbi.TRAIT_IMPL_SUFFIX));
            }
            return fqName;
        }
        return null;
    }

    public static String getInlineName(@NotNull CodegenContext codegenContext, @NotNull JetTypeMapper typeMapper) {
        return getInlineName(codegenContext, codegenContext.getContextDescriptor(), typeMapper);
    }

    private static String getInlineName(@NotNull CodegenContext codegenContext, @NotNull DeclarationDescriptor currentDescriptor, @NotNull JetTypeMapper typeMapper) {
        if (currentDescriptor instanceof PackageFragmentDescriptor) {
            PsiFile file = getContainingFile(codegenContext);

            Type packagePartType;
            if (file == null) {
                //in case package fragment clinit
                assert codegenContext instanceof PackageContext : "Expected package context but " + codegenContext;
                packagePartType = ((PackageContext) codegenContext).getPackagePartType();
            } else {
                packagePartType = PackagePartClassUtils.getPackagePartType((JetFile) file);
            }

            if (packagePartType == null) {
                DeclarationDescriptor contextDescriptor = codegenContext.getContextDescriptor();
                //noinspection ConstantConditions
                throw new RuntimeException("Couldn't find declaration for " + contextDescriptor.getContainingDeclaration().getName() + "." + contextDescriptor.getName() );
            }

            return packagePartType.getInternalName();
        }
        else if (currentDescriptor instanceof ClassifierDescriptor) {
            Type type = typeMapper.mapType((ClassifierDescriptor) currentDescriptor);
            return type.getInternalName();
        } else if (currentDescriptor instanceof FunctionDescriptor) {
            ClassDescriptor descriptor =
                    typeMapper.getBindingContext().get(CodegenBinding.CLASS_FOR_FUNCTION, (FunctionDescriptor) currentDescriptor);
            if (descriptor != null) {
                Type type = typeMapper.mapType(descriptor);
                return type.getInternalName();
            }
        }

        //TODO: add suffix for special case
        String suffix = currentDescriptor.getName().isSpecial() ? "" : currentDescriptor.getName().asString();

        //noinspection ConstantConditions
        return getInlineName(codegenContext, currentDescriptor.getContainingDeclaration(), typeMapper) + "$" + suffix;
    }

    @Nullable
    private static VirtualFile findVirtualFileContainingDescriptor(
            @NotNull Project project,
            @NotNull DeclarationDescriptor referencedDescriptor
    ) {
        FqName containerFqName = getContainerFqName(referencedDescriptor);
        if (containerFqName == null) {
            return null;
        }
        return findVirtualFileWithHeader(project, containerFqName);
    }


    public static boolean isInvokeOnLambda(String owner, String name) {
        if (!INVOKE.equals(name)) {
            return false;
        }

        for (String prefix : Arrays.asList("kotlin/Function", "kotlin/ExtensionFunction")) {
            if (owner.startsWith(prefix)) {
                String suffix = owner.substring(prefix.length());
                if (isInteger(suffix)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isAnonymousConstructorCall(@NotNull String internalName, @NotNull String methodName) {
        return "<init>".equals(methodName) && isAnonymousClass(internalName);
    }

    public static boolean isAnonymousClass(String internalName) {
        String shortName = getLastNamePart(internalName);
        int index = shortName.lastIndexOf("$");

        if (index < 0) {
            return false;
        }

        String suffix = shortName.substring(index + 1);
        return isInteger(suffix);
    }

    @NotNull
    private static String getLastNamePart(@NotNull String internalName) {
        int index = internalName.lastIndexOf("/");
        return index < 0 ? internalName : internalName.substring(index + 1);
    }

    @Nullable
    public static PsiFile getContainingFile(CodegenContext codegenContext) {
        DeclarationDescriptor contextDescriptor = codegenContext.getContextDescriptor();
        PsiElement psiElement = DescriptorToSourceUtils.descriptorToDeclaration(contextDescriptor);
        if (psiElement != null) {
            return psiElement.getContainingFile();
        }
        return null;
    }

    @NotNull
    public static MethodVisitor wrapWithMaxLocalCalc(@NotNull MethodNode methodNode) {
        return new MaxStackFrameSizeAndLocalsCalculator(API, methodNode.access, methodNode.desc, methodNode);
    }

    private static boolean isInteger(@NotNull String string) {
        if (string.isEmpty()) {
            return false;
        }

        for (int i = 0; i < string.length(); i++) {
             if (!Character.isDigit(string.charAt(i))) {
                 return false;
             }
        }

        return true;
    }

    public static boolean isCapturedFieldName(@NotNull String fieldName) {
        // TODO: improve this heuristic
        return (fieldName.startsWith(CAPTURED_FIELD_PREFIX) && !fieldName.equals(JvmAbi.KOTLIN_CLASS_FIELD_NAME)) ||
               THIS$0.equals(fieldName) ||
               RECEIVER$0.equals(fieldName);
    }

    public static boolean isReturnOpcode(int opcode) {
        return opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN;
    }

    //marked return could be either non-local or local in case of labeled lambda self-returns
    public static boolean isMarkedReturn(@NotNull AbstractInsnNode returnIns) {
        assert isReturnOpcode(returnIns.getOpcode()) : "Should be called on return instruction, but " + returnIns;
        AbstractInsnNode globalFlag = returnIns.getPrevious();
        return globalFlag instanceof MethodInsnNode && NON_LOCAL_RETURN.equals(((MethodInsnNode)globalFlag).owner);
    }

    public static void generateGlobalReturnFlag(@NotNull InstructionAdapter iv, @NotNull String labelName) {
        iv.invokestatic(NON_LOCAL_RETURN, labelName, "()V", false);
    }

    public static Type getReturnType(int opcode) {
        switch (opcode) {
            case Opcodes.RETURN: return Type.VOID_TYPE;
            case Opcodes.IRETURN: return Type.INT_TYPE;
            case Opcodes.DRETURN: return Type.DOUBLE_TYPE;
            case Opcodes.FRETURN: return Type.FLOAT_TYPE;
            case Opcodes.LRETURN: return Type.LONG_TYPE;
            default: return AsmTypeConstants.OBJECT_TYPE;
        }
    }

    public static void insertNodeBefore(@NotNull MethodNode from, @NotNull MethodNode to, @NotNull AbstractInsnNode beforeNode) {
        InsnList instructions = to.instructions;
        ListIterator<AbstractInsnNode> iterator = from.instructions.iterator();
        while (iterator.hasNext()) {
            AbstractInsnNode next = iterator.next();
            instructions.insertBefore(beforeNode, next);
        }
    }


    public static MethodNode createEmptyMethodNode() {
        return new MethodNode(API, 0, "fake", "()V", null, null);
    }

    private static boolean isLastGoto(@NotNull AbstractInsnNode insnNode, @NotNull AbstractInsnNode stopAt) {
        if (insnNode.getOpcode() == Opcodes.GOTO) {
            insnNode = insnNode.getNext();
            while (insnNode != stopAt && isLineNumberOrLabel(insnNode)) {
                insnNode = insnNode.getNext();
            }
            return stopAt == insnNode;
        }
        return false;
    }

    static boolean isLineNumberOrLabel(@Nullable AbstractInsnNode node) {
        return node instanceof LineNumberNode || node instanceof LabelNode;
    }


    @NotNull
    public static LabelNode firstLabelInChain(@NotNull LabelNode node) {
        LabelNode curNode = node;
        while (curNode.getPrevious() instanceof LabelNode) {
            curNode = (LabelNode) curNode.getPrevious();
        }
        return curNode;
    }

    @NotNull
    public static String getNodeText(@Nullable MethodNode node) {
        return getNodeText(node, new Textifier());
    }

    @NotNull
    public static String getNodeText(@Nullable MethodNode node, @NotNull Textifier textifier) {
        if (node == null) {
            return "Not generated";
        }
        node.accept(new TraceMethodVisitor(textifier));
        StringWriter sw = new StringWriter();
        textifier.print(new PrintWriter(sw));
        sw.flush();
        return node.name + " " + node.desc + ": \n " + sw.getBuffer().toString();
    }

    public static class LabelTextifier extends Textifier {

        public LabelTextifier() {
            super(API);
        }

        @Nullable
        @TestOnly
        @SuppressWarnings("UnusedDeclaration")
        public String getLabelNameIfExists(@NotNull Label l) {
            return labelNames == null ? null : labelNames.get(l);
        }
    }

    public static void addInlineMarker(
            @NotNull InstructionAdapter v,
            boolean isStartNotEnd
    ) {
        v.visitMethodInsn(Opcodes.INVOKESTATIC, INLINE_MARKER_CLASS_NAME,
                          (isStartNotEnd ? INLINE_MARKER_BEFORE_METHOD_NAME : INLINE_MARKER_AFTER_METHOD_NAME),
                          "()V", false);
    }
}
