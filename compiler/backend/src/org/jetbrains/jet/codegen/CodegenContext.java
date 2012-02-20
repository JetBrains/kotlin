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
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;

/*
 * @author max
 * @author alex.tkachman
 */
public abstract class CodegenContext {
    public static final CodegenContext STATIC = new CodegenContext(null, OwnerKind.NAMESPACE, null, null) {
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

    private final DeclarationDescriptor contextType;

    private final OwnerKind contextKind;
    @Nullable
    private final CodegenContext parentContext;
    public  final ObjectOrClosureCodegen closure;
    
    HashMap<JetType,Integer> typeInfoConstants;
    HashMap<Integer,JetType> reverseTypeInfoConstants;
    int typeInfoConstantsCount;
    HashMap<DeclarationDescriptor, DeclarationDescriptor> accessors;

    protected StackValue outerExpression;

    protected boolean outerWasUsed ;

    public CodegenContext(DeclarationDescriptor contextType, OwnerKind contextKind, @Nullable CodegenContext parentContext, @Nullable ObjectOrClosureCodegen closureCodegen) {
        this.contextType = contextType;
        this.contextKind = contextKind;
        this.parentContext = parentContext;
        closure = closureCodegen;
    }

    protected abstract ClassDescriptor getThisDescriptor ();

    public DeclarationDescriptor getClassOrNamespaceDescriptor() {
        CodegenContext c = this;
        while(true) {
            DeclarationDescriptor contextDescriptor = c.getContextDescriptor();
            if(!(contextDescriptor instanceof ClassDescriptor) && !(contextDescriptor instanceof NamespaceDescriptor)) {
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
        if(outerExpression == null)
            throw new UnsupportedOperationException();

        outerWasUsed = true;
        return prefix != null ? StackValue.composed(prefix, outerExpression) : outerExpression;
    }

    public DeclarationDescriptor getContextDescriptor() {
        return contextType;
    }

    public String getNamespaceClassName() {
        DeclarationDescriptor descriptor = contextType;
        while(!(descriptor instanceof NamespaceDescriptor)) {
            descriptor = descriptor.getContainingDeclaration();
        }
        return NamespaceCodegen.getJVMClassName(DescriptorUtils.getFQName(descriptor), true);
    }

    public OwnerKind getContextKind() {
        return contextKind;
    }

    public CodegenContext intoNamespace(NamespaceDescriptor descriptor) {
        return new NamespaceContext(descriptor, this);
    }

    public CodegenContext intoClass(ClassDescriptor descriptor, OwnerKind kind, JetTypeMapper typeMapper) {
        return new ClassContext(descriptor, kind, this, typeMapper);
    }

    public CodegenContext intoAnonymousClass(@NotNull ObjectOrClosureCodegen closure, ClassDescriptor descriptor, OwnerKind kind, JetTypeMapper typeMapper) {
        return new AnonymousClassContext(descriptor, kind, this, closure, typeMapper);
    }

    public MethodContext intoFunction(FunctionDescriptor descriptor) {
        return new MethodContext(descriptor, getContextKind(), this);
    }

    public ConstructorContext intoConstructor(ConstructorDescriptor descriptor, JetTypeMapper typeMapper) {
        if(descriptor == null) {
            descriptor = new ConstructorDescriptorImpl(getThisDescriptor(), Collections.<AnnotationDescriptor>emptyList(), true)
                    .initialize(Collections.<TypeParameterDescriptor>emptyList(), Collections.<ValueParameterDescriptor>emptyList(), Modality.OPEN, Visibility.PUBLIC);
        }
        return new ConstructorContext(descriptor, getContextKind(), this, typeMapper);
    }

    public ClosureContext intoClosure(FunctionDescriptor funDescriptor, ClassDescriptor classDescriptor, String internalClassName, ClosureCodegen closureCodegen, JetTypeMapper typeMapper) {
        return new ClosureContext(funDescriptor, classDescriptor, this, closureCodegen, internalClassName, typeMapper);
    }

    public FrameMap prepareFrame(JetTypeMapper mapper) {
        FrameMap frameMap = new FrameMap();

        if (getContextKind() != OwnerKind.NAMESPACE) {
            frameMap.enterTemp();  // 0 slot for this
        }

        CallableDescriptor receiverDescriptor = getReceiverDescriptor();
        if (receiverDescriptor != null) {
            Type type = mapper.mapType(receiverDescriptor.getReceiverParameter().getType());
            frameMap.enterTemp(type.getSize());  // Next slot for fake this
        }

        return frameMap;
    }

    @Nullable
    public CodegenContext getParentContext() {
        return parentContext;
    }

    public Type jvmType(JetTypeMapper mapper) {
        if (contextType instanceof ClassDescriptor) {
            return mapper.mapType(((ClassDescriptor) contextType).getDefaultType(), contextKind);
        }
        else if (closure != null) {
            return Type.getObjectType(closure.name);
        }
        else {
            return parentContext != null ? parentContext.jvmType(mapper) : JetTypeMapper.TYPE_OBJECT;
        }
    }

    public StackValue lookupInContext(DeclarationDescriptor d, InstructionAdapter v, StackValue result) {
        final ObjectOrClosureCodegen top = closure;
        if (top != null) {
            final StackValue answer = top.lookupInContext(d, result);
            if (answer != null)
                return result == null ? answer : StackValue.composed(result, answer);

            StackValue outer = getOuterExpression(null);
            result = result == null ? outer : StackValue.composed(result, outer);
        }

        return parentContext != null ? parentContext.lookupInContext(d, v, result) : null;
    }

    public Type enclosingClassType(JetTypeMapper typeMapper) {
        CodegenContext cur = getParentContext();
        while(cur != null && !(cur.getContextDescriptor() instanceof ClassDescriptor))
            cur = cur.getParentContext();

        return cur == null ? null : typeMapper.mapType(((ClassDescriptor)cur.getContextDescriptor()).getDefaultType());
    }
    
    public int getTypeInfoConstantIndex(JetType type) {
        if(parentContext != STATIC)
            return parentContext.getTypeInfoConstantIndex(type);
        
        if(typeInfoConstants == null) {
            typeInfoConstants = new LinkedHashMap<JetType, Integer>();
            reverseTypeInfoConstants = new LinkedHashMap<Integer, JetType>();
        }

        Integer index = typeInfoConstants.get(type);
        if(index == null) {
            index = typeInfoConstantsCount++;
            typeInfoConstants.put(type, index);
            reverseTypeInfoConstants.put(index, type);
        }
        return index;
    }
    
    DeclarationDescriptor getAccessor(DeclarationDescriptor descriptor) {
        if(accessors == null) {
            accessors = new HashMap<DeclarationDescriptor,DeclarationDescriptor>();
        }
        descriptor = descriptor.getOriginal();
        DeclarationDescriptor accessor = accessors.get(descriptor);
        if(accessor != null)
            return accessor;

        if(descriptor instanceof NamedFunctionDescriptor) {
            NamedFunctionDescriptorImpl myAccessor = new NamedFunctionDescriptorImpl(contextType,
                    Collections.<AnnotationDescriptor>emptyList(),
                    descriptor.getName() + "$bridge$" + accessors.size(),
                    CallableMemberDescriptor.Kind.DECLARATION);
            FunctionDescriptor fd = (NamedFunctionDescriptor) descriptor;
            myAccessor.initialize(fd.getReceiverParameter().exists() ? fd.getReceiverParameter().getType() : null,
                                  fd.getExpectedThisObject(),
                                  fd.getTypeParameters(),
                                  fd.getValueParameters(),
                                  fd.getReturnType(),
                                  fd.getModality(),
                                  fd.getVisibility());
            accessor = myAccessor;
        }
        else if(descriptor instanceof PropertyDescriptor) {
            PropertyDescriptor pd = (PropertyDescriptor) descriptor;
            PropertyDescriptor myAccessor = new PropertyDescriptor(contextType,
                    Collections.<AnnotationDescriptor>emptyList(),
                    pd.getModality(),
                    pd.getVisibility(),
                    pd.isVar(),
                    pd.isObjectDeclaration(),
                    pd.getName()  + "$bridge$" + accessors.size(),
                    CallableMemberDescriptor.Kind.DECLARATION
            );
            JetType receiverType = pd.getReceiverParameter().exists() ? pd.getReceiverParameter().getType() : null;
            myAccessor.setType(pd.getOutType(), Collections.<TypeParameterDescriptor>emptyList(), pd.getExpectedThisObject(), receiverType);

            PropertyGetterDescriptor pgd = new PropertyGetterDescriptor(
                        myAccessor, Collections.<AnnotationDescriptor>emptyList(), myAccessor.getModality(),
                        myAccessor.getVisibility(),
                    false, false, CallableMemberDescriptor.Kind.DECLARATION);
            pgd.initialize(myAccessor.getOutType());
            
            PropertySetterDescriptor psd = new PropertySetterDescriptor(
                    myAccessor, Collections.<AnnotationDescriptor>emptyList(), myAccessor.getModality(),
                        myAccessor.getVisibility(),
                    false, false, CallableMemberDescriptor.Kind.DECLARATION);
            myAccessor.initialize(pgd, psd);
            accessor = myAccessor;
        }
        else {
            throw new UnsupportedOperationException();
        }
        accessors.put(descriptor, accessor);
        return accessor;
    }

    public StackValue getReceiverExpression(JetTypeMapper typeMapper) {
        assert getReceiverDescriptor() != null;
        Type asmType = typeMapper.mapType(getReceiverDescriptor().getReceiverParameter().getType());
        return getThisDescriptor() != null ? StackValue.local(1, asmType) : StackValue.local(0, asmType);
    }

    public abstract boolean isStatic();

    public void copyAccessors(HashMap<DeclarationDescriptor, DeclarationDescriptor> accessors) {
        if(accessors != null) {
            if(this.accessors == null) {
                this.accessors = new HashMap<DeclarationDescriptor,DeclarationDescriptor>();
            }
            this.accessors.putAll(accessors);
        }
    }

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

    public static class ClassContext extends CodegenContext {
        public ClassContext(ClassDescriptor contextType, OwnerKind contextKind, CodegenContext parentContext, JetTypeMapper typeMapper) {
            super(contextType, contextKind, parentContext, null);

            final Type type = enclosingClassType(typeMapper);
            outerExpression = type != null
                        ? StackValue.field(type, typeMapper.getFQName(contextType), "this$0", false)
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
            outerExpression = type != null
                        ? StackValue.field(type, closure.state.getTypeMapper().mapType(contextType.getDefaultType(), OwnerKind.IMPLEMENTATION).getInternalName(), "this$0", false)
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

        public ClosureContext(FunctionDescriptor contextType, ClassDescriptor classDescriptor, CodegenContext parentContext, @NotNull ObjectOrClosureCodegen closureCodegen, String internalClassName, JetTypeMapper typeMapper) {
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
