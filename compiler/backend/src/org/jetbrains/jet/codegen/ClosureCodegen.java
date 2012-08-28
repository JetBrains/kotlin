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
import org.jetbrains.asm4.Label;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.asm4.commons.Method;
import org.jetbrains.asm4.signature.SignatureWriter;
import org.jetbrains.jet.codegen.context.*;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.context.CodegenBinding.*;

public class ClosureCodegen {

    private final BindingContext bindingContext;
    private final JetTypeMapper typeMapper;

    Method constructor;
    private final GenerationState state;
    private final MutableClosure closure;
    JvmClassName name;


    public ClosureCodegen(GenerationState state, MutableClosure closure) {
        this.state = state;
        this.closure = closure;
        bindingContext = state.getBindingContext();
        typeMapper = state.getInjector().getJetTypeMapper();
    }

    public ClosureCodegen gen(JetExpression fun, CodegenContext context, ExpressionCodegen expressionCodegen) {
        final SimpleFunctionDescriptor descriptor = bindingContext.get(BindingContext.FUNCTION, fun);
        assert descriptor != null;

        final Pair<JvmClassName, ClassBuilder> nameAndVisitor = state.forAnonymousSubclass(fun);

        final FunctionDescriptor funDescriptor = bindingContext.get(BindingContext.FUNCTION, fun);

        ClassBuilder cv = nameAndVisitor.getSecond();
        name = nameAndVisitor.getFirst();

        SignatureWriter signatureWriter = new SignatureWriter();

        assert funDescriptor != null;
        final List<ValueParameterDescriptor> parameters = funDescriptor.getValueParameters();
        final JvmClassName funClass = CodegenUtil.getInternalClassName(funDescriptor);
        signatureWriter.visitClassType(funClass.getInternalName());
        for (ValueParameterDescriptor parameter : parameters) {
            appendType(signatureWriter, parameter.getType(), '=');
        }

        appendType(signatureWriter, funDescriptor.getReturnType(), '=');
        signatureWriter.visitEnd();

        cv.defineClass(fun,
                       V1_6,
                       ACC_PUBLIC/*|ACC_SUPER*/,
                       name.getInternalName(),
                       null,
                       funClass.getInternalName(),
                       new String[0]
        );
        cv.visitSource(fun.getContainingFile().getName(), null);


        generateBridge(name.getInternalName(), funDescriptor, fun, cv);
        generateBody(funDescriptor, cv, (JetDeclarationWithBody) fun, context, expressionCodegen);

        constructor = generateConstructor(funClass, fun, cv, closure);

        if (CodegenUtil.isConst(closure)) {
            generateConstInstance(fun, cv);
        }

        CodegenUtil.generateClosureFields(closure, cv, state.getInjector().getJetTypeMapper());

        cv.done();

        return this;
    }

    private void generateConstInstance(PsiElement fun, ClassBuilder cv) {
        String classDescr = name.getDescriptor();
        cv.newField(fun, ACC_PRIVATE | ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC, "$instance", classDescr, null, null);

        MethodVisitor mv =
                cv.newMethod(fun, ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, "$getInstance", "()" + classDescr, null, new String[0]);
        if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
            StubCodegen.generateStubCode(mv);
        }
        else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();
            mv.visitFieldInsn(GETSTATIC, name.getInternalName(), "$instance", classDescr);
            mv.visitInsn(DUP);
            Label ret = new Label();
            mv.visitJumpInsn(IFNONNULL, ret);

