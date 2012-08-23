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
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class CodegenContexts {
    private CodegenContexts() {
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
    private static final StackValue local1 = StackValue.local(1, JetTypeMapper.TYPE_OBJECT);

    public abstract static class ReceiverContext extends CodegenContext {
        final CallableDescriptor receiverDescriptor;

        public ReceiverContext(
                CallableDescriptor contextDescriptor,
                OwnerKind contextKind,
                CodegenContext parentContext,
                @Nullable ObjectOrClosureCodegen closureCodegen
        ) {
            super(contextDescriptor, contextKind, parentContext, closureCodegen);
            receiverDescriptor = contextDescriptor.getReceiverParameter().exists() ? contextDescriptor : null;
        }

        @Override
        protected CallableDescriptor getReceiverDescriptor() {
            return receiverDescriptor;
        }
    }

    public static class MethodContext extends ReceiverContext {

        public MethodContext(@NotNull FunctionDescriptor contextType, OwnerKind contextKind, CodegenContext parentContext) {
            super(contextType instanceof PropertyAccessorDescriptor
                  ? ((PropertyAccessorDescriptor) contextType).getCorrespondingProperty()
                  : contextType, contextKind, parentContext, null);
        }

        @Override
        protected ClassDescriptor getThisDescriptor() {
            //noinspection ConstantConditions
            return getParentContext().getThisDescriptor();
        }

        @Override
        public StackValue lookupInContext(DeclarationDescriptor d, InstructionAdapter v, StackValue result) {
            //noinspection ConstantConditions
            return getParentContext().lookupInContext(d, v, result);
        }

        @Override
        public Type enclosingClassType(JetTypeMapper typeMapper) {
            //noinspection ConstantConditions
            return getParentContext().enclosingClassType(typeMapper);
        }

        @Override
        public boolean isStatic() {
            //noinspection ConstantConditions
            return getParentContext().isStatic();
        }

        @Override
        protected StackValue getOuterExpression(StackValue prefix) {
            //noinspection ConstantConditions
            return getParentContext().getOuterExpression(prefix);
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
                CodegenContext parent,
                JetTypeMapper typeMapper
        ) {
            super(contextDescriptor, kind, parent);

            final Type type = enclosingClassType(typeMapper);
            outerExpression = type != null
                              ? local1
                              : null;
        }

        @Override
        protected StackValue getOuterExpression(StackValue prefix) {
            return outerExpression;
        }

        @Override
        public String toString() {
            return "Constructor: " + getContextDescriptor().getName();
        }
    }

    public static class ScriptContext extends CodegenContext {

        @NotNull
        private final ClassDescriptor classDescriptor;
        @NotNull
        private final ScriptDescriptor scriptDescriptor;

        public ScriptContext(
                @NotNull ScriptDescriptor scriptDescriptor,
                @NotNull ClassDescriptor contextDescriptor,
                @NotNull OwnerKind contextKind,
                @Nullable CodegenContext parentContext,
                @Nullable ObjectOrClosureCodegen closureCodegen
        ) {
            super(contextDescriptor, contextKind, parentContext, closureCodegen);

            this.classDescriptor = contextDescriptor;
            this.scriptDescriptor = scriptDescriptor;
        }

        @Override
        protected ClassDescriptor getThisDescriptor() {
            return classDescriptor;
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
                ClassDescriptor contextDescriptor,
                OwnerKind contextKind,
                CodegenContext parentContext,
                JetTypeMapper typeMapper
        ) {
            super(contextDescriptor, contextKind, parentContext, null);

            final Type type = enclosingClassType(typeMapper);
            outerExpression = type != null
                              ? StackValue.field(type, typeMapper.getJvmClassName(contextDescriptor), "this$0", false)
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
        public AnonymousClassContext(
                ClassDescriptor contextDescriptor,
                OwnerKind contextKind,
                CodegenContext parentContext,
                @NotNull ObjectOrClosureCodegen closure,
                JetTypeMapper typeMapper
        ) {
            super(contextDescriptor, contextKind, parentContext, closure);

            final Type type = enclosingClassType(typeMapper);
            Type owner = closure.state.getInjector().getJetTypeMapper().mapType(contextDescriptor.getDefaultType(), MapTypeMode.IMPL);
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
        private final ClassDescriptor classDescriptor;

        public ClosureContext(
                FunctionDescriptor contextDescriptor,
                ClassDescriptor classDescriptor,
                CodegenContext parentContext,
                @NotNull ObjectOrClosureCodegen closureCodegen,
                JvmClassName internalClassName,
                JetTypeMapper typeMapper
        ) {
            super(contextDescriptor, OwnerKind.IMPLEMENTATION, parentContext, closureCodegen);
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
            super(contextDescriptor, kind != null ? kind : OwnerKind.NAMESPACE, parent, null);
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
