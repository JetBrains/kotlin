/*
 * @author max
 */
package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.Pair;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.JetFunctionLiteral;
import org.jetbrains.jet.lang.psi.JetFunctionLiteralExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.signature.SignatureWriter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class ClosureCodegen extends FunctionOrClosureCodegen {

    public ClosureCodegen(GenerationState state, ExpressionCodegen exprContext, ClassContext context) {
        super(exprContext, context, state);
    }

    public static Method erasedInvokeSignature(FunctionDescriptor fd) {
        boolean isExtensionFunction = fd.getReceiverParameter().exists();
        int paramCount = fd.getValueParameters().size();
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

    public GeneratedAnonymousClassDescriptor gen(JetFunctionLiteralExpression fun) {
        final Pair<String, ClassVisitor> nameAndVisitor = state.forAnonymousSubclass(fun);

        final FunctionDescriptor funDescriptor = (FunctionDescriptor) state.getBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, fun);

        cv = nameAndVisitor.getSecond();
        name = nameAndVisitor.getFirst();

        SignatureWriter signatureWriter = new SignatureWriter();

        final List<ValueParameterDescriptor> parameters = funDescriptor.getValueParameters();
        final String funClass = getInternalClassName(funDescriptor);
        signatureWriter.visitClassType(funClass);
        for (ValueParameterDescriptor parameter : parameters) {
            appendType(signatureWriter, parameter.getOutType(), '=');
        }

        appendType(signatureWriter, funDescriptor.getReturnType(), '=');
        signatureWriter.visitEnd();

        cv.visit(V1_6,
                ACC_PUBLIC,
                name,
                null,
                funClass,
                new String[0]
        );
        cv.visitSource(fun.getContainingFile().getName(), null);


        generateBridge(name, funDescriptor, cv);
        captureThis = generateBody(funDescriptor, cv, fun.getFunctionLiteral());
        final Type enclosingType = context.enclosingClassType(state.getTypeMapper());
        if (enclosingType == null) captureThis = false;

        final Method constructor = generateConstructor(funClass, captureThis, funDescriptor.getReturnType());

        if (captureThis) {
            cv.visitField(0, "this$0", enclosingType.getDescriptor(), null, null);
        }

        if(isConst()) {
            generateConstInstance();
        }

        cv.visitEnd();

        final GeneratedAnonymousClassDescriptor answer = new GeneratedAnonymousClassDescriptor(name, constructor, captureThis);
        for (DeclarationDescriptor descriptor : closure.keySet()) {
            final EnclosedValueDescriptor valueDescriptor = closure.get(descriptor);
            answer.addArg(valueDescriptor.getOuterValue());
        }
        return answer;
    }

    private void generateConstInstance() {
        cv.visitField(ACC_PRIVATE| ACC_STATIC, "$instance", "Ljava/lang/ref/SoftReference;", null, null);

        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC | ACC_STATIC, "$getInstance", "()L" + name + ";", null, new String[0]);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, name, "$instance", "Ljava/lang/ref/SoftReference;");
        mv.visitInsn(DUP);
        Label makeNew = new Label();
        mv.visitJumpInsn(IFNULL, makeNew);

        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/ref/SoftReference", "get", "()Ljava/lang/Object;");
        mv.visitInsn(DUP);

        Label ret = new Label();
        mv.visitJumpInsn(IFNULL, makeNew);
        mv.visitTypeInsn(CHECKCAST, name);
        mv.visitJumpInsn(GOTO, ret);

        mv.visitLabel(makeNew);
        mv.visitInsn(POP);

        mv.visitTypeInsn(NEW, "java/lang/ref/SoftReference");
        mv.visitInsn(DUP);

        mv.visitTypeInsn(NEW, name);
        mv.visitInsn(DUP);
        mv.visitVarInsn(ASTORE, 0);

        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, name, "<init>", "()V");

        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/ref/SoftReference", "<init>", "(Ljava/lang/Object;)V");
        mv.visitFieldInsn(PUTSTATIC, name, "$instance", "Ljava/lang/ref/SoftReference;");

        mv.visitVarInsn(ALOAD, 0);

        mv.visitLabel(ret);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0,0);
        mv.visitEnd();
    }

    private boolean generateBody(FunctionDescriptor funDescriptor, ClassVisitor cv, JetFunctionLiteral body) {
        final ClassContext closureContext = context.intoClosure(name, this);
        FunctionCodegen fc = new FunctionCodegen(closureContext, cv, state);
        fc.generateMethod(body, invokeSignature(funDescriptor), funDescriptor);
        return closureContext.isThisWasUsed();
    }

    private void generateBridge(String className, FunctionDescriptor funDescriptor, ClassVisitor cv) {
        final Method bridge = erasedInvokeSignature(funDescriptor);
        final Method delegate = invokeSignature(funDescriptor);

        final MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "invoke", bridge.getDescriptor(), state.getTypeMapper().genericSignature(funDescriptor), new String[0]);
        mv.visitCode();

        InstructionAdapter iv = new InstructionAdapter(mv);

        iv.load(0, Type.getObjectType(className));

        final ReceiverDescriptor receiver = funDescriptor.getReceiverParameter();
        int count = 1;
        if (receiver.exists()) {
            StackValue.local(count, JetTypeMapper.TYPE_OBJECT).put(JetTypeMapper.TYPE_OBJECT, iv);
            StackValue.onStack(JetTypeMapper.TYPE_OBJECT).upcast(state.getTypeMapper().mapType(receiver.getType()), iv);
            count++;
        }

        final List<ValueParameterDescriptor> params = funDescriptor.getValueParameters();
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

    private Method generateConstructor(String funClass, boolean captureThis, JetType returnType) {
        int argCount = closure.size();

        if (captureThis) {
            argCount++;
        }

        Type[] argTypes = new Type[argCount];


        int i = 0;
        if (captureThis) {
            i = 1;
            argTypes[0] = context.enclosingClassType(state.getTypeMapper());
        }

        for (DeclarationDescriptor descriptor : closure.keySet()) {
            final Type sharedVarType = exprContext.getSharedVarType(descriptor);
            final Type type = sharedVarType != null ? sharedVarType : state.getTypeMapper().mapType(((VariableDescriptor) descriptor).getOutType());
            argTypes[i++] = type;
        }

        final Method constructor = new Method("<init>", Type.VOID_TYPE, argTypes);
        final MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "<init>", constructor.getDescriptor(), null, new String[0]);
        mv.visitCode();
        InstructionAdapter iv = new InstructionAdapter(mv);
        ExpressionCodegen expressionCodegen = new ExpressionCodegen(mv, null, Type.VOID_TYPE, context, state);

        iv.load(0, Type.getObjectType(funClass));
        expressionCodegen.generateTypeInfo(new ProjectionErasingJetType(returnType));
        iv.invokespecial(funClass, "<init>", "(Ljet/typeinfo/TypeInfo;)V");

        i = 1;
        for (Type type : argTypes) {
            StackValue.local(0, JetTypeMapper.TYPE_OBJECT).put(JetTypeMapper.TYPE_OBJECT, iv);
            StackValue.local(i, type).put(type, iv);
            final String fieldName;
            if (captureThis && i == 1) {
                fieldName = "this$0";
                captureThis = false;
            }
            else {
                fieldName = "$" + (i);
                i++;
            }

            StackValue.field(type, name, fieldName, false).store(iv);
        }

        iv.visitInsn(RETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
        return constructor;
    }

    public static String getInternalClassName(FunctionDescriptor descriptor) {
        final int paramCount = descriptor.getValueParameters().size();
        if (descriptor.getReceiverParameter().exists()) {
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

    public static CallableMethod asCallableMethod(FunctionDescriptor fd) {
        Method descriptor = erasedInvokeSignature(fd);
        String owner = getInternalClassName(fd);
        final CallableMethod result = new CallableMethod(owner, descriptor, INVOKEVIRTUAL, Arrays.asList(descriptor.getArgumentTypes()));
        if (fd.getReceiverParameter().exists()) {
            result.setNeedsReceiver(null);
        }
        result.requestGenerateCallee(Type.getObjectType(getInternalClassName(fd)));
        return result;
    }
}
