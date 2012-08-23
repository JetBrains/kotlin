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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.jet.codegen.JetTypeMapper.TYPE_OBJECT;

/*
 * @author max
 * @author alex.tkachman
 */
public abstract class CodegenContext {

    @NotNull
    private final DeclarationDescriptor contextDescriptor;

    private final OwnerKind contextKind;
    @Nullable
    private final CodegenContext parentContext;
    public final ObjectOrClosureCodegen closure;
    private final boolean hasThis;

    HashMap<DeclarationDescriptor, DeclarationDescriptor> accessors;

    protected StackValue outerExpression;

    protected Type outerWasUsed;

    public CodegenContext(
            @NotNull DeclarationDescriptor contextDescriptor,
            OwnerKind contextKind,
            @Nullable CodegenContext parentContext,
            @Nullable ObjectOrClosureCodegen closureCodegen,
            boolean hasThis
    ) {
        this.contextDescriptor = contextDescriptor;
        this.contextKind = contextKind;
        this.parentContext = parentContext;
        closure = closureCodegen;
        this.hasThis = hasThis;
    }

    @NotNull
    protected abstract ClassDescriptor getThisDescriptor();

    protected final boolean hasThisDescriptor() {
        return hasThis;
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

    protected CallableDescriptor getReceiverDescriptor() {
        return null;
    }

    protected StackValue getOuterExpression(@Nullable StackValue prefix) {
        if (outerExpression == null) {
            throw new UnsupportedOperationException();
        }

        outerWasUsed = outerExpression.type;
        return prefix != null ? StackValue.composed(prefix, outerExpression) : outerExpression;
    }

    @NotNull
    public DeclarationDescriptor getContextDescriptor() {
        return contextDescriptor;
    }

    public OwnerKind getContextKind() {
        return contextKind;
    }

    public CodegenContext intoNamespace(NamespaceDescriptor descriptor) {
        return new CodegenContexts.NamespaceContext(descriptor, this, null);
    }

    public CodegenContext intoNamespacePart(String delegateTo, NamespaceDescriptor descriptor) {
        return new CodegenContexts.NamespaceContext(descriptor, this, new OwnerKind.StaticDelegateKind(delegateTo));
    }

    public CodegenContext intoClass(ClassDescriptor descriptor, OwnerKind kind, JetTypeMapper typeMapper) {
        return new CodegenContexts.ClassContext(descriptor, kind, this, typeMapper);
    }

    public CodegenContext intoAnonymousClass(
            @NotNull ObjectOrClosureCodegen closure,
            ClassDescriptor descriptor,
            OwnerKind kind,
            JetTypeMapper typeMapper
    ) {
        return new CodegenContexts.AnonymousClassContext(descriptor, kind, this, closure, typeMapper);
    }

    public CodegenContexts.MethodContext intoFunction(FunctionDescriptor descriptor) {
        return new CodegenContexts.MethodContext(descriptor, getContextKind(), this);
    }

    public CodegenContexts.ConstructorContext intoConstructor(ConstructorDescriptor descriptor, JetTypeMapper typeMapper) {
        if (descriptor == null) {
            descriptor = new ConstructorDescriptorImpl(getThisDescriptor(), Collections.<AnnotationDescriptor>emptyList(), true)
                    .initialize(Collections.<TypeParameterDescriptor>emptyList(), Collections.<ValueParameterDescriptor>emptyList(),
                                Visibilities.PUBLIC);
        }
        return new CodegenContexts.ConstructorContext(descriptor, getContextKind(), this, typeMapper);
    }

    public CodegenContext intoScript(@NotNull ScriptDescriptor script, @NotNull ClassDescriptor classDescriptor) {
        return new CodegenContexts.ScriptContext(script, classDescriptor, OwnerKind.IMPLEMENTATION, this, closure);
    }

    public CodegenContexts.ClosureContext intoClosure(
            FunctionDescriptor funDescriptor,
            ClassDescriptor classDescriptor,
            JvmClassName internalClassName,
            ClosureCodegen closureCodegen,
            JetTypeMapper typeMapper
    ) {
        return new CodegenContexts.ClosureContext(funDescriptor, classDescriptor, this, closureCodegen, internalClassName, typeMapper);
    }

    public FrameMap prepareFrame(JetTypeMapper mapper) {
        FrameMap frameMap = new FrameMap();

        if (getContextKind() != OwnerKind.NAMESPACE) {
            frameMap.enterTemp(TYPE_OBJECT);  // 0 slot for this
        }

        CallableDescriptor receiverDescriptor = getReceiverDescriptor();
        if (receiverDescriptor != null) {
            Type type = mapper.mapType(receiverDescriptor.getReceiverParameter().getType(), MapTypeMode.VALUE);
            frameMap.enterTemp(type);  // Next slot for fake this
        }

        return frameMap;
    }

    @Nullable
    public CodegenContext getParentContext() {
        return parentContext;
    }

    public StackValue lookupInContext(DeclarationDescriptor d, InstructionAdapter v, StackValue result) {
        final ObjectOrClosureCodegen top = closure;
        if (top != null) {
            final StackValue answer = top.lookupInContext(d, result);
            if (answer != null) {
                return result == null ? answer : StackValue.composed(result, answer);
            }

            StackValue outer = getOuterExpression(null);
            result = result == null ? outer : StackValue.composed(result, outer);
        }

        return parentContext != null ? parentContext.lookupInContext(d, v, result) : null;
    }

    public Type enclosingClassType(JetTypeMapper typeMapper) {
        CodegenContext cur = getParentContext();
        while (cur != null && !(cur.getContextDescriptor() instanceof ClassDescriptor)) {
            cur = cur.getParentContext();
        }

        return cur == null ? null : typeMapper.mapType(((ClassDescriptor) cur.getContextDescriptor()).getDefaultType(), MapTypeMode.IMPL);
    }

    DeclarationDescriptor getAccessor(DeclarationDescriptor descriptor) {
        if (accessors == null) {
            accessors = new HashMap<DeclarationDescriptor, DeclarationDescriptor>();
        }
        descriptor = descriptor.getOriginal();
        DeclarationDescriptor accessor = accessors.get(descriptor);
        if (accessor != null) {
            return accessor;
        }

        if (descriptor instanceof SimpleFunctionDescriptor) {
            accessor = new AccessorForFunctionDescriptor(descriptor, contextDescriptor, accessors.size());
        }
        else if (descriptor instanceof PropertyDescriptor) {
            accessor = new AccessorForPropertyDescriptor((PropertyDescriptor) descriptor, contextDescriptor, accessors.size());
        }
        else {
            throw new UnsupportedOperationException();
        }
        accessors.put(descriptor, accessor);
        return accessor;
    }

    public StackValue getReceiverExpression(JetTypeMapper typeMapper) {
        assert getReceiverDescriptor() != null;
        Type asmType = typeMapper.mapType(getReceiverDescriptor().getReceiverParameter().getType(), MapTypeMode.VALUE);
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
}
