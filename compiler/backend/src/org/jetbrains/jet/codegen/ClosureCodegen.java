/*
 * @author max
 * @author alex.tkachman
 */
package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.Pair;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.JetFunctionLiteral;
import org.jetbrains.jet.lang.psi.JetFunctionLiteralExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.signature.SignatureWriter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class ClosureCodegen extends ObjectOrClosureCodegen {

    public ClosureCodegen(GenerationState state, ExpressionCodegen exprContext, CodegenContext context) {
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
        final Pair<String, ClassBuilder> nameAndVisitor = state.forAnonymousSubclass(fun);

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

        cv.defineClass(V1_6,
                       ACC_PUBLIC/*|ACC_SUPER*/,
                       name,
                       null,
                       funClass,
                       new String[0]
        );
        cv.visitSource(fun.getContainingFile().getName(), null);


        generateBridge(name, funDescriptor, fun, cv);
        captureThis = generateBody(funDescriptor, cv, fun.getFunctionLiteral());
        ClassDescriptor thisDescriptor = context.getThisDescriptor();
        final Type enclosingType = thisDescriptor == null ? null : Type.getObjectType(thisDescriptor.getName());
        if (enclosingType == null) captureThis = false;

        final Method constructor = generateConstructor(funClass, fun);

        if (captureThis) {
            cv.newField(fun, 0, "this$0", enclosingType.getDescriptor(), null, null);
        }

        if(isConst()) {
            generateConstInstance(fun);
        }

        cv.done();

        final GeneratedAnonymousClassDescriptor answer = new GeneratedAnonymousClassDescriptor(name, constructor, captureThis, captureReceiver);
        for (DeclarationDescriptor descriptor : closure.keySet()) {
            if(descriptor instanceof VariableDescriptor) {
                final EnclosedValueDescriptor valueDescriptor = closure.get(descriptor);
                answer.addArg(valueDescriptor.getOuterValue());
            }
        }
        return answer;
    }

    private void generateConstInstance(JetFunctionLiteralExpression fun) {
        String classDescr = "L" + name + ";";
        cv.newField(fun, ACC_PRIVATE | ACC_STATIC, "$instance", classDescr, null, null);

        MethodVisitor mv = cv.newMethod(fun, ACC_PUBLIC | ACC_STATIC, "$getInstance", "()" + classDescr, null, new String[0]);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, name, "$instance", classDescr);
        mv.visitInsn(DUP);
        Label ret = new Label();
        mv.visitJumpInsn(IFNONNULL, ret);

        mv.visitInsn(POP);
        mv.visitTypeInsn(NEW, name);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, name, "<init>", "()V");
        mv.visitInsn(DUP);
        mv.visitFieldInsn(PUTSTATIC, name, "$instance", classDescr);

        mv.visitLabel(ret);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0,0);
        mv.visitEnd();
    }

    private boolean generateBody(FunctionDescriptor funDescriptor, ClassBuilder cv, JetFunctionLiteral body) {
        int arity = funDescriptor.getValueParameters().size();

        ClassDescriptorImpl function = new ClassDescriptorImpl(
                funDescriptor,
                Collections.<AnnotationDescriptor>emptyList(),
                name);
        function = function.initialize(
                false,
                Collections.<TypeParameterDescriptor>emptyList(),
                Collections.singleton((funDescriptor.getReceiverParameter().exists() ? JetStandardClasses.getReceiverFunction(arity) : JetStandardClasses.getFunction(arity)).getDefaultType()), JetScope.EMPTY, Collections.<FunctionDescriptor>emptySet(), null);

        final CodegenContext.ClosureContext closureContext = context.intoClosure(funDescriptor, function, name, this);
        FunctionCodegen fc = new FunctionCodegen(closureContext, cv, state);
        fc.generateMethod(body, invokeSignature(funDescriptor), funDescriptor);
        return closureContext.outerWasUsed;
    }

    private void generateBridge(String className, FunctionDescriptor funDescriptor, JetFunctionLiteralExpression fun, ClassBuilder cv) {
        final Method bridge = erasedInvokeSignature(funDescriptor);
        final Method delegate = invokeSignature(funDescriptor);

        if(bridge.getDescriptor().equals(delegate.getDescriptor()))
            return;

        final MethodVisitor mv = cv.newMethod(fun, ACC_PUBLIC, "invoke", bridge.getDescriptor(), state.getTypeMapper().genericSignature(funDescriptor), new String[0]);
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

    private Method generateConstructor(String funClass, JetFunctionLiteralExpression fun) {
        int argCount = captureThis ? 1 : 0;

        for (DeclarationDescriptor descriptor : closure.keySet()) {
            if(descriptor instanceof VariableDescriptor) {
                argCount++;
            }
            else if(descriptor instanceof FunctionDescriptor) {
                captureReceiver = state.getTypeMapper().mapType(((FunctionDescriptor) descriptor).getReceiverParameter().getType());
                argCount++;
            }
        }

        Type[] argTypes = new Type[argCount];

        int i = 0;
        if (captureThis) {
            argTypes[i++] = Type.getObjectType(context.getThisDescriptor().getName());
        }

        if (captureReceiver != null) {
            argTypes[i++] = captureReceiver;
        }

        for (DeclarationDescriptor descriptor : closure.keySet()) {
            if(descriptor instanceof VariableDescriptor) {
                final Type sharedVarType = exprContext.getSharedVarType(descriptor);
                final Type type = sharedVarType != null ? sharedVarType : state.getTypeMapper().mapType(((VariableDescriptor) descriptor).getOutType());
                argTypes[i++] = type;
            }
        }

        final Method constructor = new Method("<init>", Type.VOID_TYPE, argTypes);
        final MethodVisitor mv = cv.newMethod(fun, ACC_PUBLIC, "<init>", constructor.getDescriptor(), null, new String[0]);
        mv.visitCode();
        InstructionAdapter iv = new InstructionAdapter(mv);
        ExpressionCodegen expressionCodegen = new ExpressionCodegen(mv, null, Type.VOID_TYPE, context, state);

        iv.load(0, Type.getObjectType(funClass));
//        expressionCodegen.generateTypeInfo(new ProjectionErasingJetType(returnType));
        iv.aconst(null); // @todo
        iv.invokespecial(funClass, "<init>", "(Ljet/typeinfo/TypeInfo;)V");

        i = 1;
        for (Type type : argTypes) {
            StackValue.local(0, JetTypeMapper.TYPE_OBJECT).put(JetTypeMapper.TYPE_OBJECT, iv);
            StackValue.local(i, type).put(type, iv);
            final String fieldName;
            if (captureThis && i == 1) {
                fieldName = "this$0";
            }
            else {
                if (captureReceiver != null && (captureThis && i == 2 || !captureThis && i == 1)) {
                    fieldName = "receiver$0";
                }
                else {
                    fieldName = "$" + (i);
                    i++;
                }
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
}
