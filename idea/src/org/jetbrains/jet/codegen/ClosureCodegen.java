/*
 * @author max
 */
package org.jetbrains.jet.codegen;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.JetType;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.signature.SignatureWriter;

import java.util.Arrays;
import java.util.List;

public class ClosureCodegen {
    private final GenerationState state;

    public ClosureCodegen(GenerationState state) {
        this.state = state;
    }

    public static Method erasedInvokeSignature(FunctionDescriptor fd) {
        boolean isExtensionFunction = fd.getReceiverType() != null;
        int paramCount = fd.getUnsubstitutedValueParameters().size();
        if (isExtensionFunction) {
            paramCount++;
        }

        Type[] args = new Type[paramCount];
        Arrays.fill(args, JetTypeMapper.TYPE_OBJECT);
        return new Method("invoke", JetTypeMapper.TYPE_OBJECT, args);
    }

    public Method invokeSignature(FunctionDescriptor fd) {
        return state.getTypeMapper().mapSignature("invoke", fd);
    }

    public GeneratedClosureDescriptor gen(JetFunctionLiteralExpression fun) {
        JetNamedDeclaration container = PsiTreeUtil.getParentOfType(fun, JetNamespace.class, JetClass.class, JetObjectDeclaration.class);

        final Pair<String, ClassVisitor> nameAndVisitor;
        if (container instanceof JetNamespace) {
            nameAndVisitor = state.forClosureIn((JetNamespace) container);
        }
        else {
            nameAndVisitor = state.forClosureIn(state.getBindingContext().getClassDescriptor((JetClassOrObject) container));
        }


        final FunctionDescriptor funDescriptor = (FunctionDescriptor) state.getBindingContext().getDeclarationDescriptor(fun);

        final ClassVisitor cv = nameAndVisitor.getSecond();
        final String name = nameAndVisitor.getFirst();

        SignatureWriter signatureWriter = new SignatureWriter();

        final List<ValueParameterDescriptor> parameters = funDescriptor.getUnsubstitutedValueParameters();
        final String funClass = getInternalClassName(funDescriptor);
        signatureWriter.visitClassType(funClass);
        for (ValueParameterDescriptor parameter : parameters) {
            appendType(signatureWriter, parameter.getOutType(), '=');
        }

        appendType(signatureWriter, funDescriptor.getUnsubstitutedReturnType(), '=');
        signatureWriter.visitEnd();

        cv.visit(Opcodes.V1_6,
                Opcodes.ACC_PUBLIC,
                name,
                null,
                funClass,
                new String[0]
                );

        final Method constructor = generateConstructor(cv, funClass);

        generateBridge(name, funDescriptor, cv);
        generateBody(name, funDescriptor, cv, container.getProject(), fun.getFunctionLiteral().getBodyExpression().getStatements());

        cv.visitEnd();

        return new GeneratedClosureDescriptor(name, constructor);
    }

    private void generateBody(String className, FunctionDescriptor funDescriptor, ClassVisitor cv, Project project, List<JetElement> body) {
        FunctionCodegen fc = new FunctionCodegen(null, cv, state);
        fc.generatedMethod(body, OwnerKind.IMPLEMENTATION, invokeSignature(funDescriptor), funDescriptor.getReceiverType(), funDescriptor.getUnsubstitutedValueParameters(), null);
    }

    private void generateBridge(String className, FunctionDescriptor funDescriptor, ClassVisitor cv) {
        final Method bridge = erasedInvokeSignature(funDescriptor);
        final Method delegate = invokeSignature(funDescriptor);

        final MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "invoke", bridge.getDescriptor(), state.getTypeMapper().genericSignature(funDescriptor), new String[0]);
        mv.visitCode();

        InstructionAdapter iv = new InstructionAdapter(mv);

        iv.load(0, Type.getObjectType(className));

        final JetType receiverType = funDescriptor.getReceiverType();
        int count = 1;
        if (receiverType != null) {
            StackValue.local(count, JetTypeMapper.TYPE_OBJECT).put(JetTypeMapper.TYPE_OBJECT, iv);
            StackValue.onStack(JetTypeMapper.TYPE_OBJECT).upcast(state.getTypeMapper().mapType(receiverType), iv);
            count++;
        }

        final List<ValueParameterDescriptor> params = funDescriptor.getUnsubstitutedValueParameters();
        for (ValueParameterDescriptor param : params) {
            StackValue.local(count, JetTypeMapper.TYPE_OBJECT).put(JetTypeMapper.TYPE_OBJECT, iv);
            StackValue.onStack(JetTypeMapper.TYPE_OBJECT).upcast(state.getTypeMapper().mapType(param.getOutType()), iv);
            count++;
        }

        iv.invokespecial(className, "invoke", delegate.getDescriptor());
        StackValue.onStack(delegate.getReturnType()).put(JetTypeMapper.TYPE_OBJECT, iv);

        iv.areturn(JetTypeMapper.TYPE_OBJECT);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private Method generateConstructor(ClassVisitor cv, String funClass) {
        final Method constructor = new Method("<init>", Type.VOID_TYPE, new Type[0]); // TODO:
        final MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", constructor.getDescriptor(), null, new String[0]);
        mv.visitCode();
        InstructionAdapter iv = new InstructionAdapter(mv);

        iv.load(0, Type.getObjectType(funClass));
        iv.invokespecial(funClass, "<init>", "()V");

        iv.visitInsn(Opcodes.RETURN);


        mv.visitMaxs(0, 0);
        mv.visitEnd();
        return constructor;
    }

    public static String getInternalClassName(FunctionDescriptor descriptor) {
        final int paramCount = descriptor.getUnsubstitutedValueParameters().size();
        if (descriptor.getReceiverType() != null) {
            return "jet/ExtensionFunction" + paramCount;
        }
        else {
            return "jet/Function" + paramCount;
        }
    }

    private void appendType(SignatureWriter signatureWriter, JetType type, char variance) {
        signatureWriter.visitTypeArgument(variance);

        final JetTypeMapper typeMapper = state.getTypeMapper();
        final Type rawRetType = typeMapper.boxType(typeMapper.mapType(type));
        signatureWriter.visitClassType(rawRetType.getInternalName());
        signatureWriter.visitEnd();
    }
}
