/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.context;

import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.*;
import org.jetbrains.kotlin.codegen.binding.MutableClosure;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.JetSuperExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.storage.NullableLazyValue;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.*;

import static org.jetbrains.kotlin.codegen.AsmUtil.getVisibilityAccessFlag;
import static org.jetbrains.kotlin.resolve.BindingContext.NEED_SYNTHETIC_ACCESSOR;
import static org.jetbrains.org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.jetbrains.org.objectweb.asm.Opcodes.ACC_PROTECTED;

public abstract class CodegenContext<T extends DeclarationDescriptor> {
    private final T contextDescriptor;
    private final OwnerKind contextKind;
    private final CodegenContext parentContext;
    private final ClassDescriptor thisDescriptor;
    public final MutableClosure closure;
    private final LocalLookup enclosingLocalLookup;
    private final NullableLazyValue<StackValue.Field> outerExpression;

    private Map<DeclarationDescriptor, CodegenContext> childContexts;
    private Map<AccessorKey, AccessorForCallableDescriptor<?>> accessors;

    private static class AccessorKey {
        public final DeclarationDescriptor descriptor;
        public final ClassDescriptor superCallLabelTarget;

        public AccessorKey(@NotNull DeclarationDescriptor descriptor, @Nullable ClassDescriptor superCallLabelTarget) {
            this.descriptor = descriptor;
            this.superCallLabelTarget = superCallLabelTarget;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof AccessorKey)) return false;
            AccessorKey other = (AccessorKey) obj;
            return descriptor.equals(other.descriptor) &&
                   (superCallLabelTarget == null
                    ? other.superCallLabelTarget == null
                    : superCallLabelTarget.equals(other.superCallLabelTarget));
        }

        @Override
        public int hashCode() {
            return 31 * descriptor.hashCode() + (superCallLabelTarget == null ? 0 : superCallLabelTarget.hashCode());
        }

        @Override
        public String toString() {
            return descriptor.toString();
        }
    }

    public CodegenContext(
            @NotNull T contextDescriptor,
            @NotNull OwnerKind contextKind,
            @Nullable CodegenContext parentContext,
            @Nullable MutableClosure closure,
            @Nullable ClassDescriptor thisDescriptor,
            @Nullable LocalLookup localLookup
    ) {
        this.contextDescriptor = contextDescriptor;
        this.contextKind = contextKind;
        this.parentContext = parentContext;
        this.closure = closure;
        this.thisDescriptor = thisDescriptor;
        this.enclosingLocalLookup = localLookup;
        this.outerExpression = LockBasedStorageManager.NO_LOCKS.createNullableLazyValue(new Function0<StackValue.Field>() {
            @Override
            public StackValue.Field invoke() {
                return computeOuterExpression();
            }
        });

        if (parentContext != null) {
            parentContext.addChild(this);
        }
    }

    @NotNull
    public GenerationState getState() {
        return parentContext.getState();
    }

    @NotNull
    public final ClassDescriptor getThisDescriptor() {
        if (thisDescriptor == null) {
            throw new UnsupportedOperationException("Context doesn't have a \"this\": " + this);
        }
        return thisDescriptor;
    }

    public final boolean hasThisDescriptor() {
        return thisDescriptor != null;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public CodegenContext<? extends ClassOrPackageFragmentDescriptor> getClassOrPackageParentContext() {
        CodegenContext<?> context = this;
        while (true) {
            if (context.getContextDescriptor() instanceof ClassOrPackageFragmentDescriptor) {
                return (CodegenContext) context;
            }
            context = context.getParentContext();
            assert context != null : "Context which is not top-level has no parent: " + this;
        }
    }

    /**
     * This method returns not null only if context descriptor corresponds to method or function which has receiver
     */
    @Nullable
    public final CallableDescriptor getCallableDescriptorWithReceiver() {
        if (contextDescriptor instanceof CallableDescriptor) {
            CallableDescriptor callableDescriptor = (CallableDescriptor) getContextDescriptor();
            return callableDescriptor.getExtensionReceiverParameter() != null ? callableDescriptor : null;
        }
        return null;
    }

    public StackValue getOuterExpression(@Nullable StackValue prefix, boolean ignoreNoOuter) {
        return getOuterExpression(prefix, ignoreNoOuter, true);
    }

    private StackValue getOuterExpression(@Nullable StackValue prefix, boolean ignoreNoOuter, boolean captureThis) {
        if (outerExpression.invoke() == null) {
            if (!ignoreNoOuter) {
                throw new UnsupportedOperationException("Don't know how to generate outer expression for " + getContextDescriptor());
            }
            return null;
        }
        if (captureThis) {
            if (closure == null) {
                throw new IllegalStateException("Can't capture this for context without closure: " + getContextDescriptor());
            }
            closure.setCaptureThis();
        }
        return StackValue.changeReceiverForFieldAndSharedVar(outerExpression.invoke(), prefix);
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
    public PackageContext intoPackagePart(@NotNull PackageFragmentDescriptor descriptor, Type packagePartType) {
        return new PackageContext(descriptor, this, packagePartType);
    }

    @NotNull
    public FieldOwnerContext intoPackageFacade(@NotNull Type delegateTo, @NotNull PackageFragmentDescriptor descriptor, @NotNull Type publicFacadeType) {
        return new PackageFacadeContext(descriptor, this, delegateTo, publicFacadeType);
    }

    @NotNull
    public FieldOwnerContext<PackageFragmentDescriptor> intoMultifileClassPart(
            @NotNull PackageFragmentDescriptor descriptor,
            @NotNull Type multifileClassType,
            @NotNull Type filePartType
    ) {
        return new MultifileClassPartContext(descriptor, this, multifileClassType, filePartType);
    }

    @NotNull
    public FieldOwnerContext<PackageFragmentDescriptor> intoMultifileClass(
            @NotNull PackageFragmentDescriptor descriptor,
            @NotNull Type multifileClassType,
            @NotNull Type filePartType
    ) {
        return new MultifileClassFacadeContext(descriptor, this, multifileClassType, filePartType);
    }

    @NotNull
    public ClassContext intoClass(ClassDescriptor descriptor, OwnerKind kind, GenerationState state) {
        return new ClassContext(state.getTypeMapper(), descriptor, kind, this, null);
    }

    @NotNull
    public ClassContext intoAnonymousClass(@NotNull ClassDescriptor descriptor, @NotNull ExpressionCodegen codegen, @NotNull OwnerKind ownerKind) {
        return new AnonymousClassContext(codegen.getState().getTypeMapper(), descriptor, ownerKind, this, codegen);
    }

    @NotNull
    public MethodContext intoFunction(FunctionDescriptor descriptor) {
        return new MethodContext(descriptor, getContextKind(), this, null, false);
    }

    @NotNull
    public MethodContext intoInlinedLambda(FunctionDescriptor descriptor) {
        return new MethodContext(descriptor, getContextKind(), this, null, true);
    }

    @NotNull
    public ConstructorContext intoConstructor(@NotNull ConstructorDescriptor descriptor) {
        return new ConstructorContext(descriptor, getContextKind(), this, closure);
    }

    // SCRIPT: generate into script, move to ScriptingUtil
    @NotNull
    public ScriptContext intoScript(
            @NotNull ScriptDescriptor script,
            @NotNull List<ScriptDescriptor> earlierScripts,
            @NotNull ClassDescriptor classDescriptor
    ) {
        return new ScriptContext(script, earlierScripts, classDescriptor, OwnerKind.IMPLEMENTATION, this, closure);
    }

    @NotNull
    public ClosureContext intoClosure(
            @NotNull FunctionDescriptor funDescriptor,
            @NotNull LocalLookup localLookup,
            @NotNull JetTypeMapper typeMapper
    ) {
        return new ClosureContext(typeMapper, funDescriptor, this, localLookup);
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
    public <D extends CallableMemberDescriptor> D getAccessor(@NotNull D descriptor, @Nullable JetSuperExpression superCallExpression) {
        return getAccessor(descriptor, false, null, superCallExpression);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <D extends CallableMemberDescriptor> D getAccessor(
            @NotNull D possiblySubstitutedDescriptor,
            boolean isForBackingFieldInOuterClass,
            @Nullable JetType delegateType,
            @Nullable JetSuperExpression superCallExpression
    ) {
        if (accessors == null) {
            accessors = new LinkedHashMap<AccessorKey, AccessorForCallableDescriptor<?>>();
        }

        D descriptor = (D) possiblySubstitutedDescriptor.getOriginal();
        AccessorKey key = new AccessorKey(
                descriptor, superCallExpression == null ? null : ExpressionCodegen.getSuperCallLabelTarget(this, superCallExpression)
        );

        AccessorForCallableDescriptor<?> accessor = accessors.get(key);
        if (accessor != null) {
            assert !isForBackingFieldInOuterClass ||
                   accessor instanceof AccessorForPropertyBackingFieldInOuterClass : "There is already exists accessor with isForBackingFieldInOuterClass = false in this context";
            return (D) accessor;
        }

        int accessorIndex = accessors.size();
        if (descriptor instanceof SimpleFunctionDescriptor) {
            accessor = new AccessorForFunctionDescriptor(
                    (FunctionDescriptor) descriptor, contextDescriptor, accessorIndex, superCallExpression
            );
        }
        else if (descriptor instanceof ConstructorDescriptor) {
            accessor = new AccessorForConstructorDescriptor((ConstructorDescriptor) descriptor, contextDescriptor, superCallExpression);
        }
        else if (descriptor instanceof PropertyDescriptor) {
            if (isForBackingFieldInOuterClass) {
                accessor = new AccessorForPropertyBackingFieldInOuterClass((PropertyDescriptor) descriptor, contextDescriptor,
                                                                           accessorIndex, delegateType);
            }
            else {
                accessor = new AccessorForPropertyDescriptor((PropertyDescriptor) descriptor, contextDescriptor,
                                                             accessorIndex, superCallExpression);
            }
        }
        else {
            throw new UnsupportedOperationException("Do not know how to create accessor for descriptor " + descriptor);
        }

        accessors.put(key, accessor);

        return (D) accessor;
    }

    @Nullable
    protected StackValue.Field computeOuterExpression() {
        return null;
    }

    public StackValue lookupInContext(DeclarationDescriptor d, @Nullable StackValue result, GenerationState state, boolean ignoreNoOuter) {
        StackValue myOuter = null;
        if (closure != null) {
            EnclosedValueDescriptor answer = closure.getCaptureVariables().get(d);
            if (answer != null) {
                return StackValue.changeReceiverForFieldAndSharedVar(answer.getInnerValue(), result);
            }

            for (LocalLookup.LocalLookupCase aCase : LocalLookup.LocalLookupCase.values()) {
                if (aCase.isCase(d)) {
                    Type classType = state.getTypeMapper().mapType(getThisDescriptor());
                    StackValue.StackValueWithSimpleReceiver innerValue = aCase.innerValue(d, enclosingLocalLookup, state, closure, classType);
                    if (innerValue == null) {
                        break;
                    }
                    else {
                        return StackValue.changeReceiverForFieldAndSharedVar(innerValue, result);
                    }
                }
            }

            myOuter = getOuterExpression(result, ignoreNoOuter, false);
            result = myOuter;
        }

        StackValue resultValue;
        if (myOuter != null && getEnclosingClass() == d) {
            resultValue = result;
        } else {
            resultValue = parentContext != null ? parentContext.lookupInContext(d, result, state, ignoreNoOuter) : null;
        }

        if (myOuter != null && resultValue != null && !isStaticField(resultValue)) {
            closure.setCaptureThis();
        }
        return resultValue;
    }

    @NotNull
    public Collection<? extends AccessorForCallableDescriptor<?>> getAccessors() {
        return accessors == null ? Collections.<AccessorForCallableDescriptor<CallableMemberDescriptor>>emptySet() : accessors.values();
    }

    @NotNull
    public <D extends CallableMemberDescriptor> D accessibleDescriptor(
            @NotNull D descriptor,
            @Nullable JetSuperExpression superCallExpression
    ) {
        DeclarationDescriptor enclosing = descriptor.getContainingDeclaration();
        if (!hasThisDescriptor() || enclosing == getThisDescriptor() ||
            enclosing == getClassOrPackageParentContext().getContextDescriptor()) {
            return descriptor;
        }

        return accessibleDescriptorIfNeeded(descriptor, superCallExpression);
    }

    public void recordSyntheticAccessorIfNeeded(@NotNull CallableMemberDescriptor descriptor, @NotNull BindingContext bindingContext) {
        if (hasThisDescriptor() && Boolean.TRUE.equals(bindingContext.get(NEED_SYNTHETIC_ACCESSOR, descriptor))) {
            // Not a super call because neither constructors nor private members can be targets of super calls
            accessibleDescriptorIfNeeded(descriptor, /* superCallExpression = */ null);
        }
    }

    private static int getAccessFlags(@NotNull CallableMemberDescriptor descriptor) {
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

    @SuppressWarnings("unchecked")
    @NotNull
    private <D extends CallableMemberDescriptor> D accessibleDescriptorIfNeeded(
            @NotNull D descriptor,
            @Nullable JetSuperExpression superCallExpression
    ) {
        CallableMemberDescriptor unwrappedDescriptor = DescriptorUtils.unwrapFakeOverride(descriptor);
        int flag = getAccessFlags(unwrappedDescriptor);
        if ((flag & ACC_PRIVATE) == 0 && (flag & ACC_PROTECTED) == 0) {
            return descriptor;
        }

        DeclarationDescriptor enclosed = descriptor.getContainingDeclaration();
        CodegenContext descriptorContext = findParentContextWithDescriptor(enclosed);
        if (descriptorContext == null && DescriptorUtils.isCompanionObject(enclosed)) {
            CodegenContext classContext = findParentContextWithDescriptor(enclosed.getContainingDeclaration());
            if (classContext instanceof ClassContext) {
                descriptorContext = ((ClassContext) classContext).getCompanionObjectContext();
            }
        }

        if (descriptorContext == null) {
            return descriptor;
        }

        if ((flag & ACC_PROTECTED) != 0) {
            PackageFragmentDescriptor unwrappedDescriptorPackage =
                    DescriptorUtils.getParentOfType(unwrappedDescriptor, PackageFragmentDescriptor.class, false);
            PackageFragmentDescriptor contextDescriptorPackage =
                    DescriptorUtils.getParentOfType(descriptorContext.getContextDescriptor(), PackageFragmentDescriptor.class, false);

            boolean inSamePackage = contextDescriptorPackage != null && unwrappedDescriptorPackage != null &&
                                    unwrappedDescriptorPackage.getFqName().equals(contextDescriptorPackage.getFqName());
            if (inSamePackage) {
                return descriptor;
            }
        }

        return (D) descriptorContext.getAccessor(descriptor, superCallExpression);
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
        return DescriptorUtils.isCompanionObject(child.contextDescriptor);
    }

    @Nullable
    public CodegenContext findChildContext(@NotNull DeclarationDescriptor child) {
        return childContexts == null ? null : childContexts.get(child);
    }

    private static boolean isStaticField(@NotNull StackValue value) {
        return value instanceof StackValue.Field && ((StackValue.Field) value).isStaticPut;
    }
}
