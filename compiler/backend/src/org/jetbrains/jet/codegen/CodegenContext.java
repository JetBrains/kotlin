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
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.Name;
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

    protected Type outerWasUsed ;

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
        if (outerExpression == null) { throw new UnsupportedOperationException(); }

        outerWasUsed = outerExpression.type;
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
        return NamespaceCodegen.getJVMClassNameForKotlinNs(DescriptorUtils.getFQName(descriptor).toSafe()).getInternalName();
    }

    public OwnerKind getContextKind() {
        return contextKind;
    }

    public CodegenContext intoNamespace(NamespaceDescriptor descriptor) {
        return new CodegenContexts.NamespaceContext(descriptor, this);
    }

    public CodegenContext intoClass(ClassDescriptor descriptor, OwnerKind kind, JetTypeMapper typeMapper) {
        return new CodegenContexts.ClassContext(descriptor, kind, this, typeMapper);
    }

    public CodegenContext intoAnonymousClass(@NotNull ObjectOrClosureCodegen closure, ClassDescriptor descriptor, OwnerKind kind, JetTypeMapper typeMapper) {
        return new CodegenContexts.AnonymousClassContext(descriptor, kind, this, closure, typeMapper);
    }

    public CodegenContexts.MethodContext intoFunction(FunctionDescriptor descriptor) {
        return new CodegenContexts.MethodContext(descriptor, getContextKind(), this);
    }

    public CodegenContexts.ConstructorContext intoConstructor(ConstructorDescriptor descriptor, JetTypeMapper typeMapper) {
        if(descriptor == null) {
            descriptor = new ConstructorDescriptorImpl(getThisDescriptor(), Collections.<AnnotationDescriptor>emptyList(), true)
                    .initialize(Collections.<TypeParameterDescriptor>emptyList(), Collections.<ValueParameterDescriptor>emptyList(), Visibilities.PUBLIC);
        }
        return new CodegenContexts.ConstructorContext(descriptor, getContextKind(), this, typeMapper);
    }

    public CodegenContext intoScript(ScriptDescriptor script) {
        return new CodegenContexts.ScriptContext(script, OwnerKind.IMPLEMENTATION, this, closure);
    }

    public CodegenContexts.ClosureContext intoClosure(FunctionDescriptor funDescriptor, ClassDescriptor classDescriptor, JvmClassName internalClassName, ClosureCodegen closureCodegen, JetTypeMapper typeMapper) {
        return new CodegenContexts.ClosureContext(funDescriptor, classDescriptor, this, closureCodegen, internalClassName, typeMapper);
    }

    public FrameMap prepareFrame(JetTypeMapper mapper) {
        FrameMap frameMap = new FrameMap();

        if (getContextKind() != OwnerKind.NAMESPACE) {
            frameMap.enterTemp();  // 0 slot for this
        }

        CallableDescriptor receiverDescriptor = getReceiverDescriptor();
        if (receiverDescriptor != null) {
            Type type = mapper.mapType(receiverDescriptor.getReceiverParameter().getType(), MapTypeMode.VALUE);
            frameMap.enterTemp(type.getSize());  // Next slot for fake this
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
            if (answer != null) { return result == null ? answer : StackValue.composed(result, answer); }

            StackValue outer = getOuterExpression(null);
            result = result == null ? outer : StackValue.composed(result, outer);
        }

        return parentContext != null ? parentContext.lookupInContext(d, v, result) : null;
    }

    public Type enclosingClassType(JetTypeMapper typeMapper) {
        CodegenContext cur = getParentContext();
        while (cur != null && !(cur.getContextDescriptor() instanceof ClassDescriptor)) { cur = cur.getParentContext(); }

        return cur == null ? null : typeMapper.mapType(((ClassDescriptor) cur.getContextDescriptor()).getDefaultType(), MapTypeMode.IMPL);
    }
    
    public int getTypeInfoConstantIndex(JetType type) {
        if (parentContext != CodegenContexts.STATIC) { return parentContext.getTypeInfoConstantIndex(type); }
        
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
        if (accessor != null) { return accessor; }

        if(descriptor instanceof SimpleFunctionDescriptor) {
            SimpleFunctionDescriptorImpl myAccessor = new SimpleFunctionDescriptorImpl(contextType,
                    Collections.<AnnotationDescriptor>emptyList(),
                    Name.identifier(descriptor.getName() + "$bridge$" + accessors.size()), // TODO: evil
                    CallableMemberDescriptor.Kind.DECLARATION);
            FunctionDescriptor fd = (SimpleFunctionDescriptor) descriptor;
            myAccessor.initialize(fd.getReceiverParameter().exists() ? fd.getReceiverParameter().getType() : null,
                                  fd.getExpectedThisObject(),
                                  fd.getTypeParameters(),
                                  fd.getValueParameters(),
                                  fd.getReturnType(),
                                  fd.getModality(),
                                  fd.getVisibility(),
                                  /*isInline = */ false);
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
                    Name.identifier(pd.getName()  + "$bridge$" + accessors.size()), // TODO: evil
                    CallableMemberDescriptor.Kind.DECLARATION
            );
            JetType receiverType = pd.getReceiverParameter().exists() ? pd.getReceiverParameter().getType() : null;
            myAccessor.setType(pd.getType(), Collections.<TypeParameterDescriptorImpl>emptyList(), pd.getExpectedThisObject(), receiverType);

            PropertyGetterDescriptor pgd = new PropertyGetterDescriptor(
                        myAccessor, Collections.<AnnotationDescriptor>emptyList(), myAccessor.getModality(),
                        myAccessor.getVisibility(),
                    false, false, CallableMemberDescriptor.Kind.DECLARATION);
            pgd.initialize(myAccessor.getType());
            
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
        Type asmType = typeMapper.mapType(getReceiverDescriptor().getReceiverParameter().getType(), MapTypeMode.VALUE);
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
}
