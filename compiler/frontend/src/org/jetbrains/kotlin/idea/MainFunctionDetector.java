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

package org.jetbrains.kotlin.idea;

import com.intellij.util.NotNullFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.annotations.AnnotationUtilKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeProjection;
import org.jetbrains.kotlin.types.Variance;

import java.util.Collection;
import java.util.List;

public class MainFunctionDetector {
    private final NotNullFunction<KtNamedFunction, FunctionDescriptor> getFunctionDescriptor;

    /** Assumes that the function declaration is already resolved and the descriptor can be found in the {@code bindingContext}. */
    public MainFunctionDetector(@NotNull final BindingContext bindingContext) {
        this.getFunctionDescriptor = new NotNullFunction<KtNamedFunction, FunctionDescriptor>() {
            @NotNull
            @Override
            public FunctionDescriptor fun(KtNamedFunction function) {
                SimpleFunctionDescriptor functionDescriptor = bindingContext.get(BindingContext.FUNCTION, function);
                if (functionDescriptor == null) {
                    throw new IllegalStateException("No descriptor resolved for " + function + " " + function.getText());
                }
                return functionDescriptor;
            }
        };
    }

    public MainFunctionDetector(@NotNull NotNullFunction<KtNamedFunction, FunctionDescriptor> functionResolver) {
        this.getFunctionDescriptor = functionResolver;
    }

    public boolean hasMain(@NotNull List<KtDeclaration> declarations) {
        return findMainFunction(declarations) != null;
    }

    public boolean isMain(@NotNull KtNamedFunction function) {
        if (function.isLocal()) {
            return false;
        }

        if (function.getValueParameters().size() != 1 || !function.getTypeParameters().isEmpty()) {
            return false;
        }

        /* Psi only check for kotlin.jvm.jvmName annotation */
        if (!"main".equals(function.getName()) && !hasAnnotationWithExactNumberOfArguments(function, 1)) {
            return false;
        }

        /* Psi only check for kotlin.jvm.jvmStatic annotation */
        if (!function.isTopLevel() && !hasAnnotationWithExactNumberOfArguments(function, 0)) {
            return false;
        }

        return isMain(getFunctionDescriptor.fun(function));
    }

    public static boolean isMain(@NotNull DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof FunctionDescriptor)) return false;

        FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
        if (!getJVMFunctionName(functionDescriptor).equals("main")) {
            return false;
        }

        List<ValueParameterDescriptor> parameters = functionDescriptor.getValueParameters();
        if (parameters.size() != 1 || !functionDescriptor.getTypeParameters().isEmpty()) return false;

        ValueParameterDescriptor parameter = parameters.get(0);
        KotlinType parameterType = parameter.getType();
        if (!KotlinBuiltIns.isArray(parameterType)) return false;

        List<TypeProjection> typeArguments = parameterType.getArguments();
        if (typeArguments.size() != 1) return false;

        KotlinType typeArgument = typeArguments.get(0).getType();
        if (!KotlinBuiltIns.isString(typeArgument)) {
            return false;
        }
        if (typeArguments.get(0).getProjectionKind() == Variance.IN_VARIANCE) {
            return false;
        }

        if (DescriptorUtils.isTopLevelDeclaration(functionDescriptor)) return true;

        DeclarationDescriptor containingDeclaration = functionDescriptor.getContainingDeclaration();
        return containingDeclaration instanceof ClassDescriptor
               && ((ClassDescriptor) containingDeclaration).getKind().isSingleton()
               && AnnotationUtilKt.hasJvmStaticAnnotation(functionDescriptor);
    }

    @Nullable
    public KtNamedFunction getMainFunction(@NotNull Collection<KtFile> files) {
        for (KtFile file : files) {
            KtNamedFunction mainFunction = findMainFunction(file.getDeclarations());
            if (mainFunction != null) {
                return mainFunction;
            }
        }
        return null;
    }

    @Nullable
    private KtNamedFunction findMainFunction(@NotNull List<KtDeclaration> declarations) {
        for (KtDeclaration declaration : declarations) {
            if (declaration instanceof KtNamedFunction) {
                KtNamedFunction candidateFunction = (KtNamedFunction) declaration;
                if (isMain(candidateFunction)) {
                    return candidateFunction;
                }
            }
        }
        return null;
    }

    @NotNull
    private static String getJVMFunctionName(FunctionDescriptor functionDescriptor) {
        String platformName = DescriptorUtils.getJvmName(functionDescriptor);
        if (platformName != null) {
            return platformName;
        }

        return functionDescriptor.getName().asString();
    }

    private static boolean hasAnnotationWithExactNumberOfArguments(@NotNull KtNamedFunction function, int number) {
        for (KtAnnotationEntry entry : function.getAnnotationEntries()) {
            if (entry.getValueArguments().size() == number) {
                return true;
            }
        }

        return false;
    }
}