            mv.visitInsn(POP);
            mv.visitTypeInsn(NEW, name.getInternalName());
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, name.getInternalName(), "<init>", "()V");
            mv.visitInsn(DUP);
            mv.visitFieldInsn(PUTSTATIC, name.getInternalName(), "$instance", classDescr);

            mv.visitLabel(ret);
            mv.visitInsn(ARETURN);
            FunctionCodegen.endVisit(mv, "$getInstance", fun);
        }
    }

    private ClassDescriptor generateBody(
            FunctionDescriptor funDescriptor,
            ClassBuilder cv,
            JetDeclarationWithBody body,
            CodegenContext context,
            ExpressionCodegen expressionCodegen
    ) {

        final CodegenContext closureContext = context.intoClosure(funDescriptor, expressionCodegen);
        FunctionCodegen fc = new FunctionCodegen(closureContext, cv, state);
        JvmMethodSignature jvmMethodSignature = typeMapper.invokeSignature(funDescriptor);
        fc.generateMethod(body, jvmMethodSignature, false, null, funDescriptor);
        return closureContext.closure.getCaptureThis();
    }

    private void generateBridge(
            String className,
            FunctionDescriptor funDescriptor,
            JetExpression fun,
            ClassBuilder cv
    ) {
        final JvmMethodSignature bridge = CodegenUtil.erasedInvokeSignature(funDescriptor);
        final Method delegate = typeMapper.invokeSignature(funDescriptor).getAsmMethod();

        if (bridge.getAsmMethod().getDescriptor().equals(delegate.getDescriptor())) {
            return;
        }

        final MethodVisitor mv =
                cv.newMethod(fun, ACC_PUBLIC | ACC_BRIDGE | ACC_VOLATILE, "invoke", bridge.getAsmMethod().getDescriptor(), null,
                             new String[0]);
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
                StackValue.local(count, JetTypeMapper.OBJECT_TYPE).put(JetTypeMapper.OBJECT_TYPE, iv);
                StackValue.onStack(JetTypeMapper.OBJECT_TYPE)
                        .upcast(typeMapper.mapType(receiver.getType(), MapTypeMode.VALUE), iv);
                count++;
            }

            final List<ValueParameterDescriptor> params = funDescriptor.getValueParameters();
            for (ValueParameterDescriptor param : params) {
                StackValue.local(count, JetTypeMapper.OBJECT_TYPE).put(JetTypeMapper.OBJECT_TYPE, iv);
                StackValue.onStack(JetTypeMapper.OBJECT_TYPE)
                        .upcast(typeMapper.mapType(param.getType(), MapTypeMode.VALUE), iv);
                count++;
            }

            iv.invokevirtual(className, "invoke", delegate.getDescriptor());
            StackValue.onStack(delegate.getReturnType()).put(JetTypeMapper.OBJECT_TYPE, iv);

            iv.areturn(JetTypeMapper.OBJECT_TYPE);

            FunctionCodegen.endVisit(mv, "bridge", fun);
        }
    }

    private Method generateConstructor(
            JvmClassName funClass,
            JetExpression fun,
            ClassBuilder cv,
            CalculatedClosure closure
    ) {
        final ArrayList<Pair<String, Type>> args = new ArrayList<Pair<String, Type>>();
        calculateConstructorParameters(args, state, closure);

        final Type[] argTypes = nameAnTypeListToTypeArray(args);

        final Method constructor = new Method("<init>", Type.VOID_TYPE, argTypes);
        final MethodVisitor mv = cv.newMethod(fun, ACC_PUBLIC, "<init>", constructor.getDescriptor(), null, new String[0]);
        if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
            StubCodegen.generateStubCode(mv);
        }
        else if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            mv.visitCode();
            InstructionAdapter iv = new InstructionAdapter(mv);

            iv.load(0, funClass.getAsmType());
            iv.invokespecial(funClass.getInternalName(), "<init>", "()V");

            int k = 1;
            for (int i = 0; i != argTypes.length; ++i) {
                StackValue.local(0, JetTypeMapper.OBJECT_TYPE).put(JetTypeMapper.OBJECT_TYPE, iv);
                final Pair<String, Type> nameAndType = args.get(i);
                final Type type = nameAndType.second;
                StackValue.local(k, type).put(type, iv);
                k += type.getSize();
                StackValue.field(type, name, nameAndType.first, false).store(type, iv);
            }

            iv.visitInsn(RETURN);

            FunctionCodegen.endVisit(iv, "constructor", fun);
        }
        return constructor;
    }

    private void calculateConstructorParameters(
            List<Pair<String, Type>> args,
            GenerationState state,
            CalculatedClosure closure
    ) {
        final ClassDescriptor captureThis = closure.getCaptureThis();
        if (captureThis != null) {
            final Type type = typeMapper.mapType(captureThis.getDefaultType(), MapTypeMode.VALUE);
            args.add(new Pair<String, Type>(CodegenUtil.THIS$0, type));
        }
        final ClassifierDescriptor captureReceiver = closure.getCaptureReceiver();
        if (captureReceiver != null) {
            args.add(new Pair<String, Type>(CodegenUtil.RECEIVER$0,
                                            typeMapper.mapType(captureReceiver.getDefaultType(), MapTypeMode.VALUE)));
        }

        for (DeclarationDescriptor descriptor : closure.getCaptureVariables().keySet()) {
            if (descriptor instanceof VariableDescriptor && !(descriptor instanceof PropertyDescriptor)) {
                final Type sharedVarType = typeMapper.getSharedVarType(descriptor);

                final Type type = sharedVarType != null
                                  ? sharedVarType
                                  : typeMapper.mapType(((VariableDescriptor) descriptor).getType(), MapTypeMode.VALUE);
                args.add(new Pair<String, Type>("$" + descriptor.getName().getName(), type));
            }
            else if (CodegenUtil.isNamedFun(descriptor, state.getBindingContext()) &&
                     descriptor.getContainingDeclaration() instanceof FunctionDescriptor) {
                final Type type =
                        classNameForAnonymousClass(bindingContext,
                                                   (JetElement) BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor))
                        .getAsmType();

                args.add(new Pair<String, Type>("$" + descriptor.getName().getName(), type));
            }
            else if (descriptor instanceof FunctionDescriptor) {
                assert captureReceiver != null;
            }
        }
    }

    private static Type[] nameAnTypeListToTypeArray(List<Pair<String, Type>> args) {
        final Type[] argTypes = new Type[args.size()];
        for (int i = 0; i != argTypes.length; ++i) {
            argTypes[i] = args.get(i).second;
        }
        return argTypes;
    }

    private void appendType(SignatureWriter signatureWriter, JetType type, char variance) {
        signatureWriter.visitTypeArgument(variance);

        final Type rawRetType = typeMapper.mapType(type, MapTypeMode.TYPE_PARAMETER);
        signatureWriter.visitClassType(rawRetType.getInternalName());
        signatureWriter.visitEnd();
    }
}
