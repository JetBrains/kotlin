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

import jet.Function0;
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
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.storage.LockBasedStorageManager;
import org.jetbrains.jet.storage.NullableLazyValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.jetbrains.asm4.Opcodes.ACC_PRIVATE;
import static org.jetbrains.jet.codegen.AsmUtil.CAPTURED_THIS_FIELD;
import static org.jetbrains.jet.codegen.AsmUtil.getVisibilityAccessFlag;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.*;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;

public abstract class CodegenContext<T extends DeclarationDescriptor> {

    public static final CodegenContext STATIC = new RootContext();

    @NotNull
    private final T contextDescriptor;

    @NotNull
    private final OwnerKind contextKind;

    @Nullable
    private final CodegenContext parentContext;

    private final ClassDescriptor thisDescriptor;

    public final MutableClosure closure;

    private HashMap<DeclarationDescriptor, DeclarationDescriptor> accessors;

    private Map<DeclarationDescriptor, CodegenContext> childContexts;

    private NullableLazyValue<StackValue> lazyOuterExpression;

    private final LocalLookup enclosingLocalLookup;

    public CodegenContext(
            @NotNull T contextDescriptor,
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

        if (parentContext != null) {
            parentContext.addChild(this);
        }
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
            if (!(contextDescriptor instanceof ClassDescriptor) && !(contextDescriptor instanceof PackageFragmentDescriptor)) {
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
            CallableDescriptor callableDescriptor = (CallableDescriptor) getContextDescriptor();
            return callableDescriptor.getReceiverParameter() != null ? callableDescriptor : null;
        }
        return null;
    }

    public StackValue getOuterExpression(@Nullable StackValue prefix, boolean ignoreNoOuter) {
        return getOuterExpression(prefix, ignoreNoOuter, true);
    }

    private StackValue getOuterExpression(@Nullable StackValue prefix, boolean ignoreNoOuter, boolean captureThis) {
        if (lazyOuterExpression == null || lazyOuterExpression.invoke() == null) {
            if (ignoreNoOuter) {
                return null;
            }
            else {
                throw new UnsupportedOperationException();
            }
        }
        if (captureThis) {
            closure.setCaptureThis();
        }
        return prefix != null ? StackValue.composed(prefix, lazyOuterExpression.invoke()) : lazyOuterExpression.invoke();
    }

    @NotNull
    public T getContextDescriptor() {
        return contextDescriptor;
    }

    @NotNull
    public OwnerKind getContextKind() {
        return contextKind;
    }

    @NotNull
    public FieldOwnerContext intoPackagePart(@NotNull PackageFragmentDescriptor descriptor) {
        return new PackageContext(descriptor, this);
    }

    @NotNull
    public FieldOwnerContext intoPackageFacade(@NotNull Type delegateTo, @NotNull PackageFragmentDescriptor descriptor) {
        return new PackageFacadeContext(descriptor, this, delegateTo);
    }

    @NotNull
    public ClassContext intoClass(ClassDescriptor descriptor, OwnerKind kind, GenerationState state) {
        return new ClassContext(state.getTypeMapper(), descriptor, kind, this, null);
    }

    @NotNull
    public ClassContext intoAnonymousClass(
            @NotNull ClassDescriptor descriptor,
            @NotNull ExpressionCodegen expressionCodegen
    ) {
        JetTypeMapper typeMapper = expressionCodegen.getState().getTypeMapper();
        return new AnonymousClassContext(typeMapper, descriptor, OwnerKind.IMPLEMENTATION, this,
                                         expressionCodegen);
    }

    @NotNull
    public MethodContext intoFunction(FunctionDescriptor descriptor) {
        return new MethodContext(descriptor, getContextKind(), this);
    }

    @NotNull
    public ConstructorContext intoConstructor(@Nullable ConstructorDescriptor descriptor, @Nullable MutableClosure closure) {
        if (descriptor == null) {
            descriptor = new ConstructorDescriptorImpl(getThisDescriptor(), Collections.<AnnotationDescriptor>emptyList(), true)
                    .initialize(Collections.<TypeParameterDescriptor>emptyList(), Collections.<ValueParameterDescriptor>emptyList(),
                                Visibilities.PUBLIC);
        }
        return new ConstructorContext(descriptor, getContextKind(), this, closure);
    }

    @NotNull
    public ScriptContext intoScript(@NotNull ScriptDescriptor script, @NotNull ClassDescriptor classDescriptor) {
        return new ScriptContext(script, classDescriptor, OwnerKind.IMPLEMENTATION, this, closure);
    }

