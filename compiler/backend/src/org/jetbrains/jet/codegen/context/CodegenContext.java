/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.codegen.binding.MutableClosure;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ConstructorDescriptorImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.codegen.AsmUtil.CAPTURED_THIS_FIELD;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.*;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;

public abstract class CodegenContext {

    public static final CodegenContext STATIC = new RootContext();

    @NotNull
    private final DeclarationDescriptor contextDescriptor;

    @NotNull
    private final OwnerKind contextKind;

    @Nullable
    private final CodegenContext parentContext;
    private final ClassDescriptor thisDescriptor;

    public final MutableClosure closure;

    HashMap<DeclarationDescriptor, DeclarationDescriptor> accessors;

    protected StackValue outerExpression;
    private final LocalLookup enclosingLocalLookup;

    public CodegenContext(
            @NotNull DeclarationDescriptor contextDescriptor,
            @NotNull OwnerKind contextKind,
            @Nullable CodegenContext parentContext,
            @Nullable MutableClosure closure,
            @Nullable ClassDescriptor thisDescriptor,
            @Nullable LocalLookup expressionCodegen
    ) {
        this.contextDescriptor = contextDescriptor;
        this.contextKind = contextKind;
        this.parentContext = parentContext;
        this.closure = closure;
        this.thisDescriptor = thisDescriptor;
        this.enclosingLocalLookup = expressionCodegen;
    }

    @NotNull
    public final ClassDescriptor getThisDescriptor() {
        if (thisDescriptor == null) {
            throw new UnsupportedOperationException();
        }
        return thisDescriptor;
    }

    public final boolean hasThisDescriptor() {
        return thisDescriptor != null;
    }

    public DeclarationDescriptor getClassOrNamespaceDescriptor() {
        CodegenContext c = this;
        while (true) {
            assert c != null;
            DeclarationDescriptor contextDescriptor = c.getContextDescriptor();
            if (!(contextDescriptor instanceof ClassDescriptor) && !(contextDescriptor instanceof NamespaceDescriptor)) {
                c = c.getParentContext();
            }
            else {
                return contextDescriptor;
            }
        }
    }

    /**
     * This method returns not null only if context descriptor corresponds to method or function which has receiver
     */
    @Nullable
    public final CallableDescriptor getCallableDescriptorWithReceiver() {
        if (contextDescriptor instanceof CallableDescriptor) {
            final CallableDescriptor callableDescriptor = (CallableDescriptor) getContextDescriptor();
            return callableDescriptor.getReceiverParameter() != null ? callableDescriptor : null;
        }
        return null;
    }

    public StackValue getOuterExpression(@Nullable StackValue prefix, boolean ignoreNoOuter) {
        if (outerExpression == null) {
            if (ignoreNoOuter) {
                return null;
            }
            else {
                throw new UnsupportedOperationException();
            }
        }

        closure.setCaptureThis();
        return prefix != null ? StackValue.composed(prefix, outerExpression) : outerExpression;
    }

    @NotNull
    public DeclarationDescriptor getContextDescriptor() {
        return contextDescriptor;
    }

    @NotNull
    public OwnerKind getContextKind() {
        return contextKind;
    }

    public CodegenContext intoNamespace(@NotNull NamespaceDescriptor descriptor) {
        return new NamespaceContext(descriptor, this, OwnerKind.NAMESPACE);
    }

    public CodegenContext intoNamespacePart(String delegateTo, NamespaceDescriptor descriptor) {
        return new NamespaceContext(descriptor, this, new OwnerKind.StaticDelegateKind(delegateTo));
    }

    public CodegenContext intoClass(ClassDescriptor descriptor, OwnerKind kind, GenerationState state) {
        return new ClassContext(state.getTypeMapper(), descriptor, kind, this, null);
    }

    public CodegenContext intoAnonymousClass(
            ClassDescriptor descriptor,
            ExpressionCodegen expressionCodegen
    ) {
        final JetTypeMapper typeMapper = expressionCodegen.getState().getTypeMapper();
        return new AnonymousClassContext(typeMapper, descriptor, OwnerKind.IMPLEMENTATION, this,
                                         expressionCodegen);
    }

    public MethodContext intoFunction(FunctionDescriptor descriptor) {
        return new MethodContext(descriptor, getContextKind(), this);
    }

    public ConstructorContext intoConstructor(ConstructorDescriptor descriptor) {
        if (descriptor == null) {
            descriptor = new ConstructorDescriptorImpl(getThisDescriptor(), Collections.<AnnotationDescriptor>emptyList(), true)
                    .initialize(Collections.<TypeParameterDescriptor>emptyList(), Collections.<ValueParameterDescriptor>emptyList(),
                                Visibilities.PUBLIC);
        }
        return new ConstructorContext(descriptor, getContextKind(), this);
    }

    public CodegenContext intoScript(@NotNull ScriptDescriptor script, @NotNull ClassDescriptor classDescriptor) {
        return new ScriptContext(script, classDescriptor, OwnerKind.IMPLEMENTATION, this, closure);
    }

