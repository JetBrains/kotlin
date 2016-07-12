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

package org.jetbrains.kotlin.codegen.inline;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.codegen.MemberCodegen;
import org.jetbrains.kotlin.codegen.binding.CodegenBinding;
import org.jetbrains.kotlin.codegen.context.CodegenContext;
import org.jetbrains.kotlin.codegen.context.CodegenContextUtil;
import org.jetbrains.kotlin.codegen.context.InlineLambdaContext;
import org.jetbrains.kotlin.codegen.context.MethodContext;
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicArrayConstructorsKt;
import org.jetbrains.kotlin.codegen.optimization.common.UtilKt;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.codegen.when.WhenByEnumsMapping;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.fileClasses.FileClasses;
import org.jetbrains.kotlin.fileClasses.JvmFileClassesProvider;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinder;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;
import org.jetbrains.kotlin.util.OperatorNameConventions;
import org.jetbrains.org.objectweb.asm.*;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.tree.*;
import org.jetbrains.org.objectweb.asm.util.Textifier;
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ListIterator;

public class InlineCodegenUtil {
    public static final boolean GENERATE_SMAP = true;
    public static final int API = Opcodes.ASM5;

    public static final String CAPTURED_FIELD_PREFIX = "$";
    public static final String NON_CAPTURED_FIELD_PREFIX = "$$";
    public static final String THIS$0 = "this$0";
    public static final String THIS = "this";
    public static final String RECEIVER$0 = "receiver$0";
    public static final String NON_LOCAL_RETURN = "$$$$$NON_LOCAL_RETURN$$$$$";
    public static final String FIRST_FUN_LABEL = "$$$$$ROOT$$$$$";
    public static final String NUMBERED_FUNCTION_PREFIX = "kotlin/jvm/functions/Function";
    public static final String INLINE_MARKER_CLASS_NAME = "kotlin/jvm/internal/InlineMarker";
    public static final String INLINE_MARKER_BEFORE_METHOD_NAME = "beforeInlineCall";
    public static final String INLINE_MARKER_AFTER_METHOD_NAME = "afterInlineCall";
    public static final String INLINE_MARKER_FINALLY_START = "finallyStart";
    public static final String INLINE_MARKER_FINALLY_END = "finallyEnd";
    public static final String INLINE_TRANSFORMATION_SUFFIX = "$inlined";
    public static final String INLINE_FUN_THIS_0_SUFFIX = "$inline_fun";
    public static final String INLINE_FUN_VAR_SUFFIX = "$iv";

    @Nullable
    public static SMAPAndMethodNode getMethodNode(
            byte[] classData,
            final String methodName,
            final String methodDescriptor,
            ClassId classId
    ) {
        ClassReader cr = new ClassReader(classData);
        final MethodNode[] node = new MethodNode[1];
        final String[] debugInfo = new String[2];
        final int[] lines = new int[2];
        lines[0] = Integer.MAX_VALUE;
        lines[1] = Integer.MIN_VALUE;
        cr.accept(new ClassVisitor(API) {
            @Override
            public void visit(int version, int access, @NotNull String name, String signature, String superName, String[] interfaces) {
                assertVersionNotGreaterThanJava6(version, name);
            }

            @Override
            public void visitSource(String source, String debug) {
                super.visitSource(source, debug);
                debugInfo[0] = source;
                debugInfo[1] = debug;
            }

            @Override
            public MethodVisitor visitMethod(
                    int access,
                    @NotNull String name,
                    @NotNull String desc,
                    String signature,
                    String[] exceptions
            ) {
                if (methodName.equals(name) && methodDescriptor.equals(desc)) {
                    node[0] = new MethodNode(API, access, name, desc, signature, exceptions) {
                        @Override
                        public void visitLineNumber(int line, @NotNull Label start) {
                            super.visitLineNumber(line, start);
                            lines[0] = Math.min(lines[0], line);
                            lines[1] = Math.max(lines[1], line);
                        }
                    };
                    return node[0];
                }
                return null;
            }
        }, ClassReader.SKIP_FRAMES | (GENERATE_SMAP ? 0 : ClassReader.SKIP_DEBUG));

        if (node[0] == null) {
            return null;
        }

        if (classId.equals(IntrinsicArrayConstructorsKt.getClassId())) {
            // Don't load source map for intrinsic array constructors
            debugInfo[0] = null;
        }

        SMAP smap = SMAPParser.parseOrCreateDefault(debugInfo[1], debugInfo[0], classId.asString(), lines[0], lines[1]);
        return new SMAPAndMethodNode(node[0], smap);
    }

