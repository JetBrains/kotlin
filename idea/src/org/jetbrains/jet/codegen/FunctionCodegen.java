package org.jetbrains.jet.codegen;

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
        Method method = state.getTypeMapper().mapToCallableMethod(f).getSignature();
        final FunctionDescriptor functionDescriptor = state.getBindingContext().get(BindingContext.FUNCTION, f);
        generateMethod(f, method, functionDescriptor);
    }

    public void generateMethod(JetDeclarationWithBody f, Method jvmMethod, FunctionDescriptor functionDescriptor) {
        ClassContext funContext = owner.intoFunction(functionDescriptor);

        final JetExpression bodyExpression = f.getBodyExpression();
        generatedMethod(bodyExpression, jvmMethod, funContext, functionDescriptor.getValueParameters(), functionDescriptor.getTypeParameters());
    }

    private void generatedMethod(JetExpression bodyExpressions,
                                 Method jvmSignature,
                                 ClassContext context,
                                 List<ValueParameterDescriptor> paramDescrs,
                                 List<TypeParameterDescriptor> typeParameters)
    {
        int flags = Opcodes.ACC_PUBLIC; // TODO.

        OwnerKind kind = context.getContextKind();

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
            else if (!isAbstract) {
                codegen.returnExpression(bodyExpressions);
            }
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
    }

}
