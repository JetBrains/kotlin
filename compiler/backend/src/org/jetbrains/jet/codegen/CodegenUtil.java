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

package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.binding.CalculatedClosure;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.PackageContext;
import org.jetbrains.jet.codegen.signature.BothSignatureWriter;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterKind;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.CallResolverUtil;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

import static org.jetbrains.jet.lang.descriptors.Modality.ABSTRACT;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;

public class CodegenUtil {

    private CodegenUtil() {
    }

    public static boolean isInterface(DeclarationDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            ClassKind kind = ((ClassDescriptor) descriptor).getKind();
            return kind == ClassKind.TRAIT || kind == ClassKind.ANNOTATION_CLASS;
        }
        return false;
    }

    public static boolean isInterface(JetType type) {
        return isInterface(type.getConstructor().getDeclarationDescriptor());
    }

    public static SimpleFunctionDescriptor createInvoke(FunctionDescriptor fd) {
        int arity = fd.getValueParameters().size();
        SimpleFunctionDescriptorImpl invokeDescriptor = new SimpleFunctionDescriptorImpl(
                fd.getExpectedThisObject() != null
                ? KotlinBuiltIns.getInstance().getExtensionFunction(arity) : KotlinBuiltIns.getInstance().getFunction(arity),
                Collections.<AnnotationDescriptor>emptyList(),
                Name.identifier("invoke"),
                CallableMemberDescriptor.Kind.DECLARATION);

        invokeDescriptor.initialize(DescriptorUtils.getReceiverParameterType(fd.getReceiverParameter()),
                                    fd.getExpectedThisObject(),
                                    Collections.<TypeParameterDescriptorImpl>emptyList(),
                                    fd.getValueParameters(),
                                    fd.getReturnType(),
                                    Modality.FINAL,
                                    Visibilities.PUBLIC
        );
        return invokeDescriptor;
    }

    public static JvmMethodSignature erasedInvokeSignature(FunctionDescriptor fd) {
        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, false);

        boolean isExtensionFunction = fd.getReceiverParameter() != null;
        int paramCount = fd.getValueParameters().size();
        if (isExtensionFunction) {
            paramCount++;
        }

        signatureWriter.writeParametersStart();

        for (int i = 0; i < paramCount; ++i) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.VALUE);
            signatureWriter.writeAsmType(OBJECT_TYPE);
            signatureWriter.writeParameterTypeEnd();
        }

        signatureWriter.writeReturnType();
        signatureWriter.writeAsmType(OBJECT_TYPE);
        signatureWriter.writeReturnTypeEnd();

        return signatureWriter.makeJvmMethodSignature("invoke");
    }

    public static boolean isConst(CalculatedClosure closure) {
        return closure.getCaptureThis() == null && closure.getCaptureReceiverType() == null && closure.getCaptureVariables().isEmpty();
    }

    public static <T> T peekFromStack(Stack<T> stack) {
        return stack.empty() ? null : stack.peek();
    }

    public static JetType getSuperClass(ClassDescriptor classDescriptor) {
        List<ClassDescriptor> superclassDescriptors = DescriptorUtils.getSuperclassDescriptors(classDescriptor);
        for (ClassDescriptor descriptor : superclassDescriptors) {
            if (descriptor.getKind() != ClassKind.TRAIT) {
                return descriptor.getDefaultType();
            }
        }
        return KotlinBuiltIns.getInstance().getAnyType();
    }

    @NotNull
    public static <T extends CallableMemberDescriptor> T unwrapFakeOverride(T member) {
        while (member.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            //noinspection unchecked
            member = (T) member.getOverriddenDescriptors().iterator().next();
        }
        return member;
    }

    @Nullable
    public static FunctionDescriptor getDeclaredFunctionByRawSignature(
            @NotNull ClassDescriptor owner,
            @NotNull Name name,
            @NotNull ClassifierDescriptor returnedClassifier,
            @NotNull ClassifierDescriptor... valueParameterClassifiers
    ) {
        Collection<FunctionDescriptor> functions = owner.getDefaultType().getMemberScope().getFunctions(name);
        for (FunctionDescriptor function : functions) {
            if (!CallResolverUtil.isOrOverridesSynthesized(function)
                && function.getTypeParameters().isEmpty()
                && valueParameterClassesMatch(function.getValueParameters(), Arrays.asList(valueParameterClassifiers))
                && rawTypeMatches(function.getReturnType(), returnedClassifier)) {
                return function;
            }
        }
        return null;
    }

    private static boolean valueParameterClassesMatch(
            @NotNull List<ValueParameterDescriptor> parameters,
            @NotNull List<ClassifierDescriptor> classifiers) {
        if (parameters.size() != classifiers.size()) return false;
        for (int i = 0; i < parameters.size(); i++) {
            ValueParameterDescriptor parameterDescriptor = parameters.get(i);
            ClassifierDescriptor classDescriptor = classifiers.get(i);
            if (!rawTypeMatches(parameterDescriptor.getType(), classDescriptor)) {
                return false;
            }
        }
        return true;
    }

    private static boolean rawTypeMatches(JetType type, ClassifierDescriptor classifier) {
        return type.getConstructor().getDeclarationDescriptor().getOriginal() == classifier.getOriginal();
    }

    public static boolean isCallInsideSameClassAsDeclared(CallableMemberDescriptor declarationDescriptor, CodegenContext context) {
        boolean isFakeOverride = declarationDescriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE;
        boolean isDelegate = declarationDescriptor.getKind() == CallableMemberDescriptor.Kind.DELEGATION;

        DeclarationDescriptor containingDeclaration = declarationDescriptor.getContainingDeclaration();
        containingDeclaration = containingDeclaration.getOriginal();

        return !isFakeOverride && !isDelegate &&
               (((context.hasThisDescriptor() && containingDeclaration == context.getThisDescriptor()) ||
                 (context.getParentContext() instanceof PackageContext && context.getParentContext().getContextDescriptor() == containingDeclaration))
                && context.getContextKind() != OwnerKind.TRAIT_IMPL);
    }

    public static boolean isCallInsideSameModuleAsDeclared(CallableMemberDescriptor declarationDescriptor, CodegenContext context) {
        if (context == CodegenContext.STATIC) {
            return true;
        }
        DeclarationDescriptor contextDescriptor = context.getContextDescriptor();
        return DescriptorUtils.areInSameModule(declarationDescriptor, contextDescriptor);
    }

    public static boolean hasAbstractMembers(@NotNull ClassDescriptor classDescriptor) {
        return ContainerUtil.exists(classDescriptor.getDefaultType().getMemberScope().getAllDescriptors(),
                                    new Condition<DeclarationDescriptor>() {
                                        @Override
                                        public boolean value(DeclarationDescriptor declaration) {
                                            if (!(declaration instanceof MemberDescriptor)) {
                                                return false;
                                            }
                                            return ((MemberDescriptor) declaration).getModality() == ABSTRACT;
                                        }
                                    });
    }

    /**
     * A work-around of the generic nullability problem in the type checker
     * @return true if a value of this type can be null
     */
    public static boolean isNullableType(@NotNull JetType type) {
        if (type.isNullable()) {
            return true;
        }
        if (type.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor) {
            return TypeUtils.hasNullableSuperType(type);
        }
        return false;
    }

    public static boolean couldUseDirectAccessToProperty(@NotNull PropertyDescriptor propertyDescriptor, boolean forGetter, boolean isInsideClass, boolean isDelegated) {
        PropertyAccessorDescriptor accessorDescriptor = forGetter ? propertyDescriptor.getGetter() : propertyDescriptor.getSetter();
        boolean isExtensionProperty = propertyDescriptor.getReceiverParameter() != null;
        boolean specialTypeProperty = isDelegated ||
                                      isExtensionProperty ||
                                      DescriptorUtils.isClassObject(propertyDescriptor.getContainingDeclaration()) ||
                                      JetTypeMapper.isAccessor(propertyDescriptor);
        return isInsideClass &&
               !specialTypeProperty &&
               (accessorDescriptor == null ||
                accessorDescriptor.isDefault() &&
                (!isExternallyAccessible(propertyDescriptor) || accessorDescriptor.getModality() == Modality.FINAL));
    }

    private static boolean isExternallyAccessible(@NotNull PropertyDescriptor propertyDescriptor) {
        return propertyDescriptor.getVisibility() != Visibilities.PRIVATE ||
               DescriptorUtils.isClassObject(propertyDescriptor.getContainingDeclaration()) ||
               DescriptorUtils.isTopLevelDeclaration(propertyDescriptor);
    }

    @NotNull
    public static ImplementationBodyCodegen getParentBodyCodegen(@Nullable MemberCodegen classBodyCodegen) {
        assert classBodyCodegen != null &&
               classBodyCodegen
                       .getParentCodegen() instanceof ImplementationBodyCodegen : "Class object should have appropriate parent BodyCodegen";

        return ((ImplementationBodyCodegen) classBodyCodegen.getParentCodegen());
    }

    static int getPathHashCode(@NotNull VirtualFile file) {
        // Conversion to system-dependent name seems to be unnecessary, but it's hard to check now:
        // it was introduced when fixing KT-2839, which appeared again (KT-3639).
        // If you try to remove it, run tests on Windows.
        return FileUtil.toSystemDependentName(file.getPath()).hashCode();
    }

    @Nullable
    public static ClassDescriptor getExpectedThisObjectForConstructorCall(
            @NotNull ConstructorDescriptor descriptor,
            @Nullable CalculatedClosure closure
    ) {
        //for compilation against sources
        if (closure != null) {
            return closure.getCaptureThis();
        }

        //for compilation against binaries
        //TODO: It's best to use this code also for compilation against sources
        // but sometimes structures that have expectedThisObject (bug?) mapped to static classes
        ReceiverParameterDescriptor expectedThisObject = descriptor.getExpectedThisObject();
        if (expectedThisObject != null) {
            ClassDescriptor expectedThisClass = (ClassDescriptor) expectedThisObject.getContainingDeclaration();
            if (!expectedThisClass.getKind().isSingleton()) {
                return expectedThisClass;
            }
        }

        return null;
    }
}