    public static void assertVersionNotGreaterThanJava6(int version, String internalName) {
        // TODO: report a proper diagnostic
        if (version > Opcodes.V1_6 && !"true".equals(System.getProperty("kotlin.skip.bytecode.version.check"))) {
            throw new UnsupportedOperationException(
                    "Cannot inline bytecode of class " + internalName + " which has version " + version + ". " +
                    "This compiler can only inline Java 1.6 bytecode (version " + Opcodes.V1_6 + ")"
            );
        }
    }

    public static void initDefaultSourceMappingIfNeeded(
            @NotNull CodegenContext context, @NotNull MemberCodegen codegen, @NotNull GenerationState state
    ) {
        if (state.isInlineDisabled()) return;

        CodegenContext<?> parentContext = context.getParentContext();
        while (parentContext != null) {
            if (parentContext.isInlineMethodContext()) {
                //just init default one to one mapping
                codegen.getOrCreateSourceMapper();
                break;
            }
            parentContext = parentContext.getParentContext();
        }
    }

    @Nullable
    public static VirtualFile findVirtualFile(@NotNull GenerationState state, @NotNull ClassId classId) {
        return JvmVirtualFileFinder.SERVICE.getInstance(state.getProject()).findVirtualFileWithHeader(classId);
    }

    @Nullable
    public static VirtualFile findVirtualFileImprecise(@NotNull GenerationState state, @NotNull String internalClassName) {
        FqName packageFqName = JvmClassName.byInternalName(internalClassName).getPackageFqName();
        String classNameWithDollars = StringsKt.substringAfterLast(internalClassName, "/", internalClassName);
        //TODO: we cannot construct proper classId at this point, we need to read InnerClasses info from class file
        // we construct valid.package.name/RelativeClassNameAsSingleName that should work in compiler, but fails for inner classes in IDE
        return findVirtualFile(state, new ClassId(packageFqName, Name.identifier(classNameWithDollars)));
    }

    public static String getInlineName(
            @NotNull CodegenContext codegenContext,
            @NotNull KotlinTypeMapper typeMapper,
            @NotNull JvmFileClassesProvider fileClassesManager
    ) {
        return getInlineName(codegenContext, codegenContext.getContextDescriptor(), typeMapper, fileClassesManager);
    }

    private static String getInlineName(
            @NotNull CodegenContext codegenContext,
            @NotNull DeclarationDescriptor currentDescriptor,
            @NotNull KotlinTypeMapper typeMapper,
            @NotNull JvmFileClassesProvider fileClassesProvider
    ) {
        if (currentDescriptor instanceof PackageFragmentDescriptor) {
            PsiFile file = getContainingFile(codegenContext);

            Type implementationOwnerType;
            if (file == null) {
                implementationOwnerType = CodegenContextUtil.getImplementationOwnerClassType(codegenContext);
            } else {
                implementationOwnerType = FileClasses.getFileClassType(fileClassesProvider, (KtFile) file);
            }

            if (implementationOwnerType == null) {
                DeclarationDescriptor contextDescriptor = codegenContext.getContextDescriptor();
                //noinspection ConstantConditions
                throw new RuntimeException("Couldn't find declaration for " +
                                           contextDescriptor.getContainingDeclaration().getName() + "." + contextDescriptor.getName() +
                                           "; context: " + codegenContext);
            }

            return implementationOwnerType.getInternalName();
        }
        else if (currentDescriptor instanceof ClassifierDescriptor) {
            Type type = typeMapper.mapType((ClassifierDescriptor) currentDescriptor);
            return type.getInternalName();
        } else if (currentDescriptor instanceof FunctionDescriptor) {
            ClassDescriptor descriptor =
                    typeMapper.getBindingContext().get(CodegenBinding.CLASS_FOR_CALLABLE, (FunctionDescriptor) currentDescriptor);
            if (descriptor != null) {
                Type type = typeMapper.mapType(descriptor);
                return type.getInternalName();
            }
        }

        //TODO: add suffix for special case
        String suffix = currentDescriptor.getName().isSpecial() ? "" : currentDescriptor.getName().asString();

        //noinspection ConstantConditions
        return getInlineName(codegenContext, currentDescriptor.getContainingDeclaration(), typeMapper, fileClassesProvider) + "$" + suffix;
    }

