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
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyAccessorDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class CodegenContexts {
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

        @NotNull @Override public Name getName() {
            return null;
        }
    }

    public static final CodegenContext STATIC = new CodegenContext(new FakeDescriptorForStaticContext(), OwnerKind.NAMESPACE, null, null) {
        @Override
        protected ClassDescriptor getThisDescriptor() {
            return null;
        }

        @Override
        public boolean isStatic() {
            return true;
        }

        @Override
        public String toString() {
            return "ROOT";
        }
    };
    protected static final StackValue local0 = StackValue.local(0, JetTypeMapper.TYPE_OBJECT);
    protected static final StackValue local1 = StackValue.local(1, JetTypeMapper.TYPE_OBJECT);

    public abstract static class ReceiverContext extends CodegenContext {
        final CallableDescriptor receiverDescriptor;

        public ReceiverContext(CallableDescriptor contextType, OwnerKind contextKind, CodegenContext parentContext, @Nullable ObjectOrClosureCodegen closureCodegen) {
            super(contextType, contextKind, parentContext, closureCodegen);
            receiverDescriptor = contextType.getReceiverParameter().exists() ? contextType : null;
        }

        @Override
        protected CallableDescriptor getReceiverDescriptor() {
            return receiverDescriptor;
        }
    }

    public static class MethodContext  extends ReceiverContext {
        public MethodContext(FunctionDescriptor contextType, OwnerKind contextKind, CodegenContext parentContext) {
            super(contextType instanceof PropertyAccessorDescriptor ? ((PropertyAccessorDescriptor)contextType).getCorrespondingProperty() : contextType, contextKind, parentContext, null);
        }

        @Override
        protected ClassDescriptor getThisDescriptor() {
            return getParentContext().getThisDescriptor();
        }

        public StackValue lookupInContext(DeclarationDescriptor d, InstructionAdapter v, StackValue result) {
            return getParentContext().lookupInContext(d, v, result);
        }

        public Type enclosingClassType(JetTypeMapper typeMapper) {
            return getParentContext().enclosingClassType(typeMapper);
        }

        @Override
        public boolean isStatic() {
            return getParentContext().isStatic();
        }

        protected StackValue getOuterExpression(StackValue prefix) {
            return getParentContext().getOuterExpression(prefix);
        }

        @Override
        public String toString() {
            return "Method: " + getContextDescriptor();
        }
    }

    public static class ConstructorContext extends MethodContext {
        public ConstructorContext(ConstructorDescriptor contextType, OwnerKind kind, CodegenContext parent, JetTypeMapper typeMapper) {
            super(contextType, kind, parent);

            final Type type = enclosingClassType(typeMapper);
            outerExpression = type != null
                        ? local1
                        : null;
        }

        protected StackValue getOuterExpression(StackValue prefix) {
            return outerExpression;
        }

        @Override
        public String toString() {
            return "Constructor: " + getContextDescriptor().getName();
        }
    }

    public static class ScriptContext extends CodegenContext {

        public ScriptContext(
                @NotNull DeclarationDescriptor contextType,
                @NotNull OwnerKind contextKind,
                @Nullable CodegenContext parentContext,
                @Nullable ObjectOrClosureCodegen closureCodegen) {
            super(contextType, contextKind, parentContext, closureCodegen);
        }

        @Override
        protected ClassDescriptor getThisDescriptor() {
            return null;
        }

        @Override
        public boolean isStatic() {
            throw new IllegalStateException();
        }
    }

    public static class ClassContext extends CodegenContext {
        public ClassContext(ClassDescriptor contextType, OwnerKind contextKind, CodegenContext parentContext, JetTypeMapper typeMapper) {
            super(contextType, contextKind, parentContext, null);

            final Type type = enclosingClassType(typeMapper);
            outerExpression = type != null
                        ? StackValue.field(type, typeMapper.getClassFQName(contextType), "this$0", false)
                        : null;
        }

        @Override
        protected ClassDescriptor getThisDescriptor() {
            return (ClassDescriptor) getContextDescriptor();
        }

        @Override
        public boolean isStatic() {
            return false;
        }
    }

    public static class AnonymousClassContext extends CodegenContext {
        public AnonymousClassContext(ClassDescriptor contextType, OwnerKind contextKind, CodegenContext parentContext, @NotNull ObjectOrClosureCodegen closure, JetTypeMapper typeMapper) {
            super(contextType, contextKind, parentContext, closure);

            final Type type = enclosingClassType(typeMapper);
            Type owner = closure.state.getInjector().getJetTypeMapper().mapType(contextType.getDefaultType(), MapTypeMode.IMPL);
            outerExpression = type != null
                        ? StackValue.field(type, JvmClassName.byType(owner), "this$0", false)
                        : null;
        }

        @Override
        protected ClassDescriptor getThisDescriptor() {
            return (ClassDescriptor) getContextDescriptor();
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

    public static class ClosureContext extends ReceiverContext {
        private ClassDescriptor classDescriptor;

        public ClosureContext(FunctionDescriptor contextType, ClassDescriptor classDescriptor, CodegenContext parentContext, @NotNull ObjectOrClosureCodegen closureCodegen, JvmClassName internalClassName, JetTypeMapper typeMapper) {
            super(contextType, OwnerKind.IMPLEMENTATION, parentContext, closureCodegen);
            this.classDescriptor = classDescriptor;

            final Type type = enclosingClassType(typeMapper);
            outerExpression = type != null
                        ? StackValue.field(type, internalClassName, "this$0", false)
                        : null;
        }

        @Override
        protected ClassDescriptor getThisDescriptor() {
            return classDescriptor;
        }

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
        public NamespaceContext(NamespaceDescriptor contextType, CodegenContext parent) {
            super(contextType, OwnerKind.NAMESPACE, parent, null);
        }

        @Override
        protected ClassDescriptor getThisDescriptor() {
            return null;
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