    @NotNull
    public CodegenContext intoClosure(
            @NotNull FunctionDescriptor funDescriptor,
            @NotNull LocalLookup localLookup,
            @NotNull JetTypeMapper typeMapper
    ) {
        ClassDescriptor classDescriptor = anonymousClassForFunction(typeMapper.getBindingContext(), funDescriptor);
        return new ClosureContext(typeMapper, funDescriptor, classDescriptor, this, localLookup);
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

    @NotNull
    public DeclarationDescriptor getAccessor(@NotNull DeclarationDescriptor descriptor) {
        return getAccessor(descriptor, false, null);
    }

    @NotNull
    public DeclarationDescriptor getAccessor(@NotNull DeclarationDescriptor descriptor, boolean isForBackingFieldInOuterClass, @Nullable JetType delegateType) {
        if (accessors == null) {
            accessors = new HashMap<DeclarationDescriptor, DeclarationDescriptor>();
        }
        descriptor = descriptor.getOriginal();
        DeclarationDescriptor accessor = accessors.get(descriptor);
        if (accessor != null) {
            assert !isForBackingFieldInOuterClass ||
                   accessor instanceof AccessorForPropertyBackingFieldInOuterClass : "There is already exists accessor with isForBackingFieldInOuterClass = false in this context";
            return accessor;
        }

        int accessorIndex = accessors.size();
        if (descriptor instanceof SimpleFunctionDescriptor || descriptor instanceof ConstructorDescriptor) {
            accessor = new AccessorForFunctionDescriptor((FunctionDescriptor) descriptor, contextDescriptor, accessorIndex);
        }
        else if (descriptor instanceof PropertyDescriptor) {
            if (isForBackingFieldInOuterClass) {
                accessor = new AccessorForPropertyBackingFieldInOuterClass((PropertyDescriptor) descriptor, contextDescriptor,
                                                                           accessorIndex, delegateType);
            } else {
                accessor = new AccessorForPropertyDescriptor((PropertyDescriptor) descriptor, contextDescriptor, accessorIndex);
            }
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

    protected void initOuterExpression(@NotNull final JetTypeMapper typeMapper, @NotNull final ClassDescriptor classDescriptor) {
        lazyOuterExpression = LockBasedStorageManager.NO_LOCKS.createNullableLazyValue(new Function0<StackValue>() {
            @Override
            public StackValue invoke() {
                ClassDescriptor enclosingClass = getEnclosingClass();
                return enclosingClass != null && canHaveOuter(typeMapper.getBindingContext(), classDescriptor)
                       ? StackValue.field(typeMapper.mapType(enclosingClass),
                                          CodegenBinding.getAsmType(typeMapper.getBindingTrace(), classDescriptor),
                                          CAPTURED_THIS_FIELD,
                                          false)
                       : null;
            }
        });
    }

    public StackValue lookupInContext(DeclarationDescriptor d, @Nullable StackValue result, GenerationState state, boolean ignoreNoOuter) {
        StackValue myOuter = null;
        if (closure != null) {
            EnclosedValueDescriptor answer = closure.getCaptureVariables().get(d);
            if (answer != null) {
                StackValue innerValue = answer.getInnerValue();
                return result == null ? innerValue : StackValue.composed(result, innerValue);
            }

            for (LocalLookup.LocalLookupCase aCase : LocalLookup.LocalLookupCase.values()) {
                if (aCase.isCase(d, state)) {
                    Type classType = state.getBindingContext().get(ASM_TYPE, getThisDescriptor());
                    StackValue innerValue = aCase.innerValue(d, enclosingLocalLookup, state, closure, classType);
                    if (innerValue == null) {
                        break;
                    }
                    else {
                        return result == null ? innerValue : StackValue.composed(result, innerValue);
                    }
                }
            }

            myOuter = getOuterExpression(null, ignoreNoOuter, false);
            result = result == null || myOuter == null ? myOuter : StackValue.composed(result, myOuter);
        }

        StackValue resultValue;
        if (myOuter != null && getEnclosingClass() == d) {
            resultValue = result;
        } else {
            resultValue = parentContext != null ? parentContext.lookupInContext(d, result, state, ignoreNoOuter) : null;
        }

        if (myOuter != null && resultValue != null) {
            closure.setCaptureThis();
        }
        return resultValue;
    }

    @NotNull
    public Map<DeclarationDescriptor, DeclarationDescriptor> getAccessors() {
        return accessors == null ? Collections.<DeclarationDescriptor, DeclarationDescriptor>emptyMap() : accessors;
    }

    @NotNull
    public PropertyDescriptor accessiblePropertyDescriptor(PropertyDescriptor propertyDescriptor) {
        return (PropertyDescriptor) accessibleDescriptorIfNeeded(propertyDescriptor, true);
    }

    @NotNull
    public FunctionDescriptor accessibleFunctionDescriptor(FunctionDescriptor fd) {
        return (FunctionDescriptor) accessibleDescriptorIfNeeded(fd, true);
    }

    @NotNull
    public void recordSyntheticAccessorIfNeeded(@NotNull FunctionDescriptor fd, @NotNull JetTypeMapper typeMapper) {
        if (fd instanceof ConstructorDescriptor || needSyntheticAccessorInBindingTrace(fd, typeMapper)) {
            accessibleDescriptorIfNeeded(fd, false);
        }
    }

    @NotNull
    public void recordSyntheticAccessorIfNeeded(PropertyDescriptor propertyDescriptor, JetTypeMapper typeMapper) {
        if (needSyntheticAccessorInBindingTrace(propertyDescriptor, typeMapper)) {
            accessibleDescriptorIfNeeded(propertyDescriptor, false);
        }
    }

    private boolean needSyntheticAccessorInBindingTrace(@NotNull CallableMemberDescriptor descriptor, @NotNull JetTypeMapper typeMapper) {
        Boolean result = typeMapper.getBindingContext().get(BindingContext.NEED_SYNTHETIC_ACCESSOR, descriptor);
        return result == null ? false : result.booleanValue();
    }

    @NotNull
    private int getAccessFlags(CallableMemberDescriptor descriptor) {
        int flag = getVisibilityAccessFlag(descriptor);
        if (descriptor instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;

            PropertySetterDescriptor setter = propertyDescriptor.getSetter();
            PropertyGetterDescriptor getter = propertyDescriptor.getGetter();

            flag |= (getter == null ? 0 : getVisibilityAccessFlag(getter)) |
                (setter == null ? 0 : getVisibilityAccessFlag(setter));
        }
        return flag;
    }

    @NotNull
    private MemberDescriptor accessibleDescriptorIfNeeded(CallableMemberDescriptor descriptor, boolean fromOutsideContext) {
        int flag = getAccessFlags(descriptor);
        if ((flag & ACC_PRIVATE) == 0) {
            return descriptor;
        }

        CodegenContext descriptorContext = null;
        if (!fromOutsideContext || getClassOrNamespaceDescriptor() != descriptor.getContainingDeclaration()) {
            DeclarationDescriptor enclosed = descriptor.getContainingDeclaration();
            boolean isClassObjectMember = DescriptorUtils.isClassObject(enclosed);
            //go upper
            if (hasThisDescriptor() && (enclosed != getThisDescriptor() || !fromOutsideContext)) {
                CodegenContext currentContext = this;
                while (currentContext != null) {
                    if (currentContext.getContextDescriptor() == enclosed) {
                        descriptorContext = currentContext;
                        break;
                    }

                    //accessors for private members in class object for call from class
                    if (isClassObjectMember && currentContext instanceof ClassContext) {
                        ClassContext classContext = (ClassContext) currentContext;
                        CodegenContext classObject = classContext.getClassObjectContext();
                        if (classObject != null && classObject.getContextDescriptor() == enclosed) {
                            descriptorContext = classObject;
                            break;
                        }
                    }

                    currentContext = currentContext.getParentContext();
                }
            }
        }

        return (MemberDescriptor) (descriptorContext != null ? descriptorContext.getAccessor(descriptor) : descriptor);
    }

    private void addChild(@NotNull CodegenContext child) {
        if (shouldAddChild(child)) {
            if (childContexts == null) {
                childContexts = new HashMap<DeclarationDescriptor, CodegenContext>();
            }
            DeclarationDescriptor childContextDescriptor = child.getContextDescriptor();
            childContexts.put(childContextDescriptor, child);
        }
    }

    protected boolean shouldAddChild(@NotNull CodegenContext child) {
        DeclarationDescriptor childContextDescriptor = child.contextDescriptor;
        if (childContextDescriptor instanceof ClassDescriptor) {
            ClassKind kind = ((ClassDescriptor) childContextDescriptor).getKind();
                return kind == ClassKind.CLASS_OBJECT;
        }
        return false;
    }

    @Nullable
    public CodegenContext findChildContext(@NotNull DeclarationDescriptor child) {
        return childContexts == null ? null : childContexts.get(child);
    }
}
