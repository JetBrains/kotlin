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

package org.jetbrains.jet.codegen;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.signature.kotlin.JetMethodAnnotationWriter;
import org.jetbrains.jet.codegen.signature.kotlin.JetValueParameterAnnotationWriter;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;

import java.util.List;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

/**
 * @author max
 * @author yole
 * @author alex.tkachman
 */
public class FunctionCodegen {
    private final CodegenContext owner;
    private final ClassBuilder v;
    private final GenerationState state;
    private final JetTypeMapper typeMapper;

    public FunctionCodegen(CodegenContext owner, ClassBuilder v, GenerationState state) {
        this.owner = owner;
        this.v = v;
        this.state = state;
        typeMapper = state.getTypeMapper();
    }

    public void gen(JetNamedFunction f) {
        final SimpleFunctionDescriptor functionDescriptor = state.getBindingContext().get(BindingContext.FUNCTION, f);
        assert functionDescriptor != null;
        JvmMethodSignature method = typeMapper.mapToCallableMethod(functionDescriptor, false, owner.getContextKind()).getSignature();
        generateMethod(f, method, true, null, functionDescriptor);
    }

    public void generateMethod(JetDeclarationWithBody f,
            JvmMethodSignature jvmMethod, boolean needJetAnnotations,
            @Nullable String propertyTypeSignature, FunctionDescriptor functionDescriptor) {

        CodegenContext.MethodContext funContext = owner.intoFunction(functionDescriptor);

        final JetExpression bodyExpression = f.getBodyExpression();
        generatedMethod(bodyExpression, jvmMethod, needJetAnnotations, propertyTypeSignature, funContext, functionDescriptor, f);
    }