    public static boolean isInvokeOnLambda(@NotNull String owner, @NotNull String name) {
        return OperatorNameConventions.INVOKE.asString().equals(name) &&
               owner.startsWith(NUMBERED_FUNCTION_PREFIX) &&
               isInteger(owner.substring(NUMBERED_FUNCTION_PREFIX.length()));
    }

    public static boolean isAnonymousConstructorCall(@NotNull String internalName, @NotNull String methodName) {
        return "<init>".equals(methodName) && isAnonymousClass(internalName);
    }

    public static boolean isWhenMappingAccess(@NotNull String internalName, @NotNull String fieldName) {
        return fieldName.startsWith(WhenByEnumsMapping.MAPPING_ARRAY_FIELD_PREFIX) && internalName.endsWith(WhenByEnumsMapping.MAPPINGS_CLASS_NAME_POSTFIX);
    }

    public static boolean isAnonymousSingletonLoad(@NotNull String internalName, @NotNull String fieldName) {
        return JvmAbi.INSTANCE_FIELD.equals(fieldName) && isAnonymousClass(internalName);
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
        return fieldName.startsWith(CAPTURED_FIELD_PREFIX) &&
               !fieldName.startsWith(NON_CAPTURED_FIELD_PREFIX) ||
               THIS$0.equals(fieldName) ||
               RECEIVER$0.equals(fieldName);
    }

    public static boolean isReturnOpcode(int opcode) {
        return opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN;
    }

    //marked return could be either non-local or local in case of labeled lambda self-returns
    public static boolean isMarkedReturn(@NotNull AbstractInsnNode returnIns) {
        return getMarkedReturnLabelOrNull(returnIns) != null;
    }

