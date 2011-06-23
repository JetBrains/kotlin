package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;

import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class FunctionCodegen {
    private final JetDeclaration owner;
    private final ClassVisitor v;
    private final BindingContext bindingContext;
    private final JetTypeMapper typeMapper;
    private final Codegens factory;

    public FunctionCodegen(JetDeclaration owner, ClassVisitor v, JetStandardLibrary standardLibrary, BindingContext bindingContext, Codegens factory) {
        this.owner = owner;
        this.v = v;
        this.bindingContext = bindingContext;
        this.factory = factory;
        typeMapper = new JetTypeMapper(standardLibrary, bindingContext);
    }

    public void gen(JetNamedFunction f, OwnerKind kind) {
        final JetTypeReference receiverTypeRef = f.getReceiverTypeRef();
        final JetType receiverType = receiverTypeRef == null ? null : bindingContext.resolveTypeReference(receiverTypeRef);
        Method method = typeMapper.mapSignature(f);
        List<ValueParameterDescriptor> paramDescrs = bindingContext.getFunctionDescriptor(f).getUnsubstitutedValueParameters();
        generateMethod(f, kind, method, receiverType, paramDescrs);
    }

    public void generateMethod(JetDeclarationWithBody f,
                               OwnerKind kind,
                               Method jvmSignature,
                               @Nullable JetType receiverType,
                               List<ValueParameterDescriptor> paramDescrs) {
        final DeclarationDescriptor contextDesc = owner instanceof JetClassOrObject
                ? bindingContext.getClassDescriptor((JetClassOrObject) owner)
                : bindingContext.getNamespaceDescriptor((JetNamespace) owner);
        final JetExpression bodyExpression = f.getBodyExpression();
        final List<JetElement> bodyExpressions = bodyExpression != null ? Collections.<JetElement>singletonList(bodyExpression) : null;
        generatedMethod(bodyExpressions, kind, jvmSignature, receiverType, paramDescrs, contextDesc);
    }

    public void generatedMethod(List<JetElement> bodyExpressions,
                                OwnerKind kind,
                                Method jvmSignature,
                                JetType receiverType,
                                List<ValueParameterDescriptor> paramDescrs,
                                DeclarationDescriptor contextDesc)
    {
        int flags = Opcodes.ACC_PUBLIC; // TODO.

        boolean isStatic = kind == OwnerKind.NAMESPACE;
        if (isStatic) flags |= Opcodes.ACC_STATIC;

        boolean isAbstract = kind == OwnerKind.INTERFACE || bodyExpressions == null;
        if (isAbstract) flags |= Opcodes.ACC_ABSTRACT;

        if (isAbstract && (kind == OwnerKind.IMPLEMENTATION || kind == OwnerKind.DELEGATING_IMPLEMENTATION)) {
            return;
        }

        final MethodVisitor mv = v.visitMethod(flags, jvmSignature.getName(), jvmSignature.getDescriptor(), null, null);
        if (kind != OwnerKind.INTERFACE) {
            mv.visitCode();
            FrameMap frameMap = new FrameMap();

            if (kind != OwnerKind.NAMESPACE || receiverType != null) {
                frameMap.enterTemp();  // 0 slot for this
            }

            Type[] argTypes = jvmSignature.getArgumentTypes();
            for (int i = 0; i < paramDescrs.size(); i++) {
                ValueParameterDescriptor parameter = paramDescrs.get(i);
                frameMap.enter(parameter, argTypes[i].getSize());
            }

            StackValue thisExpression = receiverType == null ? null : StackValue.local(0, typeMapper.mapType(receiverType));
            ExpressionCodegen codegen = new ExpressionCodegen(mv, bindingContext, frameMap, typeMapper,
                    jvmSignature.getReturnType(), contextDesc, kind, thisExpression, factory);
            if (kind instanceof OwnerKind.DelegateKind) {
                OwnerKind.DelegateKind dk = (OwnerKind.DelegateKind) kind;
                InstructionAdapter iv = new InstructionAdapter(mv);
                iv.load(0, JetTypeMapper.TYPE_OBJECT);
                dk.getDelegate().put(JetTypeMapper.TYPE_OBJECT, iv);
                for (int i = 0; i < argTypes.length; i++) {
                    Type argType = argTypes[i];
                    iv.load(i + 1, argType);
                }
                iv.invokeinterface(dk.getOwnerClass(), jvmSignature.getName(), jvmSignature.getDescriptor());
                iv.areturn(jvmSignature.getReturnType());
            }
            else if (!isAbstract) {
                JetElement last = null;
                for (JetElement expression : bodyExpressions) {
                    expression.accept(codegen);
                    last = expression;
                }
                generateReturn(mv, last, codegen, jvmSignature);
            }
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }

    private void generateReturn(MethodVisitor mv, JetElement bodyExpression, ExpressionCodegen codegen, Method jvmSignature) {
        if (!endsWithReturn(bodyExpression)) {
            if (jvmSignature.getReturnType() == Type.VOID_TYPE) {
                mv.visitInsn(Opcodes.RETURN);
            }
            else {
                codegen.returnTopOfStack();
            }
        }
    }

    private static boolean endsWithReturn(JetElement bodyExpression) {
        if (bodyExpression instanceof JetBlockExpression) {
            final List<JetElement> statements = ((JetBlockExpression) bodyExpression).getStatements();
            return statements.size() > 0 && statements.get(statements.size()-1) instanceof JetReturnExpression;
        }

        return bodyExpression instanceof JetReturnExpression;
    }
}
