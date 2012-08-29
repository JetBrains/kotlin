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

package org.jetbrains.jet.codegen.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.codegen.JetTypeMapper.OBJECT_TYPE;
import static org.jetbrains.jet.codegen.context.CodegenBinding.CLASS_FOR_FUNCTION;
import static org.jetbrains.jet.codegen.context.CodegenBinding.FQN;

/*
 * @author max
 * @author alex.tkachman
 */
public abstract class CodegenContext {

    public static final CodegenContext STATIC =
            new CodegenContext(new FakeDescriptorForStaticContext(), OwnerKind.NAMESPACE, null, null, null, null) {
                @Override
                public boolean isStatic() {
                    return true;
                }

                @Override
                public String toString() {
                    return "ROOT";
                }
            };
    private static final StackValue local1 = StackValue.local(1, OBJECT_TYPE);
    @NotNull
    private final DeclarationDescriptor contextDescriptor;

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
            OwnerKind contextKind,
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

    public CallableDescriptor getReceiverDescriptor() {
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

    public OwnerKind getContextKind() {
        return contextKind;
    }

    public CodegenContext intoNamespace(NamespaceDescriptor descriptor) {
        return new NamespaceContext(descriptor, this, null);
    }

    public CodegenContext intoNamespacePart(String delegateTo, NamespaceDescriptor descriptor) {
        return new NamespaceContext(descriptor, this, new OwnerKind.StaticDelegateKind(delegateTo));
    }

    public CodegenContext intoClass(ClassDescriptor descriptor, OwnerKind kind, GenerationState state) {
        return new ClassContext(state.getInjector().getJetTypeMapper(), descriptor, kind, this, null);
    }

    public CodegenContext intoAnonymousClass(
            ClassDescriptor descriptor,
            ExpressionCodegen expressionCodegen
    ) {
        final JetTypeMapper typeMapper = expressionCodegen.getState().getInjector().getJetTypeMapper();
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
        final JetTypeMapper typeMapper = expressionCodegen.getState().getInjector().getJetTypeMapper();
        return new ClosureContext(typeMapper, funDescriptor,
                                  typeMapper.getBindingContext().get(CLASS_FOR_FUNCTION, funDescriptor),
                                  this, expressionCodegen);
    }

    public FrameMap prepareFrame(JetTypeMapper mapper) {
        FrameMap frameMap = new FrameMap();

        if (getContextKind() != OwnerKind.NAMESPACE) {
            frameMap.enterTemp(OBJECT_TYPE);  // 0 slot for this
        }

        CallableDescriptor receiverDescriptor = getReceiverDescriptor();
        if (receiverDescriptor != null) {
            Type type = mapper.mapType(receiverDescriptor.getReceiverParameter().getType(), MapTypeMode.VALUE);
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

    public DeclarationDescriptor getAccessor(DeclarationDescriptor descriptor) {
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

    protected void initOuterExpression(JetTypeMapper typeMapper, ClassDescriptor classDescriptor) {
        final ClassDescriptor enclosingClass = getEnclosingClass();
        outerExpression = enclosingClass != null
                          ? StackValue
                .field(typeMapper.mapType(enclosingClass.getDefaultType(), MapTypeMode.VALUE), CodegenBinding.getJvmClassName(
                        typeMapper.getBindingTrace(), classDescriptor), CodegenUtil.THIS$0,
                       false)
                          : null;
    }

    public StackValue lookupInContext(DeclarationDescriptor d, StackValue result, GenerationState state, boolean ignoreNoOuter) {
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
            result = result == null ? outer : StackValue.composed(result, outer);
        }

        return parentContext != null ? parentContext.lookupInContext(d, result, state, ignoreNoOuter) : null;
    }

    @NotNull
    public Map<DeclarationDescriptor, DeclarationDescriptor> getAccessors() {
        return accessors == null ? Collections.<DeclarationDescriptor, DeclarationDescriptor>emptyMap() : accessors;
    }

    private static class FakeDescriptorForStaticContext implements DeclarationDescriptor {

        @NotNull
        @Override
        public DeclarationDescriptor getOriginal() {
            throw new IllegalStateException();
        }

        @Override
        public DeclarationDescriptor getContainingDeclaration() {
            throw new IllegalStateException();
        }

        @Override
        public DeclarationDescriptor substitute(TypeSubstitutor substitutor) {
            throw new IllegalStateException();
        }

        @Override
        public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
            throw new IllegalStateException();
        }

        @Override
        public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
            throw new IllegalStateException();
        }

        @Override
        public List<AnnotationDescriptor> getAnnotations() {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public Name getName() {
            throw new IllegalStateException();
        }
    }

    private abstract static class ReceiverContext extends CodegenContext {
        final CallableDescriptor receiverDescriptor;

        public ReceiverContext(
                CallableDescriptor contextDescriptor,
                OwnerKind contextKind,
                CodegenContext parentContext,
                @Nullable MutableClosure closure,
                ClassDescriptor thisDescriptor,
                @Nullable LocalLookup localLookup
        ) {
            super(contextDescriptor, contextKind, parentContext, closure, thisDescriptor, localLookup);
            receiverDescriptor = contextDescriptor.getReceiverParameter().exists() ? contextDescriptor : null;
        }

        @Override
        public CallableDescriptor getReceiverDescriptor() {
            return receiverDescriptor;
        }
    }

    public static class MethodContext extends ReceiverContext {
        public MethodContext(
                @NotNull FunctionDescriptor contextType,
                OwnerKind contextKind,
                CodegenContext parentContext
        ) {
            super(contextType instanceof PropertyAccessorDescriptor
                  ? ((PropertyAccessorDescriptor) contextType).getCorrespondingProperty()
                  : contextType, contextKind, parentContext, null,
                  parentContext.hasThisDescriptor() ? parentContext.getThisDescriptor() : null, null);
        }

        @Override
        public StackValue lookupInContext(DeclarationDescriptor d, StackValue result, GenerationState state, boolean ignoreNoOuter) {
            if (getContextDescriptor() == d) {
                return StackValue.local(0, JetTypeMapper.OBJECT_TYPE);
            }

            //noinspection ConstantConditions
            return getParentContext().lookupInContext(d, result, state, ignoreNoOuter);
        }

        @Override
        public boolean isStatic() {
            //noinspection ConstantConditions
            return getParentContext().isStatic();
        }

        @Override
        public StackValue getOuterExpression(StackValue prefix, boolean ignoreNoOuter) {
            //noinspection ConstantConditions
            return getParentContext().getOuterExpression(prefix, false);
        }

        @Override
        public String toString() {
            return "Method: " + getContextDescriptor();
        }
    }

    public static class ConstructorContext extends MethodContext {
        public ConstructorContext(
                ConstructorDescriptor contextDescriptor,
                OwnerKind kind,
                CodegenContext parent
        ) {
            super(contextDescriptor, kind, parent);

            final ClassDescriptor type = getEnclosingClass();
            outerExpression = type != null ? local1 : null;
        }

        @Override
        public StackValue getOuterExpression(StackValue prefix, boolean ignoreNoOuter) {
            return outerExpression;
        }

        @Override
        public String toString() {
            return "Constructor: " + getContextDescriptor().getName();
        }
    }

    public static class ScriptContext extends CodegenContext {
        @NotNull
        private final ScriptDescriptor scriptDescriptor;

        public ScriptContext(
                @NotNull ScriptDescriptor scriptDescriptor,
                @NotNull ClassDescriptor contextDescriptor,
                @NotNull OwnerKind contextKind,
                @Nullable CodegenContext parentContext,
                @Nullable MutableClosure closure
        ) {
            super(contextDescriptor, contextKind, parentContext, closure, contextDescriptor, null);

            this.scriptDescriptor = scriptDescriptor;
        }

        @NotNull
        public ScriptDescriptor getScriptDescriptor() {
            return scriptDescriptor;
        }

        @Override
        public boolean isStatic() {
            return true;
        }
    }

    public static class ClassContext extends CodegenContext {
        public ClassContext(
                JetTypeMapper typeMapper,
                ClassDescriptor contextDescriptor,
                OwnerKind contextKind,
                CodegenContext parentContext,
                LocalLookup localLookup
        ) {
            super(contextDescriptor, contextKind, parentContext, (MutableClosure) typeMapper.getCalculatedClosure(contextDescriptor),
                  contextDescriptor,
                  localLookup);
            initOuterExpression(typeMapper, contextDescriptor);
        }

        @Override
        public boolean isStatic() {
            return false;
        }
    }

    private static class AnonymousClassContext extends CodegenContext {
        public AnonymousClassContext(
                JetTypeMapper typeMapper,
                ClassDescriptor contextDescriptor,
                OwnerKind contextKind,
                CodegenContext parentContext,
                LocalLookup localLookup
        ) {
            super(contextDescriptor, contextKind, parentContext, (MutableClosure) typeMapper.getCalculatedClosure(contextDescriptor),
                  contextDescriptor,
                  localLookup);
            initOuterExpression(typeMapper, contextDescriptor);
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public String toString() {
            return "Anonymous: " + getThisDescriptor();
        }
    }

    private static class ClosureContext extends ReceiverContext {
        private final ClassDescriptor classDescriptor;

        public ClosureContext(
                JetTypeMapper typeMapper,
                FunctionDescriptor contextDescriptor,
                ClassDescriptor classDescriptor,
                CodegenContext parentContext,
                LocalLookup localLookup
        ) {
            super(contextDescriptor, OwnerKind.IMPLEMENTATION, parentContext,
                  (MutableClosure) typeMapper.getCalculatedClosure(classDescriptor), classDescriptor, localLookup);
            this.classDescriptor = classDescriptor;

            initOuterExpression(typeMapper, classDescriptor);
        }

        @NotNull
        @Override
        public DeclarationDescriptor getContextDescriptor() {
            return classDescriptor;
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public String toString() {
            return "Closure: " + classDescriptor;
        }
    }

    public static class NamespaceContext extends CodegenContext {
        public NamespaceContext(NamespaceDescriptor contextDescriptor, CodegenContext parent, OwnerKind kind) {
            super(contextDescriptor, kind != null ? kind : OwnerKind.NAMESPACE, parent, null, null, null);
        }

        @Override
        public boolean isStatic() {
            return true;
        }

        @Override
        public String toString() {
            return "Namespace: " + getContextDescriptor().getName();
        }
    }
}
