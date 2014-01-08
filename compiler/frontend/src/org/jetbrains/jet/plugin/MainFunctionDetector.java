/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import com.google.common.base.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.TypeResolver;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.ScopeProvider;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.utils.Printer;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

public class MainFunctionDetector {
    private final BindingTrace bindingTrace;
    private final TypeResolver typeResolver;
    private final Function<JetDeclaration, JetScope> declaringScopes;

    public MainFunctionDetector(
            BindingTrace bindingTrace,
            TypeResolver typeResolver,
            Function<JetDeclaration, JetScope> declaringScopes
    ) {
        this.bindingTrace = bindingTrace;
        this.typeResolver = typeResolver;
        this.declaringScopes = declaringScopes;
    }

    public MainFunctionDetector(ResolveSession resolveSession) {
        final ScopeProvider scopeProvider = resolveSession.getInjector().getScopeProvider();
        Function<JetDeclaration, JetScope> declaringScopes = new Function<JetDeclaration, JetScope>() {
            @Override
            public JetScope apply(JetDeclaration declaration) {
                return scopeProvider.getResolutionScopeForDeclaration(declaration);
            }
        };
        this.bindingTrace = resolveSession.getTrace();
        this.typeResolver = resolveSession.getInjector().getTypeResolver();
        this.declaringScopes = declaringScopes;
    }

    public boolean hasMain(@NotNull List<JetDeclaration> declarations) {
        return findMainFunction(declarations) != null;
    }

    public boolean isMain(@NotNull JetNamedFunction function) {
        if ("main".equals(function.getName())) {
            List<JetParameter> parameters = function.getValueParameters();
            if (parameters.size() == 1) {
                JetTypeReference reference = parameters.get(0).getTypeReference();
                if (reference != null) {
                    JetScope scope = declaringScopes.apply(function);
                    if (scope != null) {
                        JetType type = typeResolver.resolveType(scope, reference, bindingTrace, true);
                        KotlinBuiltIns kotlinBuiltIns = KotlinBuiltIns.getInstance();
                        if (kotlinBuiltIns.isArray(type)) {
                            List<TypeProjection> typeArguments = type.getArguments();
                            if (typeArguments.size() == 1) {
                                JetType typeArgument = typeArguments.get(0).getType();
                                if (JetTypeChecker.INSTANCE.equalTypes(typeArgument, kotlinBuiltIns.getStringType())) {
                                    return true;
                                }
                            }
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
