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

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.binding.CalculatedClosure;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.NamespaceContext;
import org.jetbrains.jet.codegen.signature.BothSignatureWriter;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterKind;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetClassObject;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

import static org.jetbrains.jet.lang.descriptors.Modality.ABSTRACT;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;

public class CodegenUtil {

    private CodegenUtil() {
    }

    private static final Random RANDOM = new Random(55L);

    public static boolean isInterface(DeclarationDescriptor descriptor) {
        if (descriptor instanceof ClassDescriptor) {
            final ClassKind kind = ((ClassDescriptor) descriptor).getKind();
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
                                    Visibilities.PUBLIC,
                                    /*isInline = */false
        );
        return invokeDescriptor;
    }

    public static boolean isNonLiteralObject(JetClassOrObject myClass) {
        return myClass instanceof JetObjectDeclaration && !((JetObjectDeclaration) myClass).isObjectLiteral() &&
               !(myClass.getParent() instanceof JetClassObject);
    }


    public static String createTmpVariableName(Collection<String> existingNames) {
        String prefix = "tmp";
        int i = RANDOM.nextInt(Integer.MAX_VALUE);
        String name = prefix + i;
        while (existingNames.contains(name)) {
            i++;
            name = prefix + i;
        }
        return name;
    }


    public static int getFlagsForVisibility(@NotNull Visibility visibility) {
        if (visibility == Visibilities.INTERNAL) {
            return JvmStdlibNames.FLAG_INTERNAL_BIT;
        }
        else if (visibility == Visibilities.PRIVATE) {
            return JvmStdlibNames.FLAG_PRIVATE_BIT;
        }
        return 0;
    }

    public static int getFlagsForClassKind(@NotNull ClassDescriptor descriptor) {
        return descriptor.getKind() == ClassKind.OBJECT ? JvmStdlibNames.FLAG_CLASS_KIND_OBJECT : JvmStdlibNames.FLAG_CLASS_KIND_DEFAULT;
    }

    @NotNull
    public static JvmClassName getInternalClassName(FunctionDescriptor descriptor) {
        final int paramCount = descriptor.getValueParameters().size();
        if (descriptor.getReceiverParameter() != null) {
            return JvmClassName.byInternalName("jet/ExtensionFunction" + paramCount);
        }
        else {
            return JvmClassName.byInternalName("jet/Function" + paramCount);
        }
    }

    public static JvmMethodSignature erasedInvokeSignature(FunctionDescriptor fd) {

        BothSignatureWriter signatureWriter = new BothSignatureWriter(BothSignatureWriter.Mode.METHOD, false);

        signatureWriter.writeFormalTypeParametersStart();
        signatureWriter.writeFormalTypeParametersEnd();

        boolean isExtensionFunction = fd.getReceiverParameter() != null;
        int paramCount = fd.getValueParameters().size();
        if (isExtensionFunction) {
            paramCount++;
        }

        signatureWriter.writeParametersStart();

        for (int i = 0; i < paramCount; ++i) {
            signatureWriter.writeParameterType(JvmMethodParameterKind.VALUE);
            signatureWriter.writeAsmType(OBJECT_TYPE, true);
            signatureWriter.writeParameterTypeEnd();
        }

        signatureWriter.writeParametersEnd();

        signatureWriter.writeReturnType();
        signatureWriter.writeAsmType(OBJECT_TYPE, true);
        signatureWriter.writeReturnTypeEnd();

        return signatureWriter.makeJvmMethodSignature("invoke");
    }

    public static boolean isConst(CalculatedClosure closure) {
        return closure.getCaptureThis() == null && closure.getCaptureReceiver() == null && closure.getCaptureVariables().isEmpty();
    }

    public static <T> T peekFromStack(Stack<T> stack) {
        return stack.empty() ? null : stack.peek();
    }

    @Nullable
    public static String getLocalNameForObject(JetObjectDeclaration object) {
        PsiElement parent = object.getParent();
        if (parent instanceof JetClassObject) {
            return JvmAbi.CLASS_OBJECT_CLASS_NAME;
        }

        return null;
    }

