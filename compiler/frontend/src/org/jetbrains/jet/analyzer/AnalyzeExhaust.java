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

package org.jetbrains.jet.analyzer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.BodiesResolveContext;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import java.util.Collections;
import java.util.List;

public class AnalyzeExhaust {
    private static final ModuleConfiguration ERROR_CONFIGURATION = new ModuleConfiguration() {
        @Override
        public List<ImportPath> getDefaultImports() {
            return Collections.emptyList();
        }

        @Override
        public void extendNamespaceScope(@NotNull BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor, @NotNull WritableScope namespaceMemberScope) {
        }

        @NotNull
        @Override
        public PlatformToKotlinClassMap getPlatformToKotlinClassMap() {
            return PlatformToKotlinClassMap.EMPTY;
        }

        @Override
        public String toString() {
            return "ERROR_CONFIGURATION";
        }
    };

    public static AnalyzeExhaust success(@NotNull BindingContext bindingContext, @NotNull ModuleConfiguration configuration) {
        return new AnalyzeExhaust(bindingContext, configuration, null, null);
    }
    public static AnalyzeExhaust success(@NotNull BindingContext bindingContext,
            @Nullable BodiesResolveContext bodiesResolveContext,
            @NotNull ModuleConfiguration configuration
    ) {
        return new AnalyzeExhaust(bindingContext, configuration, bodiesResolveContext, null);
    }

    public static AnalyzeExhaust error(@NotNull BindingContext bindingContext, @NotNull Throwable error) {
        return new AnalyzeExhaust(bindingContext, ERROR_CONFIGURATION, null, error);
    }

    private final BindingContext bindingContext;
    private final Throwable error;
    private final BodiesResolveContext bodiesResolveContext;
    private final ModuleConfiguration configuration;

    private AnalyzeExhaust(
            @NotNull BindingContext bindingContext,
            @NotNull ModuleConfiguration configuration,
            @Nullable BodiesResolveContext bodiesResolveContext,
            @Nullable Throwable error
    ) {
        this.bindingContext = bindingContext;
        this.error = error;
        this.bodiesResolveContext = bodiesResolveContext;
        this.configuration = configuration;
    }

    @Nullable
    public BodiesResolveContext getBodiesResolveContext() {
        return bodiesResolveContext;
    }

    @NotNull
    public BindingContext getBindingContext() {
        return bindingContext;
    }

    @NotNull
    public Throwable getError() {
        return error;
    }

    public boolean isError() {
        return error != null;
    }

    public void throwIfError() {
        if (isError()) {
            throw new IllegalStateException("failed to analyze: " + error, error);
        }
    }

    @NotNull
    public ModuleConfiguration getModuleConfiguration() {
        return configuration;
    }
}
