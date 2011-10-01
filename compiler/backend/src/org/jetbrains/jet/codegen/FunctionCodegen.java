package org.jetbrains.jet.codegen;

import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;

import java.util.List;
import java.util.Set;

/**
 * @author max
 * @author yole
 */
public class FunctionCodegen {
    private final ClassContext owner;
    private final ClassVisitor v;
    private final GenerationState state;

    public FunctionCodegen(ClassContext owner, ClassVisitor v, GenerationState state) {
        this.owner = owner;
        this.v = v;
        this.state = state;
    }

    public void gen(JetNamedFunction f) {
        Method method = state.getTypeMapper().mapToCallableMethod(f, owner.getContextKind()).getSignature();
        final FunctionDescriptor functionDescriptor = state.getBindingContext().get(BindingContext.FUNCTION, f);
        generateMethod(f, method, functionDescriptor);
    }

    public void generateMethod(JetDeclarationWithBody f, Method jvmMethod, FunctionDescriptor functionDescriptor) {
        ClassContext funContext = owner.intoFunction(functionDescriptor);

        final JetExpression bodyExpression = f.getBodyExpression();
        generatedMethod(bodyExpression, jvmMethod, funContext, functionDescriptor);
    }

    private void generatedMethod(JetExpression bodyExpressions,
                                 Method jvmSignature,
                                 ClassContext context,
                                 FunctionDescriptor functionDescriptor)
    {
        List<ValueParameterDescriptor> paramDescrs = functionDescriptor.getValueParameters();
        List<TypeParameterDescriptor> typeParameters = functionDescriptor.getTypeParameters();

        int flags = Opcodes.ACC_PUBLIC; // TODO.

        OwnerKind kind = context.getContextKind();

        if(kind == OwnerKind.TRAIT_IMPL && bodyExpressions == null)
            return;

        boolean isStatic = kind == OwnerKind.NAMESPACE || kind == OwnerKind.TRAIT_IMPL;
        if (isStatic) flags |= Opcodes.ACC_STATIC;

        boolean isAbstract = !isStatic && (bodyExpressions == null || CodegenUtil.isInterface(functionDescriptor.getContainingDeclaration()));
        if (isAbstract) flags |= Opcodes.ACC_ABSTRACT;

        final MethodVisitor mv = v.visitMethod(flags, jvmSignature.getName(), jvmSignature.getDescriptor(), null, null);
        if (!isAbstract) {
            mv.visitCode();
            FrameMap frameMap = context.prepareFrame();

            ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, jvmSignature.getReturnType(), context, state);

            Type[] argTypes = jvmSignature.getArgumentTypes();
            for (int i = 0; i < paramDescrs.size(); i++) {
                ValueParameterDescriptor parameter = paramDescrs.get(i);
                frameMap.enter(parameter, argTypes[i].getSize());
            }

            for (final TypeParameterDescriptor typeParameterDescriptor : typeParameters) {
                int slot = frameMap.enterTemp();
                codegen.addTypeParameter(typeParameterDescriptor, StackValue.local(slot, JetTypeMapper.TYPE_TYPEINFO));
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
            else {
                codegen.returnExpression(bodyExpressions);
            }
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            generateBridgeIfNeeded(owner, state, v, jvmSignature, functionDescriptor, kind);
        }
    }

    static void generateBridgeIfNeeded(ClassContext owner, GenerationState state, ClassVisitor v, Method jvmSignature, FunctionDescriptor functionDescriptor, OwnerKind kind) {
        Set<? extends FunctionDescriptor> overriddenFunctions = functionDescriptor.getOverriddenDescriptors();
        if(kind != OwnerKind.TRAIT_IMPL) {
            for (FunctionDescriptor overriddenFunction : overriddenFunctions) {
                // TODO should we check params here as well?
                checkOverride(owner, state, v, jvmSignature, functionDescriptor, overriddenFunction);
            }
            checkOverride(owner, state, v, jvmSignature, functionDescriptor, functionDescriptor.getOriginal());
        }
    }

    private static void checkOverride(ClassContext owner, GenerationState state, ClassVisitor v, Method jvmSignature, FunctionDescriptor functionDescriptor, FunctionDescriptor overriddenFunction) {
        Type type1 = state.getTypeMapper().mapType(overriddenFunction.getOriginal().getReturnType());
        Type type2 = state.getTypeMapper().mapType(functionDescriptor.getReturnType());
        if(!type1.equals(type2)) {
            Method overriden = state.getTypeMapper().mapSignature(overriddenFunction.getName(), overriddenFunction.getOriginal());
            int flags = Opcodes.ACC_PUBLIC; // TODO.

            final MethodVisitor mv = v.visitMethod(flags, jvmSignature.getName(), overriden.getDescriptor(), null, null);
            mv.visitCode();

            Type[] argTypes = jvmSignature.getArgumentTypes();
            InstructionAdapter iv = new InstructionAdapter(mv);
            iv.load(0, JetTypeMapper.TYPE_OBJECT);
            for (int i = 0, reg = 1; i < argTypes.length; i++) {
                Type argType = argTypes[i];
                iv.load(reg, argType);
                if(argType.getSort() == Type.OBJECT) {
                    iv.checkcast(argType);
                }
                //noinspection AssignmentToForLoopParameter
                reg += argType.getSize();
            }

            iv.invokevirtual(state.getTypeMapper().jvmName((ClassDescriptor) owner.getContextDescriptor(), OwnerKind.IMPLEMENTATION), jvmSignature.getName(), jvmSignature.getDescriptor());
            if(JetTypeMapper.isPrimitive(jvmSignature.getReturnType()) && !JetTypeMapper.isPrimitive(overriden.getReturnType()))
                StackValue.valueOf(iv, jvmSignature.getReturnType());
            if(jvmSignature.getReturnType() == Type.VOID_TYPE)
                iv.aconst(null);
            iv.areturn(overriden.getReturnType());
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }

}