    public static JetType getSuperClass(ClassDescriptor classDescriptor) {
        final List<ClassDescriptor> superclassDescriptors = DescriptorUtils.getSuperclassDescriptors(classDescriptor);
        for (ClassDescriptor descriptor : superclassDescriptors) {
            if (descriptor.getKind() != ClassKind.TRAIT) {
                return descriptor.getDefaultType();
            }
        }
        return KotlinBuiltIns.getInstance().getAnyType();
    }

    public static <T extends CallableMemberDescriptor> T unwrapFakeOverride(T member) {
        while (member.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            //noinspection unchecked
            member = (T) member.getOverriddenDescriptors().iterator().next();
        }
        return member;
    }

    public static void checkMustGenerateCode(CallableMemberDescriptor descriptor) {
        if (descriptor.getKind() != CallableMemberDescriptor.Kind.DECLARATION) {
            throw new IllegalStateException("Must not generate code for descriptor: " + descriptor);
        }
    }

    @Nullable
    public static FunctionDescriptor getDeclaredFunctionByRawSignature(
            @NotNull ClassDescriptor owner,
            @NotNull Name name,
            @NotNull ClassDescriptor returnedClass,
            @NotNull ClassDescriptor... valueParameterClasses
    ) {
        Collection<FunctionDescriptor> functions = owner.getDefaultType().getMemberScope().getFunctions(name);
        for (FunctionDescriptor function : functions) {
            if (function.getKind() == CallableMemberDescriptor.Kind.DECLARATION
                && function.getTypeParameters().isEmpty()
                && valueParameterClassesMatch(function.getValueParameters(), Arrays.asList(valueParameterClasses))
                && rawTypeMatches(function.getReturnType(), returnedClass)) {
                return function;
            }
        }
        return null;
    }

    private static boolean valueParameterClassesMatch(
            @NotNull List<ValueParameterDescriptor> parameters,
            @NotNull List<ClassDescriptor> classes) {
        if (parameters.size() != classes.size()) return false;
        for (int i = 0; i < parameters.size(); i++) {
            ValueParameterDescriptor parameterDescriptor = parameters.get(i);
            ClassDescriptor classDescriptor = classes.get(i);
            if (!rawTypeMatches(parameterDescriptor.getType(), classDescriptor)) {
                return false;
            }
        }
        return true;
    }

    private static boolean rawTypeMatches(JetType type, ClassDescriptor classDescriptor) {
        return type.getConstructor().getDeclarationDescriptor().getOriginal() == classDescriptor.getOriginal();
    }

    @SuppressWarnings("unchecked")
    static Collection<ClassDescriptor> getInnerClassesAndObjects(ClassDescriptor classDescriptor) {
        JetScope innerClassesScope = classDescriptor.getUnsubstitutedInnerClassesScope();
        Collection<DeclarationDescriptor> inners = innerClassesScope.getAllDescriptors();
        for (DeclarationDescriptor inner : inners) {
            assert inner instanceof ClassDescriptor
                    : "Not a class in inner classes scope of " + classDescriptor + ": " + inner;
        }
        return new ImmutableList.Builder<ClassDescriptor>()
                .addAll((Collection) inners)
                .addAll(innerClassesScope.getObjectDescriptors())
                .build();
    }

    public static boolean isCallInsideSameClassAsDeclared(CallableMemberDescriptor declarationDescriptor, CodegenContext context) {
        boolean isFakeOverride = declarationDescriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE;
        boolean isDelegate = declarationDescriptor.getKind() == CallableMemberDescriptor.Kind.DELEGATION;

        DeclarationDescriptor containingDeclaration = declarationDescriptor.getContainingDeclaration();
        containingDeclaration = containingDeclaration.getOriginal();

        return !isFakeOverride && !isDelegate &&
               (((context.hasThisDescriptor() && containingDeclaration == context.getThisDescriptor()) ||
                 (context.getParentContext() instanceof NamespaceContext && context.getParentContext().getContextDescriptor() == containingDeclaration))
                && context.getContextKind() != OwnerKind.TRAIT_IMPL);
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
}