    public static @Nullable String getMarkedReturnLabelOrNull(@NotNull AbstractInsnNode returnInsn) {
        if (!isReturnOpcode(returnInsn.getOpcode())) {
            return null;
        }
        AbstractInsnNode previous = returnInsn.getPrevious();
        if (previous instanceof MethodInsnNode) {
            MethodInsnNode marker = (MethodInsnNode) previous;
            if (NON_LOCAL_RETURN.equals(marker.owner)) {
                return marker.name;
            }
        }
        return null;
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
            default: return AsmTypes.OBJECT_TYPE;
        }
    }

    public static void insertNodeBefore(@NotNull MethodNode from, @NotNull InsnList instructions, @NotNull AbstractInsnNode beforeNode) {
        ListIterator<AbstractInsnNode> iterator = from.instructions.iterator();
        while (iterator.hasNext()) {
            AbstractInsnNode next = iterator.next();
            instructions.insertBefore(beforeNode, next);
        }
    }

    public static void insertNodeBefore(@NotNull MethodNode from, @NotNull MethodNode to, @NotNull AbstractInsnNode beforeNode) {
        insertNodeBefore(from, to.instructions, beforeNode);
    }


    public static MethodNode createEmptyMethodNode() {
        return new MethodNode(API, 0, "fake", "()V", null, null);
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

    @NotNull
    /* package */ static ClassReader buildClassReaderByInternalName(@NotNull GenerationState state, @NotNull String internalName) {
        //try to find just compiled classes then in dependencies
        try {
            OutputFile outputFile = state.getFactory().get(internalName + ".class");
            if (outputFile != null) {
                return new ClassReader(outputFile.asByteArray());
            }
            else {
                VirtualFile file = findVirtualFileImprecise(state, internalName);
                if (file == null) {
                    throw new RuntimeException("Couldn't find virtual file for " + internalName);
                }
                return new ClassReader(file.contentsToByteArray());
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void generateFinallyMarker(@NotNull InstructionAdapter v, int depth, boolean start) {
        v.iconst(depth);
        v.invokestatic(INLINE_MARKER_CLASS_NAME, start ? INLINE_MARKER_FINALLY_START : INLINE_MARKER_FINALLY_END, "(I)V", false);
    }

    public static boolean isFinallyEnd(@NotNull AbstractInsnNode node) {
        return isFinallyMarker(node, INLINE_MARKER_FINALLY_END);
    }

    public static boolean isFinallyStart(@NotNull AbstractInsnNode node) {
        return isFinallyMarker(node, INLINE_MARKER_FINALLY_START);
    }

    public static boolean isFinallyMarker(@Nullable AbstractInsnNode node) {
        return isFinallyMarker(node, INLINE_MARKER_FINALLY_END) || isFinallyMarker(node, INLINE_MARKER_FINALLY_START);
    }

    public static boolean isFinallyMarker(@Nullable AbstractInsnNode node, String name) {
        if (!(node instanceof MethodInsnNode)) return false;
        MethodInsnNode method = (MethodInsnNode) node;
        return INLINE_MARKER_CLASS_NAME.equals(method.owner) && name.equals(method.name);
    }

    public static boolean isFinallyMarkerRequired(@NotNull MethodContext context) {
        return context.isInlineMethodContext() || context instanceof InlineLambdaContext;
    }

    public static int getConstant(AbstractInsnNode ins) {
        int opcode = ins.getOpcode();
        Integer value;
        if (opcode >= Opcodes.ICONST_0 && opcode <= Opcodes.ICONST_5) {
            value = opcode - Opcodes.ICONST_0;
        }
        else if (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) {
            IntInsnNode index = (IntInsnNode) ins;
            value = index.operand;
        }
        else {
            LdcInsnNode index = (LdcInsnNode) ins;
            value = (Integer) index.cst;
        }
        return value;
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

    public static boolean isInlineMarker(AbstractInsnNode insn) {
        return isInlineMarker(insn, null);
    }

    public static boolean isInlineMarker(AbstractInsnNode insn, String name) {
        if (insn instanceof MethodInsnNode) {
            MethodInsnNode methodInsnNode = (MethodInsnNode) insn;
            return insn.getOpcode() == Opcodes.INVOKESTATIC &&
                   methodInsnNode.owner.equals(INLINE_MARKER_CLASS_NAME) &&
                   (name != null ? methodInsnNode.name.equals(name)
                                 : methodInsnNode.name.equals(INLINE_MARKER_BEFORE_METHOD_NAME) ||
                                   methodInsnNode.name.equals(INLINE_MARKER_AFTER_METHOD_NAME));
        }
        else {
            return false;
        }
    }

    public static boolean isBeforeInlineMarker(AbstractInsnNode insn) {
        return isInlineMarker(insn, INLINE_MARKER_BEFORE_METHOD_NAME);
    }

    public static boolean isAfterInlineMarker(AbstractInsnNode insn) {
        return isInlineMarker(insn, INLINE_MARKER_AFTER_METHOD_NAME);
    }

    public static int getLoadStoreArgSize(int opcode) {
        return opcode == Opcodes.DSTORE || opcode == Opcodes.LSTORE || opcode == Opcodes.DLOAD || opcode == Opcodes.LLOAD ? 2 : 1;
    }

    public static boolean isStoreInstruction(int opcode) {
        return opcode >= Opcodes.ISTORE && opcode <= Opcodes.ASTORE;
    }

    public static int calcMarkerShift(Parameters parameters, MethodNode node) {
        int markerShiftTemp = getIndexAfterLastMarker(node);
        return markerShiftTemp - parameters.getRealArgsSizeOnStack() + parameters.getArgsSizeOnStack();
    }

    protected static int getIndexAfterLastMarker(MethodNode node) {
        int markerShiftTemp = -1;
        for (LocalVariableNode variable : node.localVariables) {
            if (isFakeLocalVariableForInline(variable.name)) {
                markerShiftTemp = Math.max(markerShiftTemp, variable.index + 1);
            }
        }
        return markerShiftTemp;
    }

    public static boolean isFakeLocalVariableForInline(@NotNull String name) {
        return name.startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION) || name.startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT);
    }

    public static boolean isThis0(String name) {
        return THIS$0.equals(name);
    }

    @Nullable
    public static AbstractInsnNode getPrevMeaningful(@NotNull AbstractInsnNode node) {
        AbstractInsnNode result = node.getPrevious();
        while (result != null && !UtilKt.isMeaningful(result)) {
            result = result.getPrevious();
        }
        return result;
    }

    public static void removeInterval(@NotNull MethodNode node, @NotNull AbstractInsnNode startInc, @NotNull AbstractInsnNode endInc) {
        while (startInc != endInc) {
            AbstractInsnNode next = startInc.getNext();
            node.instructions.remove(startInc);
            startInc = next;
        }
        node.instructions.remove(startInc);
    }
}
