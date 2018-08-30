/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.context;

import kotlin.annotations.jvm.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.*;
import org.jetbrains.kotlin.codegen.binding.MutableClosure;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.load.java.JavaVisibilities;
import org.jetbrains.kotlin.load.java.sam.SamConstructorDescriptor;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.storage.NullableLazyValue;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.*;

import static org.jetbrains.kotlin.codegen.AsmUtil.getVisibilityAccessFlag;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isNonDefaultInterfaceMember;
import static org.jetbrains.kotlin.descriptors.annotations.AnnotationUtilKt.isEffectivelyInlineOnly;
import static org.jetbrains.kotlin.resolve.jvm.annotations.JvmAnnotationUtilKt.hasJvmDefaultAnnotation;
import static org.jetbrains.kotlin.resolve.jvm.annotations.JvmAnnotationUtilKt.isCallableMemberWithJvmDefaultAnnotation;
import static org.jetbrains.org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.jetbrains.org.objectweb.asm.Opcodes.ACC_PROTECTED;

public abstract class CodegenContext<T extends DeclarationDescriptor> {
    private final T contextDescriptor;
    private final OwnerKind contextKind;
    private final CodegenContext parentContext;
    private final ClassDescriptor thisDescriptor;
    @Nullable public final MutableClosure closure;
    private final LocalLookup enclosingLocalLookup;
    private final NullableLazyValue<StackValue.Field> outerExpression;

    private Map<DeclarationDescriptor, CodegenContext> childContexts;
    private Map<AccessorKey, AccessorForCallableDescriptor<?>> accessors;
    private Map<AccessorKey, AccessorForPropertyDescriptorFactory> propertyAccessorFactories;
    private AccessorForCompanionObjectInstanceFieldDescriptor accessorForCompanionObjectInstanceFieldDescriptor = null;