    private void generatedMethod(JetExpression bodyExpressions,
            JvmMethodSignature jvmSignature,
            boolean needJetAnnotations, @Nullable String propertyTypeSignature,
            CodegenContext.MethodContext context,
            FunctionDescriptor functionDescriptor,
            JetDeclarationWithBody fun
    )
    {
        List<ValueParameterDescriptor> paramDescrs = functionDescriptor.getValueParameters();
        List<TypeParameterDescriptor> typeParameters = (functionDescriptor instanceof PropertyAccessorDescriptor ? ((PropertyAccessorDescriptor)functionDescriptor).getCorrespondingProperty(): functionDescriptor).getTypeParameters();

        int flags = ACC_PUBLIC; // TODO.
        
        if (!functionDescriptor.getValueParameters().isEmpty()
                && functionDescriptor.getValueParameters().get(functionDescriptor.getValueParameters().size() - 1)
                        .getVarargElementType() != null)
        {
            flags |= ACC_VARARGS;
        }
        
        if (functionDescriptor.getModality() == Modality.FINAL) {
            flags |= ACC_FINAL;
        }

        OwnerKind kind = context.getContextKind();
        
        if (kind == OwnerKind.TRAIT_IMPL) {
            needJetAnnotations = false;
        }

        ReceiverDescriptor expectedThisObject = functionDescriptor.getExpectedThisObject();
        ReceiverDescriptor receiverParameter = functionDescriptor.getReceiverParameter();

        if (kind != OwnerKind.TRAIT_IMPL || bodyExpressions != null) {
            boolean isStatic = kind == OwnerKind.NAMESPACE;
            if (isStatic || kind == OwnerKind.TRAIT_IMPL)
                flags |= ACC_STATIC;

            boolean isAbstract = (
                        functionDescriptor.getModality() == Modality.ABSTRACT
                        || CodegenUtil.isInterface(functionDescriptor.getContainingDeclaration())
                    ) && !isStatic && kind != OwnerKind.TRAIT_IMPL;
            if (isAbstract) flags |= ACC_ABSTRACT;
            
            final MethodVisitor mv = v.newMethod(fun, flags, jvmSignature.getAsmMethod().getName(), jvmSignature.getAsmMethod().getDescriptor(), jvmSignature.getGenericsSignature(), null);
            AnnotationCodegen.forMethod(mv).genAnnotations(functionDescriptor, state.getTypeMapper());
            if(v.generateCode() != ClassBuilder.Mode.SIGNATURES) {
                int start = 0;
                if (needJetAnnotations) {
                    if (functionDescriptor instanceof PropertyAccessorDescriptor) {
                        PropertyCodegen.generateJetPropertyAnnotation(mv, propertyTypeSignature, jvmSignature.getKotlinTypeParameter());
                    } else if (functionDescriptor instanceof SimpleFunctionDescriptor) {
                        if (propertyTypeSignature != null) {
                            throw new IllegalStateException();
                        }
                        JetMethodAnnotationWriter aw = JetMethodAnnotationWriter.visitAnnotation(mv);
                        aw.writeKind(JvmStdlibNames.JET_METHOD_KIND_REGULAR);
                        aw.writeNullableReturnType(functionDescriptor.getReturnType().isNullable());
                        aw.writeTypeParameters(jvmSignature.getKotlinTypeParameter());
                        aw.writeReturnType(jvmSignature.getKotlinReturnType());
                        aw.visitEnd();
                    } else {
                        throw new IllegalStateException();
                    }

                    if(receiverParameter.exists()) {
                        JetValueParameterAnnotationWriter av = JetValueParameterAnnotationWriter.visitParameterAnnotation(mv, start++);
                        av.writeName("this$receiver");
                        av.writeNullable(receiverParameter.getType().isNullable());
                        av.writeReceiver();
                        if (jvmSignature.getKotlinParameterTypes() != null && jvmSignature.getKotlinParameterTypes().get(0) != null) {
                            av.writeType(jvmSignature.getKotlinParameterTypes().get(0).getKotlinSignature());
                        }
                        av.visitEnd();
                    }
                    for(int i = 0; i != paramDescrs.size(); ++i) {
                        JetValueParameterAnnotationWriter av = JetValueParameterAnnotationWriter.visitParameterAnnotation(mv, i + start);
                        ValueParameterDescriptor parameterDescriptor = paramDescrs.get(i);
                        av.writeName(parameterDescriptor.getName());
                        av.writeHasDefaultValue(parameterDescriptor.hasDefaultValue());
                        av.writeNullable(parameterDescriptor.getType().isNullable());
                        if (jvmSignature.getKotlinParameterTypes() != null && jvmSignature.getKotlinParameterTypes().get(i) != null) {
                            av.writeType(jvmSignature.getKotlinParameterTypes().get(i + start).getKotlinSignature());
                        }
                        av.visitEnd();
                    }
                }
            }

            if (!isAbstract && v.generateCode() == ClassBuilder.Mode.STUBS) {
                StubCodegen.generateStubCode(mv);
            }


            if (!isAbstract && v.generateCode() == ClassBuilder.Mode.FULL) {
                mv.visitCode();
                
                Label methodBegin = new Label();
                mv.visitLabel(methodBegin);
                
                FrameMap frameMap = context.prepareFrame(typeMapper);

                ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, jvmSignature.getAsmMethod().getReturnType(), context, state);

                Type[] argTypes = jvmSignature.getAsmMethod().getArgumentTypes();
                int add = 0;

                if(kind == OwnerKind.TRAIT_IMPL)
                    add++;

                if(receiverParameter.exists())
                    add++;

                for (int i = 0; i < paramDescrs.size(); i++) {
                    ValueParameterDescriptor parameter = paramDescrs.get(i);
                    frameMap.enter(parameter, argTypes[i+add].getSize());
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
                    iv.invokeinterface(dk.getOwnerClass(), jvmSignature.getAsmMethod().getName(), jvmSignature.getAsmMethod().getDescriptor());
                    iv.areturn(jvmSignature.getAsmMethod().getReturnType());
                }
                else {
                    for (ValueParameterDescriptor parameter : paramDescrs) {
                        Type sharedVarType = typeMapper.getSharedVarType(parameter);
                        if (sharedVarType != null) {
                            Type localVarType = typeMapper.mapType(parameter.getType());
                            int index = frameMap.getIndex(parameter);
                            mv.visitTypeInsn(NEW, sharedVarType.getInternalName());
                            mv.visitInsn(DUP);
                            mv.visitInsn(DUP);
                            mv.visitMethodInsn(INVOKESPECIAL, sharedVarType.getInternalName(), "<init>", "()V");
                            mv.visitVarInsn(localVarType.getOpcode(ILOAD), index);
                            mv.visitFieldInsn(PUTFIELD, sharedVarType.getInternalName(), "ref", StackValue.refType(localVarType).getDescriptor());
                            mv.visitVarInsn(sharedVarType.getOpcode(ISTORE), index);
                        }
                    }

                    codegen.returnExpression(bodyExpressions);
                }
                
                Label methodEnd = new Label();
                mv.visitLabel(methodEnd);

                int k = 0;

                if(expectedThisObject.exists()) {
                    Type type = typeMapper.mapType(expectedThisObject.getType());
                    // TODO: specify signature
                    mv.visitLocalVariable("this", type.getDescriptor(), null, methodBegin, methodEnd, k++);
                }

                if(receiverParameter.exists()) {
                    Type type = typeMapper.mapType(receiverParameter.getType());
                    // TODO: specify signature
                    mv.visitLocalVariable("this$receiver", type.getDescriptor(), null, methodBegin, methodEnd, k);
                    k += type.getSize();
                }

                for (ValueParameterDescriptor parameter : paramDescrs) {
                    Type type = typeMapper.mapType(parameter.getType());
                    // TODO: specify signature
                    mv.visitLocalVariable(parameter.getName(), type.getDescriptor(), null, methodBegin, methodEnd, k);
                    k += type.getSize();
                }

                endVisit(mv, null, fun);
                mv.visitEnd();

                generateBridgeIfNeeded(owner, state, v, jvmSignature.getAsmMethod(), functionDescriptor, kind);
            }
        }

