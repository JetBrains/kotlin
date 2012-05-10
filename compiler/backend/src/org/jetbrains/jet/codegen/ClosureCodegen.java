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

/*
 * @author max
 * @author alex.tkachman
 */
package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.codegen.signature.BothSignatureWriter;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterKind;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.signature.SignatureWriter;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class ClosureCodegen extends ObjectOrClosureCodegen {

    private final BindingContext bindingContext;

    public ClosureCodegen(GenerationState state, ExpressionCodegen exprContext, CodegenContext context) {
        super(exprContext, context, state);
        bindingContext = state.getBindingContext();
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
            signatureWriter.writeAsmType(JetTypeMapper.TYPE_OBJECT, true);
            signatureWriter.writeParameterTypeEnd();
        }
        
        signatureWriter.writeParametersEnd();

        signatureWriter.writeReturnType();
        signatureWriter.writeAsmType(JetTypeMapper.TYPE_OBJECT, true);
        signatureWriter.writeReturnTypeEnd();

        return signatureWriter.makeJvmMethodSignature("invoke");
    }

    public static CallableMethod asCallableMethod(FunctionDescriptor fd) {
        JvmMethodSignature descriptor = erasedInvokeSignature(fd);
        String owner = getInternalClassName(fd);
        final CallableMethod result = new CallableMethod(owner, "", "", descriptor, INVOKEVIRTUAL);
        if (fd.getReceiverParameter().exists()) {
            result.setNeedsReceiver(fd);
        }
        result.setNeedsThis(getInternalType(fd));
        result.requestGenerateCallee(Type.getObjectType(getInternalClassName(fd)));
        return result;
    }

    public JvmMethodSignature invokeSignature(FunctionDescriptor fd) {
        return state.getInjector().getJetTypeMapper().mapSignature("invoke", fd);
    }

    public GeneratedAnonymousClassDescriptor gen(JetExpression fun) {
        final Pair<String, ClassBuilder> nameAndVisitor = state.forAnonymousSubclass(fun);

        final FunctionDescriptor funDescriptor = (FunctionDescriptor) bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, fun);

        cv = nameAndVisitor.getSecond();
        name = nameAndVisitor.getFirst();

        SignatureWriter signatureWriter = new SignatureWriter();

        final List<ValueParameterDescriptor> parameters = funDescriptor.getValueParameters();
        final String funClass = getInternalClassName(funDescriptor);
        signatureWriter.visitClassType(funClass);
        for (ValueParameterDescriptor parameter : parameters) {
            appendType(signatureWriter, parameter.getType(), '=');
        }

        appendType(signatureWriter, funDescriptor.getReturnType(), '=');
        signatureWriter.visitEnd();

        cv.defineClass(fun,
                       V1_6,
                       ACC_PUBLIC/*|ACC_SUPER*/,
                       name,
                       null,
                       funClass,
                       new String[0]
        );
        cv.visitSource(fun.getContainingFile().getName(), null);


        generateBridge(name, funDescriptor, fun, cv);
        captureThis = generateBody(funDescriptor, cv, (JetDeclarationWithBody) fun);
        ClassDescriptor thisDescriptor = context.getThisDescriptor();
        final Type enclosingType = thisDescriptor == null ? null : state.getInjector().getJetTypeMapper().mapType(thisDescriptor.getDefaultType(), MapTypeMode.VALUE);
        if (enclosingType == null)
            captureThis = null;

        final Method constructor = generateConstructor(funClass, fun);

        if (captureThis != null) {
            cv.newField(fun, ACC_FINAL, "this$0", enclosingType.getDescriptor(), null, null);
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
            else if(CodegenUtil.isNamedFun(descriptor, state.getBindingContext()) && descriptor.getContainingDeclaration() instanceof FunctionDescriptor) {
                final EnclosedValueDescriptor valueDescriptor = closure.get(descriptor);
                answer.addArg(valueDescriptor.getOuterValue());
            }
        }
        return answer;
    }

    private void generateConstInstance(PsiElement fun) {
        String classDescr = "L" + name + ";";
        cv.newField(fun, ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "$instance", classDescr, null, null);

        MethodVisitor mv = cv.newMethod(fun, ACC_PUBLIC | ACC_STATIC, "$getInstance", "()" + classDescr, null, new String[0]);
        if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
            StubCodegen.generateStubCode(mv);
        }
        else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
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
            FunctionCodegen.endVisit(mv, "$getInstance", fun);
        }
    }

    private Type generateBody(FunctionDescriptor funDescriptor, ClassBuilder cv, JetDeclarationWithBody body) {
        ClassDescriptor function = state.getInjector().getJetTypeMapper().getClosureAnnotator().classDescriptorForFunctionDescriptor(funDescriptor, name);

        final CodegenContext.ClosureContext closureContext = context.intoClosure(funDescriptor, function, name, this, state.getInjector().getJetTypeMapper());
        FunctionCodegen fc = new FunctionCodegen(closureContext, cv, state);
        JvmMethodSignature jvmMethodSignature = invokeSignature(funDescriptor);
        fc.generateMethod(body, jvmMethodSignature, false, null, funDescriptor);
        return closureContext.outerWasUsed;
    }

    private void generateBridge(String className, FunctionDescriptor funDescriptor, JetExpression fun, ClassBuilder cv) {
        final JvmMethodSignature bridge = erasedInvokeSignature(funDescriptor);
        final Method delegate = invokeSignature(funDescriptor).getAsmMethod();

        if(bridge.getAsmMethod().getDescriptor().equals(delegate.getDescriptor()))
            return;

        final MethodVisitor mv = cv.newMethod(fun, ACC_PUBLIC, "invoke", bridge.getAsmMethod().getDescriptor(), null, new String[0]);
        if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
            StubCodegen.generateStubCode(mv);
        }
        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();

            InstructionAdapter iv = new InstructionAdapter(mv);

            iv.load(0, Type.getObjectType(className));

            final ReceiverDescriptor receiver = funDescriptor.getReceiverParameter();
            int count = 1;
            if (receiver.exists()) {
                StackValue.local(count, JetTypeMapper.TYPE_OBJECT).put(JetTypeMapper.TYPE_OBJECT, iv);
                StackValue.onStack(JetTypeMapper.TYPE_OBJECT).upcast(state.getInjector().getJetTypeMapper().mapType(receiver.getType(), MapTypeMode.VALUE), iv);
                count++;
            }

            final List<ValueParameterDescriptor> params = funDescriptor.getValueParameters();
            for (ValueParameterDescriptor param : params) {
                StackValue.local(count, JetTypeMapper.TYPE_OBJECT).put(JetTypeMapper.TYPE_OBJECT, iv);
                StackValue.onStack(JetTypeMapper.TYPE_OBJECT).upcast(state.getInjector().getJetTypeMapper().mapType(param.getType(), MapTypeMode.VALUE), iv);
                count++;
            }

            iv.invokespecial(className, "invoke", delegate.getDescriptor());
            StackValue.onStack(delegate.getReturnType()).put(JetTypeMapper.TYPE_OBJECT, iv);

            iv.areturn(JetTypeMapper.TYPE_OBJECT);

            FunctionCodegen.endVisit(mv, "bridge", fun);
        }
    }

    private Method generateConstructor(String funClass, PsiElement fun) {
        int argCount = captureThis != null ? 1 : 0;
        argCount += (captureReceiver != null ? 1 : 0);

        ArrayList<DeclarationDescriptor> variableDescriptors = new ArrayList<DeclarationDescriptor>();
        
        for (DeclarationDescriptor descriptor : closure.keySet()) {
            if(descriptor instanceof VariableDescriptor && !(descriptor instanceof PropertyDescriptor)) {
                argCount++;
                variableDescriptors.add(descriptor);
            }
            else if(CodegenUtil.isNamedFun(descriptor, state.getBindingContext()) && descriptor.getContainingDeclaration() instanceof FunctionDescriptor) {
                argCount++;
                variableDescriptors.add(descriptor);
            }
            else if(descriptor instanceof FunctionDescriptor) {
                assert captureReceiver != null;
            }
        }

        Type[] argTypes = new Type[argCount];

        int i = 0;
        if (captureThis != null) {
            argTypes[i++] = state.getInjector().getJetTypeMapper().mapType(context.getThisDescriptor().getDefaultType(), MapTypeMode.VALUE);
        }

        if (captureReceiver != null) {
            argTypes[i++] = captureReceiver;
        }

        for (DeclarationDescriptor descriptor : closure.keySet()) {
            if(descriptor instanceof VariableDescriptor && !(descriptor instanceof PropertyDescriptor)) {
                final Type sharedVarType = state.getInjector().getJetTypeMapper().getSharedVarType(descriptor);
                final Type type;
                if (sharedVarType != null) {
                    type = sharedVarType;
                }
                else {
                    type = state.getInjector().getJetTypeMapper().mapType(((VariableDescriptor) descriptor).getType(), MapTypeMode.VALUE);
                }
                argTypes[i++] = type;
            }
            else if(CodegenUtil.isNamedFun(descriptor, state.getBindingContext()) && descriptor.getContainingDeclaration() instanceof FunctionDescriptor) {
                final Type type = Type.getObjectType(state.getInjector().getJetTypeMapper().getClosureAnnotator().classNameForAnonymousClass((JetElement) BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor)));
                argTypes[i++] = type;
            }
        }

        final Method constructor = new Method("<init>", Type.VOID_TYPE, argTypes);
        final MethodVisitor mv = cv.newMethod(fun, ACC_PUBLIC, "<init>", constructor.getDescriptor(), null, new String[0]);
        if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
            StubCodegen.generateStubCode(mv);
        }
        else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();
            InstructionAdapter iv = new InstructionAdapter(mv);

            iv.load(0, Type.getObjectType(funClass));
            iv.invokespecial(funClass, "<init>", "()V");

            i = 1;
            for (Type type : argTypes) {
                StackValue.local(0, JetTypeMapper.TYPE_OBJECT).put(JetTypeMapper.TYPE_OBJECT, iv);
                StackValue.local(i, type).put(type, iv);
                final String fieldName;
                if (captureThis != null && i == 1) {
                    fieldName = "this$0";
                }
                else {
                    if (captureReceiver != null && (captureThis != null && i == 2 || captureThis == null && i == 1)) {
                        fieldName = "receiver$0";
                    }
                    else {
                        DeclarationDescriptor removed = variableDescriptors.remove(0);
                        fieldName = "$" + removed.getName();
                    }
                }
                i += type.getSize();

                StackValue.field(type, name, fieldName, false).store(iv);
            }

            iv.visitInsn(RETURN);

            FunctionCodegen.endVisit(iv, "constructor", fun);
        }
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

    public static ClassDescriptor getInternalType(FunctionDescriptor descriptor) {
        final int paramCount = descriptor.getValueParameters().size();
        if (descriptor.getReceiverParameter().exists()) {
            return JetStandardClasses.getReceiverFunction(paramCount);
        }
        else {
            return JetStandardClasses.getFunction(paramCount);
        }
    }

    private void appendType(SignatureWriter signatureWriter, JetType type, char variance) {
        signatureWriter.visitTypeArgument(variance);

        final JetTypeMapper typeMapper = state.getInjector().getJetTypeMapper();
        final Type rawRetType = typeMapper.mapType(type, MapTypeMode.TYPE_PARAMETER);
        signatureWriter.visitClassType(rawRetType.getInternalName());
        signatureWriter.visitEnd();
    }
}