    private static class AccessorKey {
        public final DeclarationDescriptor descriptor;
        public final ClassDescriptor superCallLabelTarget;
        public final AccessorKind accessorKind;
        public AccessorKey(
                @NotNull DeclarationDescriptor descriptor,
                @Nullable ClassDescriptor superCallLabelTarget,
                @NotNull AccessorKind accessorKind
        ) {
            this.descriptor = descriptor;
            this.superCallLabelTarget = superCallLabelTarget;
            this.accessorKind = accessorKind;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof AccessorKey)) return false;
            AccessorKey other = (AccessorKey) obj;
            return descriptor.equals(other.descriptor) &&
                   accessorKind == other.accessorKind &&
                   (superCallLabelTarget == null ? other.superCallLabelTarget == null
                                                 : superCallLabelTarget.equals(other.superCallLabelTarget));
        }

        @Override
        public int hashCode() {
            return 31 * descriptor.hashCode() +
                   accessorKind.hashCode() +
                   (superCallLabelTarget == null ? 0 : superCallLabelTarget.hashCode());
        }

        @Override
        public String toString() {
            return descriptor.toString();
        }
    }

    private static class AccessorForPropertyDescriptorFactory {
        private final @NotNull PropertyDescriptor property;
        private final @NotNull DeclarationDescriptor containingDeclaration;
        private final @Nullable ClassDescriptor superCallTarget;
        private final @NotNull String nameSuffix;
        private final @NotNull AccessorKind accessorKind;

        private AccessorForPropertyDescriptor withSyntheticGetterAndSetter = null;
        private AccessorForPropertyDescriptor withSyntheticGetter = null;
        private AccessorForPropertyDescriptor withSyntheticSetter = null;

        public AccessorForPropertyDescriptorFactory(
                @NotNull PropertyDescriptor property,
                @NotNull DeclarationDescriptor containingDeclaration,
                @Nullable ClassDescriptor superCallTarget,
                @NotNull String nameSuffix,
                @NotNull AccessorKind accessorKind
        ) {
            this.property = property;
            this.containingDeclaration = containingDeclaration;
            this.superCallTarget = superCallTarget;
            this.nameSuffix = nameSuffix;
            this.accessorKind = accessorKind;
        }

        @SuppressWarnings("ConstantConditions")
        public PropertyDescriptor getOrCreateAccessorIfNeeded(boolean getterAccessorRequired, boolean setterAccessorRequired) {
            if (getterAccessorRequired && setterAccessorRequired) {
                return getOrCreateAccessorWithSyntheticGetterAndSetter();
            }
            else if (getterAccessorRequired && !setterAccessorRequired) {
                if (withSyntheticGetter == null) {
                    withSyntheticGetter = new AccessorForPropertyDescriptor(
                            property, containingDeclaration, superCallTarget, nameSuffix,
                            true, false, accessorKind);
                }
                return withSyntheticGetter;
            }
            else if (!getterAccessorRequired && setterAccessorRequired) {
                if (withSyntheticSetter == null) {
                    withSyntheticSetter = new AccessorForPropertyDescriptor(
                            property, containingDeclaration, superCallTarget, nameSuffix,
                            false, true, accessorKind);
                }
                return withSyntheticSetter;
            }
            else {
                return property;
            }
        }

        @NotNull
        public AccessorForPropertyDescriptor getOrCreateAccessorWithSyntheticGetterAndSetter() {
            if (withSyntheticGetterAndSetter == null) {
                withSyntheticGetterAndSetter = new AccessorForPropertyDescriptor(
                        property, containingDeclaration, superCallTarget, nameSuffix,
                        true, true, accessorKind);
            }
            return withSyntheticGetterAndSetter;
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
        this.outerExpression = LockBasedStorageManager.NO_LOCKS.createNullableLazyValue(this::computeOuterExpression);

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
                throw new UnsupportedOperationException("Don't know how to generate outer expression: " + this);
            }
            return null;
        }
        if (captureThis) {
            if (closure == null) {
                throw new IllegalStateException("Can't capture this for context without closure: " + this);
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
    public PackageContext intoPackagePart(@NotNull PackageFragmentDescriptor descriptor, Type packagePartType, @Nullable KtFile sourceFile) {
        return new PackageContext(descriptor, this, packagePartType, sourceFile);
    }

    @NotNull
    public MultifileClassPartContext intoMultifileClassPart(
            @NotNull PackageFragmentDescriptor descriptor,
            @NotNull Type multifileClassType,
            @NotNull Type filePartType,
            @NotNull KtFile sourceFile
    ) {
        return new MultifileClassPartContext(descriptor, this, multifileClassType, filePartType, sourceFile);
    }

    @NotNull
    public FieldOwnerContext<PackageFragmentDescriptor> intoMultifileClass(
            @NotNull PackageFragmentDescriptor descriptor,
            @NotNull Type multifileClassType,
            @NotNull Type filePartType
    ) {
        return new MultifileClassFacadeContext(descriptor, this, multifileClassType, filePartType);
    }

    public ClassContext intoDefaultImplsClass(ClassDescriptor descriptor, ClassContext interfaceContext, GenerationState state) {
        return new DefaultImplsClassContext(state.getTypeMapper(), descriptor, OwnerKind.DEFAULT_IMPLS, this, null, interfaceContext);
    }

    @NotNull
    public ClassContext intoClass(@NotNull ClassDescriptor descriptor, @NotNull OwnerKind kind, @NotNull GenerationState state) {
        if (shouldAddChild(descriptor)) {
            CodegenContext savedContext = this.findChildContext(descriptor);
            if (savedContext != null) {
                assert savedContext.getContextKind() == kind : "Kinds should be same, but: " +
                                                                   savedContext.getContextKind() + "!= " + kind;
                return (ClassContext) savedContext;
            }
        }

        ClassContext classContext = new ClassContext(state.getTypeMapper(), descriptor, kind, this, null);

        if (descriptor.getCompanionObjectDescriptor() != null) {
            //We need to create companion object context ahead of time
            // because otherwise we can't generate synthetic accessor for private members in companion object
            classContext.intoClass(descriptor.getCompanionObjectDescriptor(), OwnerKind.IMPLEMENTATION, state);
        }
        return classContext;
    }

    @NotNull
    public ClassContext intoAnonymousClass(@NotNull ClassDescriptor descriptor, @NotNull ExpressionCodegen codegen, @NotNull OwnerKind ownerKind) {
        return new AnonymousClassContext(codegen.getState().getTypeMapper(), descriptor, ownerKind, this, codegen);
    }

    @NotNull
    public MethodContext intoFunction(FunctionDescriptor descriptor, boolean isDefaultFunctionContext) {
        return new MethodContext(descriptor, getContextKind(), this, null, isDefaultFunctionContext);
    }

    @NotNull
    public MethodContext intoFunction(FunctionDescriptor descriptor) {
        return intoFunction(descriptor, false);
    }

    @NotNull
    public MethodContext intoInlinedLambda(FunctionDescriptor descriptor, boolean isCrossInline, boolean isPropertyReference) {
        return new InlineLambdaContext(descriptor, getContextKind(), this, null, isCrossInline, isPropertyReference);
    }

    @NotNull
    public ConstructorContext intoConstructor(@NotNull ConstructorDescriptor descriptor) {
        return new ConstructorContext(descriptor, getContextKind(), this, closure);
    }

    @NotNull
    public ScriptContext intoScript(
            @NotNull ScriptDescriptor script,
            @NotNull List<ScriptDescriptor> earlierScripts,
            @NotNull ClassDescriptor classDescriptor,
            @NotNull KotlinTypeMapper typeMapper
    ) {
        return new ScriptContext(typeMapper, script, earlierScripts, classDescriptor, this);
    }

    @NotNull
    public ClosureContext intoClosure(
            @NotNull FunctionDescriptor funDescriptor,
            @NotNull LocalLookup localLookup,
            @NotNull KotlinTypeMapper typeMapper
    ) {
        return new ClosureContext(typeMapper, funDescriptor, this, localLookup);
    }

    @NotNull
    public ClosureContext intoCoroutineClosure(
            // copy of lambda descriptor that has an additional value parameter Continuation<T>
            @NotNull FunctionDescriptor jvmViewOfSuspendLambda,
            // original coroutine lambda descriptor
            @NotNull FunctionDescriptor originalSuspendLambdaDescriptor,
            @NotNull LocalLookup localLookup,
            @NotNull KotlinTypeMapper typeMapper
    ) {
        return new ClosureContext(typeMapper, jvmViewOfSuspendLambda, this, localLookup, originalSuspendLambdaDescriptor);
    }

    public ClassContext intoWrapperForErasedInlineClass(ClassDescriptor descriptor, GenerationState state) {
        return new ClassContext(state.getTypeMapper(), descriptor, OwnerKind.ERASED_INLINE_CLASS, this, null);
    }

    @Nullable
    public CodegenContext getParentContext() {
        return parentContext;
    }

    public boolean isContextWithUninitializedThis() {
        return false;
    }

    @Nullable
    public CodegenContext getEnclosingClassContext() {
        CodegenContext cur = getEnclosingThisContext();
        while (cur != null) {
            DeclarationDescriptor curDescriptor = cur.getContextDescriptor();
            if (curDescriptor instanceof ClassDescriptor) {
                return cur;
            }
            cur = cur.getParentContext();
        }
        return null;
    }

    @Nullable
    public CodegenContext getEnclosingThisContext() {
        CodegenContext cur = getParentContext();
        while (cur != null && cur.isContextWithUninitializedThis()) {
            CodegenContext parent = cur.getParentContext();
            assert parent != null : "Context " + cur + " should have a parent";
            cur = parent.getParentContext();
        }
        return cur;
    }

    @Nullable
    public ClassDescriptor getEnclosingClass() {
        // TODO store enclosing context class in the context itself
        CodegenContext enclosingClassContext = getEnclosingClassContext();
        if (enclosingClassContext == null) return null;
        return (ClassDescriptor) enclosingClassContext.getContextDescriptor();
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
    private PropertyDescriptor getPropertyAccessor(
            @NotNull PropertyDescriptor propertyDescriptor,
            @Nullable ClassDescriptor superCallTarget,
            boolean getterAccessorRequired,
            boolean setterAccessorRequired
    ) {
        return getAccessor(propertyDescriptor, AccessorKind.NORMAL, null, superCallTarget, getterAccessorRequired, setterAccessorRequired);
    }


    public  <D extends CallableMemberDescriptor> D getAccessorForJvmDefaultCompatibility(@NotNull D descriptor) {
        if (descriptor instanceof PropertyAccessorDescriptor) {
            PropertyDescriptor propertyAccessor = getAccessor(((PropertyAccessorDescriptor) descriptor).getCorrespondingProperty(),
                                                              AccessorKind.JVM_DEFAULT_COMPATIBILITY, null, null,
                                                              descriptor instanceof PropertyGetterDescriptor,
                                                              descriptor instanceof PropertySetterDescriptor);
            return descriptor instanceof PropertyGetterDescriptor ? (D) propertyAccessor.getGetter() : (D) propertyAccessor.getSetter();
        }
        return getAccessor(descriptor, AccessorKind.JVM_DEFAULT_COMPATIBILITY, null, null);
    }

    @NotNull
    private <D extends CallableMemberDescriptor> D getAccessor(@NotNull D descriptor, @Nullable ClassDescriptor superCallTarget) {
        return getAccessor(descriptor, AccessorKind.NORMAL, null, superCallTarget);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <D extends CallableMemberDescriptor> D getAccessorForSuperCallIfNeeded(
            @NotNull D descriptor,
            @Nullable ClassDescriptor superCallTarget,
            @NotNull GenerationState state) {
        if (superCallTarget != null && !isNonDefaultInterfaceMember(descriptor)) {
            CodegenContext afterInline = getFirstCrossInlineOrNonInlineContext();
            CodegenContext c = afterInline.findParentContextWithDescriptor(superCallTarget);
            assert c != null : "Couldn't find a context for a super-call: " + descriptor;
            if (c != afterInline.getParentContext()) {
                return (D) c.getAccessor(descriptor, superCallTarget);
            }
        }
        return descriptor;
    }

    @NotNull
    public <D extends CallableMemberDescriptor> D getAccessor(
            @NotNull D possiblySubstitutedDescriptor,
            @NotNull AccessorKind accessorKind,
            @Nullable KotlinType delegateType,
            @Nullable ClassDescriptor superCallTarget
    ) {
        // TODO this corresponds to default behavior for properties before fixing KT-9717. Is it Ok in general case?
        // Does not matter for other descriptor kinds.
        return getAccessor(possiblySubstitutedDescriptor, accessorKind, delegateType, superCallTarget,
                           /* getterAccessorRequired */ true,
                           /* setterAccessorRequired */ true);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private <D extends CallableMemberDescriptor> D getAccessor(
            @NotNull D possiblySubstitutedDescriptor,
            @NotNull AccessorKind accessorKind,
            @Nullable KotlinType delegateType,
            @Nullable ClassDescriptor superCallTarget,
            boolean getterAccessorRequired,
            boolean setterAccessorRequired
    ) {
        if (accessors == null) {
            accessors = new LinkedHashMap<>();
        }
        if (propertyAccessorFactories == null) {
            propertyAccessorFactories = new LinkedHashMap<>();
        }

        D descriptor = (D) possiblySubstitutedDescriptor.getOriginal();
        AccessorKey key = new AccessorKey(descriptor, superCallTarget, accessorKind);

        // NB should check for property accessor factory first (or change property accessor tracking under propertyAccessorFactory creation)
        if (propertyAccessorFactories.containsKey(key)) {
            return (D) propertyAccessorFactories.get(key).getOrCreateAccessorIfNeeded(getterAccessorRequired, setterAccessorRequired);
        }

        if (accessors.containsKey(key)) {
            AccessorForCallableDescriptor<?> accessor = accessors.get(key);
            assert accessorKind == AccessorKind.NORMAL ||
                   accessor instanceof AccessorForPropertyBackingField : "There is already exists accessor with isForBackingField = false in this context";
            return (D) accessor;
        }

        String nameSuffix = SyntheticAccessorUtilKt.getAccessorNameSuffix(descriptor, key.superCallLabelTarget, accessorKind);
        AccessorForCallableDescriptor<?> accessor;
        if (descriptor instanceof SimpleFunctionDescriptor) {
            accessor = new AccessorForFunctionDescriptor((FunctionDescriptor) descriptor, contextDescriptor, superCallTarget, nameSuffix, accessorKind);
        }
        else if (descriptor instanceof ClassConstructorDescriptor) {
            accessor = new AccessorForConstructorDescriptor((ClassConstructorDescriptor) descriptor, contextDescriptor, superCallTarget, accessorKind);
        }
        else if (descriptor instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
            if (accessorKind == AccessorKind.NORMAL || accessorKind == AccessorKind.JVM_DEFAULT_COMPATIBILITY) {
                AccessorForPropertyDescriptorFactory factory =
                        new AccessorForPropertyDescriptorFactory(propertyDescriptor, contextDescriptor, superCallTarget, nameSuffix, accessorKind);
                propertyAccessorFactories.put(key, factory);

                // Record worst case accessor for accessor methods generation.
                accessors.put(key, factory.getOrCreateAccessorWithSyntheticGetterAndSetter());

                return (D) factory.getOrCreateAccessorIfNeeded(getterAccessorRequired, setterAccessorRequired);
            }

            accessor = new AccessorForPropertyBackingField(
                    propertyDescriptor, contextDescriptor, delegateType,
                    accessorKind == AccessorKind.IN_CLASS_COMPANION ? null : propertyDescriptor.getExtensionReceiverParameter(),
                    accessorKind == AccessorKind.IN_CLASS_COMPANION ? null : propertyDescriptor.getDispatchReceiverParameter(),
                    nameSuffix, accessorKind
            );
        }
        else {
            throw new UnsupportedOperationException("Do not know how to create accessor for descriptor " + descriptor +
                                                    " in context " + this);
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
            EnclosedValueDescriptor capturedVariable = closure.getCaptureVariables().get(d);
            if (capturedVariable != null) {
                return StackValue.changeReceiverForFieldAndSharedVar(capturedVariable.getInnerValue(), result);
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
        }
        else {
            CodegenContext enclosingClassContext = getEnclosingThisContext();
            resultValue = enclosingClassContext != null ? enclosingClassContext.lookupInContext(d, result, state, ignoreNoOuter) : null;
        }

        if (myOuter != null && resultValue != null && !isStaticField(resultValue)) {
            closure.setCaptureThis();
        }
        return resultValue;
    }

    @NotNull
    @ReadOnly
    public Collection<? extends AccessorForCallableDescriptor<?>> getAccessors() {
        return accessors == null ? Collections.<AccessorForCallableDescriptor<CallableMemberDescriptor>>emptySet() : accessors.values();
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <D extends CallableMemberDescriptor> D accessibleDescriptor(
            @NotNull D descriptor,
            @Nullable ClassDescriptor superCallTarget
    ) {
        CodegenContext properContext = getFirstCrossInlineOrNonInlineContext();
        DeclarationDescriptor enclosing = descriptor.getContainingDeclaration();
        boolean isInliningContext = properContext.isInlineMethodContext();
        boolean sameJvmDefault = hasJvmDefaultAnnotation(descriptor) ==
                                 isCallableMemberWithJvmDefaultAnnotation(properContext.contextDescriptor) ||
                                 properContext.contextDescriptor instanceof AccessorForCallableDescriptor;
        if (!isInliningContext && (
                !properContext.hasThisDescriptor() ||
                ((enclosing == properContext.getThisDescriptor()) && sameJvmDefault) ||
                ((enclosing == properContext.getClassOrPackageParentContext().getContextDescriptor()) && sameJvmDefault))) {
            return descriptor;
        }
        return (D) properContext.accessibleDescriptorIfNeeded(descriptor, superCallTarget, isInliningContext);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private <D extends CallableMemberDescriptor> D accessibleDescriptorIfNeeded(
            @NotNull D descriptor,
            @Nullable ClassDescriptor superCallTarget,
            boolean withinInliningContext
    ) {
        CallableMemberDescriptor unwrappedDescriptor = DescriptorUtils.unwrapFakeOverride(descriptor);

        DeclarationDescriptor enclosed = descriptor.getContainingDeclaration();
        CodegenContext descriptorContext = findParentContextWithDescriptor(enclosed);
        if (descriptorContext == null && DescriptorUtils.isCompanionObject(enclosed)) {
            CodegenContext classContext = findParentContextWithDescriptor(enclosed.getContainingDeclaration());
            if (classContext instanceof ClassContext) {
                descriptorContext = ((ClassContext) classContext).getCompanionObjectContext();
            }
        }

        if (descriptorContext == null &&
            JavaVisibilities.PROTECTED_STATIC_VISIBILITY == descriptor.getVisibility() &&
            (!(descriptor.getOriginal() instanceof SamConstructorDescriptor))) {
            //seems we need static receiver in resolved call
            descriptorContext = ExpressionCodegen.getParentContextSubclassOf((ClassDescriptor) enclosed, this);
            superCallTarget = (ClassDescriptor) enclosed;
        }

        if (descriptorContext == null && withinInliningContext && superCallTarget != null) {
            //generate super calls within inline function through synthetic accessors
            descriptorContext = ExpressionCodegen.getParentContextSubclassOf((ClassDescriptor) enclosed, this);
        }

        if (descriptorContext == null && descriptor instanceof ClassConstructorDescriptor) {
            ClassDescriptor classDescriptor = ((ClassConstructorDescriptor) descriptor).getContainingDeclaration();
            if (DescriptorUtils.isSealedClass(classDescriptor)) {
                CodegenContext parentContextForClass = findParentContextWithDescriptor(classDescriptor.getContainingDeclaration());
                if (parentContextForClass != null) {
                    //generate super constructor calls for top-level sealed classes from top level child
                    descriptorContext = parentContextForClass.findChildContext(classDescriptor);
                }
            }
        }

        if (descriptorContext == null) {
            return descriptor;
        }

        if (hasJvmDefaultAnnotation(descriptor) && descriptorContext instanceof DefaultImplsClassContext) {
            descriptorContext = ((DefaultImplsClassContext) descriptorContext).getInterfaceContext();
        }

        if (descriptor instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
            int propertyAccessFlag = getVisibilityAccessFlag(descriptor);

            PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
            int getterAccessFlag = getter == null ? propertyAccessFlag
                                                  : propertyAccessFlag | getVisibilityAccessFlag(getter);
            boolean getterAccessorRequired = isAccessorRequired(getterAccessFlag, unwrappedDescriptor, descriptorContext,
                                                                withinInliningContext, superCallTarget != null);

            PropertySetterDescriptor setter = propertyDescriptor.getSetter();

            int setterAccessFlag = propertyAccessFlag;
            if (setter != null && setter.getVisibility().normalize() != Visibilities.INVISIBLE_FAKE) {
                setterAccessFlag = propertyAccessFlag | getVisibilityAccessFlag(setter);
            }
            boolean setterAccessorRequired = isAccessorRequired(setterAccessFlag, unwrappedDescriptor, descriptorContext,
                                                                withinInliningContext, superCallTarget != null);

            if (!getterAccessorRequired && !setterAccessorRequired) {
                return descriptor;
            }
            return (D) descriptorContext.getPropertyAccessor(propertyDescriptor, superCallTarget, getterAccessorRequired, setterAccessorRequired);
        }
        else {
            int flag = getVisibilityAccessFlag(unwrappedDescriptor);
            if (!isAccessorRequired(flag, unwrappedDescriptor, descriptorContext, withinInliningContext, superCallTarget != null)) {
                return descriptor;
            }
            return (D) descriptorContext.getAccessor(descriptor, superCallTarget);
        }
    }

    private static boolean isAccessorRequired(
            int accessFlag,
            @NotNull CallableMemberDescriptor unwrappedDescriptor,
            @NotNull CodegenContext descriptorContext,
            boolean withinInline,
            boolean isSuperCall
    ) {
        if (isEffectivelyInlineOnly(unwrappedDescriptor)) return false;

        return isSuperCall && withinInline ||
               (accessFlag & ACC_PRIVATE) != 0 ||
               ((accessFlag & ACC_PROTECTED) != 0 &&
                (withinInline || !isInSamePackage(unwrappedDescriptor, descriptorContext.getContextDescriptor())));
    }

    private static boolean isInSamePackage(DeclarationDescriptor descriptor1, DeclarationDescriptor descriptor2) {
        PackageFragmentDescriptor package1 =
                DescriptorUtils.getParentOfType(descriptor1, PackageFragmentDescriptor.class, false);
        PackageFragmentDescriptor package2 =
                DescriptorUtils.getParentOfType(descriptor2, PackageFragmentDescriptor.class, false);

        return package2 != null && package1 != null &&
               package1.getFqName().equals(package2.getFqName());
    }

    private void addChild(@NotNull CodegenContext child) {
        if (shouldAddChild(child.contextDescriptor)) {
            if (childContexts == null) {
                childContexts = new HashMap<>();
            }
            DeclarationDescriptor childContextDescriptor = child.getContextDescriptor();
            childContexts.put(childContextDescriptor, child);
        }
    }

    private static boolean shouldAddChild(@NotNull DeclarationDescriptor childContextDescriptor) {
        return DescriptorUtils.isCompanionObject(childContextDescriptor) || DescriptorUtils.isSealedClass(childContextDescriptor);
    }

    @Nullable
    protected CodegenContext findChildContext(@NotNull DeclarationDescriptor child) {
        return childContexts == null ? null : childContexts.get(child);
    }

    private static boolean isStaticField(@NotNull StackValue value) {
        return value instanceof StackValue.Field && ((StackValue.Field) value).isStaticPut;
    }

    public boolean isInlineMethodContext() {
        return false;
    }

    @NotNull
    public CodegenContext getFirstCrossInlineOrNonInlineContext() {
        return this;
    }

    @Nullable
    public LocalLookup getEnclosingLocalLookup() {
        return enclosingLocalLookup;
    }

    @NotNull
    public AccessorForCompanionObjectInstanceFieldDescriptor markCompanionObjectDescriptorWithAccessorRequired(@NotNull ClassDescriptor companionObjectDescriptor) {
        assert DescriptorUtils.isCompanionObject(companionObjectDescriptor) : "Companion object expected: " + companionObjectDescriptor;

        assert accessorForCompanionObjectInstanceFieldDescriptor == null
               || accessorForCompanionObjectInstanceFieldDescriptor.getCompanionObjectDescriptor() == companionObjectDescriptor
                : "Unexpected companion object descriptor with accessor required: " + companionObjectDescriptor +
                  "; should be " + accessorForCompanionObjectInstanceFieldDescriptor.getCompanionObjectDescriptor();

        if (accessorForCompanionObjectInstanceFieldDescriptor == null) {
            accessorForCompanionObjectInstanceFieldDescriptor =
                    new AccessorForCompanionObjectInstanceFieldDescriptor(
                            companionObjectDescriptor,
                            Name.identifier(JvmCodegenUtil.getCompanionObjectAccessorName(companionObjectDescriptor))
                    );
        }

        return accessorForCompanionObjectInstanceFieldDescriptor;
    }

    @Nullable
    public AccessorForCompanionObjectInstanceFieldDescriptor getAccessorForCompanionObjectDescriptorIfRequired() {
        return accessorForCompanionObjectInstanceFieldDescriptor;
    }

}