    public CodegenContext intoClosure(
            FunctionDescriptor funDescriptor,
            ExpressionCodegen expressionCodegen
    ) {
        final JetTypeMapper typeMapper = expressionCodegen.getState().getTypeMapper();
        return new ClosureContext(typeMapper, funDescriptor,
                                  typeMapper.getBindingContext().get(CLASS_FOR_FUNCTION, funDescriptor),
                                  this, expressionCodegen);
    }

    public FrameMap prepareFrame(JetTypeMapper mapper) {
        FrameMap frameMap = new FrameMap();

        if (getContextKind() != OwnerKind.NAMESPACE) {
            frameMap.enterTemp(OBJECT_TYPE);  // 0 slot for this
        }

        CallableDescriptor receiverDescriptor = getCallableDescriptorWithReceiver();
        if (receiverDescriptor != null) {
            Type type = mapper.mapType(receiverDescriptor.getReceiverParameter().getType());
            frameMap.enterTemp(type);  // Next slot for receiver
        }

        return frameMap;
    }

    @Nullable
    public CodegenContext getParentContext() {
        return parentContext;
    }

    public ClassDescriptor getEnclosingClass() {
        CodegenContext cur = getParentContext();
        while (cur != null && !(cur.getContextDescriptor() instanceof ClassDescriptor)) {
            cur = cur.getParentContext();
        }

        return cur == null ? null : (ClassDescriptor) cur.getContextDescriptor();
    }

    @Nullable
    public CodegenContext findParentContextWithDescriptor(DeclarationDescriptor descriptor) {
        CodegenContext c = this;
        while (c != null) {
            if (c.getContextDescriptor() == descriptor) break;
            c = c.getParentContext();
        }
        return c;
    }

    public DeclarationDescriptor getAccessor(DeclarationDescriptor descriptor) {
        if (accessors == null) {
            accessors = new HashMap<DeclarationDescriptor, DeclarationDescriptor>();
        }
        descriptor = descriptor.getOriginal();
        DeclarationDescriptor accessor = accessors.get(descriptor);
        if (accessor != null) {
            return accessor;
        }

        if (descriptor instanceof SimpleFunctionDescriptor || descriptor instanceof ConstructorDescriptor) {
            accessor = new AccessorForFunctionDescriptor(descriptor, contextDescriptor, accessors.size());
        }
        else if (descriptor instanceof PropertyDescriptor) {
            accessor = new AccessorForPropertyDescriptor((PropertyDescriptor) descriptor, contextDescriptor, accessors.size());
        }
        else {
            throw new UnsupportedOperationException("Do not know how to create accessor for descriptor " + descriptor);
        }
        accessors.put(descriptor, accessor);
        return accessor;
    }

    public StackValue getReceiverExpression(JetTypeMapper typeMapper) {
        assert getCallableDescriptorWithReceiver() != null;
        @SuppressWarnings("ConstantConditions")
        Type asmType = typeMapper.mapType(getCallableDescriptorWithReceiver().getReceiverParameter().getType());
        return hasThisDescriptor() ? StackValue.local(1, asmType) : StackValue.local(0, asmType);
    }

    public abstract boolean isStatic();

    public void copyAccessors(Map<DeclarationDescriptor, DeclarationDescriptor> accessors) {
        if (accessors != null) {
            if (this.accessors == null) {
                this.accessors = new HashMap<DeclarationDescriptor, DeclarationDescriptor>();
            }
            this.accessors.putAll(accessors);
        }
    }

    protected void initOuterExpression(JetTypeMapper typeMapper, ClassDescriptor classDescriptor) {
        ClassDescriptor enclosingClass = getEnclosingClass();
        outerExpression = enclosingClass != null && canHaveOuter(typeMapper.getBindingContext(), classDescriptor)
                          ? StackValue.field(typeMapper.mapType(enclosingClass),
                                             CodegenBinding.getJvmInternalName(typeMapper.getBindingTrace(), classDescriptor),
                                             CAPTURED_THIS_FIELD,
                                             false)
                          : null;
    }

    public StackValue lookupInContext(DeclarationDescriptor d, @Nullable StackValue result, GenerationState state, boolean ignoreNoOuter) {
        final MutableClosure top = closure;
        if (top != null) {
            EnclosedValueDescriptor answer = closure.getCaptureVariables().get(d);
            if (answer != null) {
                StackValue innerValue = answer.getInnerValue();
                return result == null ? innerValue : StackValue.composed(result, innerValue);
            }

            for (LocalLookup.LocalLookupCase aCase : LocalLookup.LocalLookupCase.values()) {
                if (aCase.isCase(d, state)) {
                    StackValue innerValue = aCase.innerValue(d, enclosingLocalLookup, state, closure, state.getBindingContext().get(FQN, getThisDescriptor()));
                    if (innerValue == null) {
                        break;
                    }
                    else {
                        return result == null ? innerValue : StackValue.composed(result, innerValue);
                    }
                }
            }

            StackValue outer = getOuterExpression(null, ignoreNoOuter);
            result = result == null || outer == null ? outer : StackValue.composed(result, outer);
        }

        return parentContext != null ? parentContext.lookupInContext(d, result, state, ignoreNoOuter) : null;
    }

    @NotNull
    public Map<DeclarationDescriptor, DeclarationDescriptor> getAccessors() {
        return accessors == null ? Collections.<DeclarationDescriptor, DeclarationDescriptor>emptyMap() : accessors;
    }
}