        generateDefaultIfNeeded(context, state, v, jvmSignature.getAsmMethod(), functionDescriptor, kind);
    }

    public static void endVisit(MethodVisitor mv, String description, PsiElement method) {
        try {
            mv.visitMaxs(-1, -1);
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable t) {
            throw new CompilationException(
                "wrong code generated" + (description != null ? " for " + description : "") + t.getClass().getName() + " " + t.getMessage(),
                t, method);
        }
        mv.visitEnd();
    }

    static void generateBridgeIfNeeded(CodegenContext owner, GenerationState state, ClassBuilder v, Method jvmSignature, FunctionDescriptor functionDescriptor, OwnerKind kind) {
        Set<? extends FunctionDescriptor> overriddenFunctions = functionDescriptor.getOverriddenDescriptors();
        if(kind != OwnerKind.TRAIT_IMPL) {
            for (FunctionDescriptor overriddenFunction : overriddenFunctions) {
                // TODO should we check params here as well?
                checkOverride(owner, state, v, jvmSignature, functionDescriptor, overriddenFunction.getOriginal());
            }
            checkOverride(owner, state, v, jvmSignature, functionDescriptor, functionDescriptor.getOriginal());
        }
    }

    static void generateDefaultIfNeeded(CodegenContext.MethodContext owner, GenerationState state, ClassBuilder v, Method jvmSignature, @Nullable FunctionDescriptor functionDescriptor, OwnerKind kind) {
        DeclarationDescriptor contextClass = owner.getContextDescriptor().getContainingDeclaration();

        if(kind != OwnerKind.TRAIT_IMPL) {
            // we don't generate defaults for traits but do for traitImpl
            if(contextClass instanceof ClassDescriptor) {
                PsiElement psiElement = state.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, contextClass);
                if(psiElement instanceof JetClass) {
                    JetClass element = (JetClass) psiElement;
                    if(element.isTrait())
                        return;
                }
            }
        }

        boolean needed = false;
        if(functionDescriptor != null) {
            for (ValueParameterDescriptor parameterDescriptor : functionDescriptor.getValueParameters()) {
                if(parameterDescriptor.hasDefaultValue()) {
                    needed = true;
                    break;
                }
            }
        }

        if(needed) {
            ReceiverDescriptor receiverParameter = functionDescriptor.getReceiverParameter();
            boolean hasReceiver = receiverParameter.exists();
            boolean isStatic = kind == OwnerKind.NAMESPACE;

            if(kind == OwnerKind.TRAIT_IMPL) {
                String correctedDescr = "(" + jvmSignature.getDescriptor().substring(jvmSignature.getDescriptor().indexOf(";") + 1);
                jvmSignature = new Method(jvmSignature.getName(), correctedDescr);
            }

            int flags = ACC_PUBLIC | ACC_SYNTHETIC; // TODO.

            String ownerInternalName = contextClass instanceof NamespaceDescriptor ?
                                       NamespaceCodegen.getJVMClassName(DescriptorUtils.getFQName(contextClass).toSafe(), true) :
                                       state.getTypeMapper().mapType(((ClassDescriptor) contextClass).getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName();

            String descriptor = jvmSignature.getDescriptor().replace(")","I)");
            boolean isConstructor = "<init>".equals(jvmSignature.getName());
            if(!isStatic && !isConstructor)
                descriptor = descriptor.replace("(","(L" + ownerInternalName + ";");
            final MethodVisitor mv = v.newMethod(null, flags | (isConstructor ? 0 : ACC_STATIC), isConstructor ? "<init>" : jvmSignature.getName() + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX, descriptor, null, null);
            InstructionAdapter iv = new InstructionAdapter(mv);
            if (v.generateCode() == ClassBuilder.Mode.STUBS) {
                StubCodegen.generateStubCode(mv);
            }
            else if (v.generateCode() == ClassBuilder.Mode.FULL) {
                mv.visitCode();

                FrameMap frameMap = owner.prepareFrame(state.getTypeMapper());

                ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, jvmSignature.getReturnType(), owner, state);

                int var = 0;
                if(!isStatic) {
                    var++;
                }

                Type receiverType = receiverParameter.exists() ? state.getTypeMapper().mapType(receiverParameter.getType()) : Type.DOUBLE_TYPE;
                if(hasReceiver) {
                    var += receiverType.getSize();
                }

                Type[] argTypes = jvmSignature.getArgumentTypes();
                List<ValueParameterDescriptor> paramDescrs = functionDescriptor.getValueParameters();
                for (int i = 0; i < paramDescrs.size(); i++) {
                    int size = argTypes[i + (hasReceiver ? 1 : 0)].getSize();
                    var += size;
                }

                int maskIndex = var;

                var = 0;
                if(!isStatic) {
                    mv.visitVarInsn(ALOAD, var++);
                }

                if(hasReceiver) {
                    iv.load(var, receiverType);
                    var += receiverType.getSize();
                }

                int extra = hasReceiver ? 1 : 0;

                Type[] argumentTypes = jvmSignature.getArgumentTypes();
                for (int index = 0; index < paramDescrs.size(); index++) {
                    ValueParameterDescriptor parameterDescriptor = paramDescrs.get(index);

                    Type t = argumentTypes[extra + index];
                    Label endArg = null;
                    if (parameterDescriptor.hasDefaultValue()) {
                        iv.load(maskIndex, Type.INT_TYPE);
                        iv.iconst(1 << index);
                        iv.and(Type.INT_TYPE);
                        Label loadArg = new Label();
                        iv.ifeq(loadArg);

                        JetParameter jetParameter = (JetParameter) state.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, parameterDescriptor);
                        assert jetParameter != null;
                        codegen.gen(jetParameter.getDefaultValue(), t);

                        endArg = new Label();
                        iv.goTo(endArg);

                        iv.mark(loadArg);
                    }

                    iv.load(var, t);
                    var += t.getSize();

                    if (parameterDescriptor.hasDefaultValue()) {
                        iv.mark(endArg);
                    }
                }

                if(!isStatic) {
                    if(kind == OwnerKind.TRAIT_IMPL) {
                        iv.invokeinterface(ownerInternalName, jvmSignature.getName(), jvmSignature.getDescriptor());
                    }
                    else {
                        if(!isConstructor)
                            iv.invokevirtual(ownerInternalName, jvmSignature.getName(), jvmSignature.getDescriptor());
                        else
                            iv.invokespecial(ownerInternalName, jvmSignature.getName(), jvmSignature.getDescriptor());
                    }
                }
                else {
                    iv.invokestatic(ownerInternalName, jvmSignature.getName(), jvmSignature.getDescriptor());
                }

                iv.areturn(jvmSignature.getReturnType());

                endVisit(mv, "default method", state.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, functionDescriptor));
                mv.visitEnd();
            }
        }
    }

    private static boolean differentMethods(Method method, Method overriden) {
        if(!method.getReturnType().equals(overriden.getReturnType()))
            return true;
        Type[] methodArgumentTypes = method.getArgumentTypes();
        Type[] overridenArgumentTypes = overriden.getArgumentTypes();
        if(methodArgumentTypes.length != overridenArgumentTypes.length)
            return true;
        for(int i = 0; i != methodArgumentTypes.length; ++i)
            if(!methodArgumentTypes[i].equals(overridenArgumentTypes[i]))
                return true;
        return false;
    }
    
    private static void checkOverride(CodegenContext owner, GenerationState state, ClassBuilder v, Method jvmSignature, FunctionDescriptor functionDescriptor, FunctionDescriptor overriddenFunction) {
        Method method = state.getTypeMapper().mapSignature(functionDescriptor.getName(), functionDescriptor).getAsmMethod();
        Method overriden = state.getTypeMapper().mapSignature(overriddenFunction.getName(), overriddenFunction.getOriginal()).getAsmMethod();

        if(overriddenFunction.getModality() == Modality.ABSTRACT) {
            Set<? extends FunctionDescriptor> overriddenFunctions = overriddenFunction.getOverriddenDescriptors();
            for (FunctionDescriptor of : overriddenFunctions) {
                checkOverride(owner, state, v, jvmSignature, overriddenFunction, of.getOriginal());
            }
        }

        if(differentMethods(method, overriden)) {
            int flags = ACC_PUBLIC | ACC_BRIDGE; // TODO.

            final MethodVisitor mv = v.newMethod(null, flags, jvmSignature.getName(), overriden.getDescriptor(), null, null);
            if (v.generateCode() == ClassBuilder.Mode.STUBS) {
                StubCodegen.generateStubCode(mv);
            }
            else if (v.generateCode() == ClassBuilder.Mode.FULL) {
                mv.visitCode();

                Type[] argTypes = overriden.getArgumentTypes();
                InstructionAdapter iv = new InstructionAdapter(mv);
                iv.load(0, JetTypeMapper.TYPE_OBJECT);
                for (int i = 0, reg = 1; i < argTypes.length; i++) {
                    Type argType = argTypes[i];
                    iv.load(reg, argType);
                    if(argType.getSort() == Type.OBJECT) {
                        StackValue.onStack(JetTypeMapper.TYPE_OBJECT).put(method.getArgumentTypes()[i], iv);
                    }

                    //noinspection AssignmentToForLoopParameter
                    reg += argType.getSize();
                }

                iv.invokevirtual(state.getTypeMapper().mapType(((ClassDescriptor) owner.getContextDescriptor()).getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName(), jvmSignature.getName(), jvmSignature.getDescriptor());
                if(JetTypeMapper.isPrimitive(jvmSignature.getReturnType()) && !JetTypeMapper.isPrimitive(overriden.getReturnType()))
                    StackValue.valueOf(iv, jvmSignature.getReturnType());
                if(jvmSignature.getReturnType() == Type.VOID_TYPE)
                    iv.aconst(null);
                iv.areturn(overriden.getReturnType());
                endVisit(mv, "bridge method", state.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, functionDescriptor));
            }
        }
    }

    public void genDelegate(FunctionDescriptor functionDescriptor, CallableMemberDescriptor overriddenDescriptor, StackValue field) {
        JvmMethodSignature jvmMethodSignature = state.getTypeMapper().mapSignature(functionDescriptor.getName(), functionDescriptor);
        genDelegate(functionDescriptor, overriddenDescriptor, field, jvmMethodSignature);
    }

    public void genDelegate(CallableMemberDescriptor functionDescriptor, CallableMemberDescriptor overriddenDescriptor, StackValue field, JvmMethodSignature jvmMethodSignature) {
        Method method = jvmMethodSignature.getAsmMethod();
        int flags = ACC_PUBLIC | ACC_SYNTHETIC; // TODO.

        final MethodVisitor mv = v.newMethod(null, flags, method.getName(), method.getDescriptor(), null, null);
        if (v.generateCode() == ClassBuilder.Mode.STUBS) {
            StubCodegen.generateStubCode(mv);
        }
        else if (v.generateCode() == ClassBuilder.Mode.FULL) {
            mv.visitCode();

            Type[] argTypes = method.getArgumentTypes();
            InstructionAdapter iv = new InstructionAdapter(mv);
            iv.load(0, JetTypeMapper.TYPE_OBJECT);
            for (int i = 0, reg = 1; i < argTypes.length; i++) {
                Type argType = argTypes[i];
                iv.load(reg, argType);
                if(argType.getSort() == Type.OBJECT) {
                    StackValue.onStack(JetTypeMapper.TYPE_OBJECT).put(method.getArgumentTypes()[i], iv);
                }

                //noinspection AssignmentToForLoopParameter
                reg += argType.getSize();
            }

            iv.load(0, JetTypeMapper.TYPE_OBJECT);
            field.put(field.type, iv);
            ClassDescriptor classDescriptor = (ClassDescriptor) overriddenDescriptor.getContainingDeclaration();
            String internalName = state.getTypeMapper().mapType(classDescriptor.getDefaultType()).getInternalName();
            if(classDescriptor.getKind() == ClassKind.TRAIT)
                iv.invokeinterface(internalName, method.getName(), method.getDescriptor());
            else
                iv.invokevirtual(internalName, method.getName(), method.getDescriptor());
            iv.areturn(method.getReturnType());
            endVisit(mv, "delegate method", state.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, functionDescriptor));
        }
    }
}
