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

import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.SubModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BodiesResolveContext;
import org.jetbrains.jet.lang.resolve.ModuleSourcesManager;

import java.util.Collection;

public class AnalyzeExhaust {

    private static final ModuleSourcesManager ERROR_MANAGER = new ModuleSourcesManager() {
        @NotNull
        @Override
        public SubModuleDescriptor getSubModuleForFile(@NotNull PsiFile file) {
            throw new UnsupportedOperationException(); // TODO
        }

        @NotNull
        @Override
        public Collection<JetFile> getPackageFragmentSources(@NotNull PackageFragmentDescriptor packageFragment) {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public String toString() {
            return "ERROR";
        }
    };

    public static AnalyzeExhaust success(@NotNull BindingContext bindingContext, @NotNull ModuleSourcesManager moduleSourcesManager) {
        return new AnalyzeExhaust(bindingContext, moduleSourcesManager, null, null);
    }
    public static AnalyzeExhaust success(@NotNull BindingContext bindingContext,
            @Nullable BodiesResolveContext bodiesResolveContext,
            @NotNull ModuleSourcesManager moduleSourcesManager
    ) {
        return new AnalyzeExhaust(bindingContext, moduleSourcesManager, bodiesResolveContext, null);
    }

    public static AnalyzeExhaust error(@NotNull BindingContext bindingContext, @NotNull Throwable error) {
        return new AnalyzeExhaust(bindingContext, ERROR_MANAGER, null, error);
    }

    private final BindingContext bindingContext;
    private final Throwable error;
    private final BodiesResolveContext bodiesResolveContext;
    private final ModuleSourcesManager moduleSourcesManager;

    private AnalyzeExhaust(
            @NotNull BindingContext bindingContext,
            @NotNull ModuleSourcesManager moduleSourcesManager,
            @Nullable BodiesResolveContext bodiesResolveContext,
            @Nullable Throwable error
    ) {
        this.bindingContext = bindingContext;
        this.error = error;
        this.bodiesResolveContext = bodiesResolveContext;
        this.moduleSourcesManager = moduleSourcesManager;
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
    public ModuleSourcesManager getModuleSourcesManager() {
        return moduleSourcesManager;
    }
}
