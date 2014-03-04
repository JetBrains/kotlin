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

package org.jetbrains.jet.plugin;

import com.intellij.util.NotNullFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

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

    /** Uses the {@code resolveSession} to resolve the function declaration. Suitable when the function declaration is not resolved yet. */
    public MainFunctionDetector(@NotNull final ResolveSession resolveSession) {
        this.getFunctionDescriptor = new NotNullFunction<JetNamedFunction, FunctionDescriptor>() {
            @NotNull
            @Override
            public FunctionDescriptor fun(JetNamedFunction function) {
                return (FunctionDescriptor) resolveSession.resolveToDescriptor(function);
            }
        };
    }

    public boolean hasMain(@NotNull List<JetDeclaration> declarations) {
        return findMainFunction(declarations) != null;
    }

    public boolean isMain(@NotNull JetNamedFunction function) {
        if ("main".equals(function.getName())) {
            FunctionDescriptor functionDescriptor = getFunctionDescriptor.fun(function);
            List<ValueParameterDescriptor> parameters = functionDescriptor.getValueParameters();
            if (parameters.size() == 1) {
                ValueParameterDescriptor parameter = parameters.get(0);
                JetType parameterType = parameter.getType();
                KotlinBuiltIns kotlinBuiltIns = KotlinBuiltIns.getInstance();
                if (kotlinBuiltIns.isArray(parameterType)) {
                    List<TypeProjection> typeArguments = parameterType.getArguments();
                    if (typeArguments.size() == 1) {
                        JetType typeArgument = typeArguments.get(0).getType();
                        if (JetTypeChecker.INSTANCE.equalTypes(typeArgument, kotlinBuiltIns.getStringType())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
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
