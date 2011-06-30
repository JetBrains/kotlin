package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.*;
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
 * @author yole
 */
public class FunctionCodegen {
    private final JetDeclaration owner;
    private final ClassVisitor v;
    private final GenerationState state;

    public FunctionCodegen(JetDeclaration owner, ClassVisitor v, GenerationState state) {
        this.owner = owner;
        this.v = v;
        this.state = state;
    }

    public void gen(JetNamedFunction f, OwnerKind kind) {
        final JetTypeReference receiverTypeRef = f.getReceiverTypeRef();
        final JetType receiverType = receiverTypeRef == null ? null : state.getBindingContext().resolveTypeReference(receiverTypeRef);
        Method method = state.getTypeMapper().mapToCallableMethod(f).getDescriptor();
        final FunctionDescriptor functionDescriptor = state.getBindingContext().getFunctionDescriptor(f);
        generateMethod(f, kind, method, receiverType, functionDescriptor.getValueParameters(),
                functionDescriptor.getTypeParameters());
    }

    public void generateMethod(JetDeclarationWithBody f,
                               OwnerKind kind,
                               Method jvmSignature,
                               @Nullable JetType receiverType,
                               List<ValueParameterDescriptor> paramDescrs,
                               List<TypeParameterDescriptor> typeParameters) {
        final DeclarationDescriptor contextDesc = owner instanceof JetClassOrObject
                ? state.getBindingContext().getClassDescriptor((JetClassOrObject) owner)
                : state.getBindingContext().getNamespaceDescriptor((JetNamespace) owner);
        final JetExpression bodyExpression = f.getBodyExpression();
        final List<JetElement> bodyExpressions = bodyExpression != null ? Collections.<JetElement>singletonList(bodyExpression) : null;
        generatedMethod(bodyExpressions, kind, jvmSignature, receiverType, paramDescrs, typeParameters, contextDesc);
    }

    public void generatedMethod(List<JetElement> bodyExpressions,
                                OwnerKind kind,
                                Method jvmSignature,
                                JetType receiverType,
                                List<ValueParameterDescriptor> paramDescrs,
                                List<TypeParameterDescriptor> typeParameters,
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

            int thisIdx = -1;
            if (kind != OwnerKind.NAMESPACE) {
                frameMap.enterTemp();  // 0 slot for this
                thisIdx++;
            }

            if (receiverType != null) {
                thisIdx++;
                frameMap.enterTemp();  // Next slot for fake this
            }

            StackValue thisExpression = receiverType == null ? null : StackValue.local(thisIdx, state.getTypeMapper().mapType(receiverType));
            ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, jvmSignature.getReturnType(), contextDesc, kind, thisExpression, state);

            int firstArg = thisIdx+1;
            Type[] argTypes = jvmSignature.getArgumentTypes();
            for (int i = 0; i < paramDescrs.size(); i++) {
                ValueParameterDescriptor parameter = paramDescrs.get(i);
                frameMap.enter(parameter, argTypes[i].getSize());
            }
            for (int i = 0; i < typeParameters.size(); i++) {
                final TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
                codegen.addTypeParameter(typeParameterDescriptor,
                        StackValue.local(firstArg + paramDescrs.size() + i, JetTypeMapper.TYPE_TYPEINFO));
                frameMap.enterTemp();
            }

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
