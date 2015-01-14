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

package org.jetbrains.kotlin.plugin;

import com.intellij.util.NotNullFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.JetDeclaration;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetNamedFunction;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.annotations.AnnotationsPackage;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeProjection;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.Collection;
import java.util.List;

public class MainFunctionDetector {
    private final NotNullFunction<JetNamedFunction, FunctionDescriptor> getFunctionDescriptor;

    /** Assumes that the function declaration is already resolved and the descriptor can be found in the {@code bindingContext}. */
    public MainFunctionDetector(@NotNull final BindingContext bindingContext) {
        this.getFunctionDescriptor = new NotNullFunction<JetNamedFunction, FunctionDescriptor>() {
            @NotNull
            @Override
            public FunctionDescriptor fun(JetNamedFunction function) {
                SimpleFunctionDescriptor functionDescriptor = bindingContext.get(BindingContext.FUNCTION, function);
                if (functionDescriptor == null) {
                    throw new IllegalStateException("No descriptor resolved for " + function + " " + function.getText());
                }
                return functionDescriptor;
            }
        };
    }

    public MainFunctionDetector(@NotNull NotNullFunction<JetNamedFunction, FunctionDescriptor> functionResolver) {
        this.getFunctionDescriptor = functionResolver;
    }

    public boolean hasMain(@NotNull List<JetDeclaration> declarations) {
        return findMainFunction(declarations) != null;
    }

    public boolean isMain(@NotNull JetNamedFunction function) {
        if (!"main".equals(function.getName())) return false;

        FunctionDescriptor functionDescriptor = getFunctionDescriptor.fun(function);
        List<ValueParameterDescriptor> parameters = functionDescriptor.getValueParameters();
        if (parameters.size() != 1) return false;

        ValueParameterDescriptor parameter = parameters.get(0);
        JetType parameterType = parameter.getType();
        KotlinBuiltIns kotlinBuiltIns = KotlinBuiltIns.getInstance();
        if (!KotlinBuiltIns.isArray(parameterType)) return false;

        List<TypeProjection> typeArguments = parameterType.getArguments();
        if (typeArguments.size() != 1) return false;

        JetType typeArgument = typeArguments.get(0).getType();
        if (!JetTypeChecker.DEFAULT.equalTypes(typeArgument, kotlinBuiltIns.getStringType())) return false;

        if (DescriptorUtils.isTopLevelDeclaration(functionDescriptor)) return true;

        DeclarationDescriptor containingDeclaration = functionDescriptor.getContainingDeclaration();
        return containingDeclaration instanceof ClassDescriptor
               && ((ClassDescriptor) containingDeclaration).getKind().isSingleton()
               && AnnotationsPackage.hasPlatformStaticAnnotation(functionDescriptor);
    }

    @Nullable
    public JetNamedFunction getMainFunction(@NotNull Collection<JetFile> files) {
        for (JetFile file : files) {
            JetNamedFunction mainFunction = findMainFunction(file.getDeclarations());
            if (mainFunction != null) {
                return mainFunction;
            }
        }
        return null;
    }

    @Nullable
    private JetNamedFunction findMainFunction(@NotNull List<JetDeclaration> declarations) {
        for (JetDeclaration declaration : declarations) {
            if (declaration instanceof JetNamedFunction) {
                JetNamedFunction candidateFunction = (JetNamedFunction) declaration;
                if (isMain(candidateFunction)) {
                    return candidateFunction;
                }
            }
        }
        return null;
    }
}
