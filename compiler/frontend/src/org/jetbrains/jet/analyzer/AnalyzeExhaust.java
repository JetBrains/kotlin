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
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.ErrorUtils;

public class AnalyzeExhaust {
    public static final AnalyzeExhaust EMPTY = success(BindingContext.EMPTY, ErrorUtils.getErrorModule());

    @NotNull
    public static AnalyzeExhaust success(@NotNull BindingContext bindingContext, @NotNull ModuleDescriptor module) {
        return new AnalyzeExhaust(bindingContext, module, null);
    }

    @NotNull
    public static AnalyzeExhaust error(@NotNull BindingContext bindingContext, @NotNull Throwable error) {
        return new AnalyzeExhaust(bindingContext, ErrorUtils.getErrorModule(), error);
    }

    private final BindingContext bindingContext;
    private final Throwable error;
    private final ModuleDescriptor moduleDescriptor;

    private AnalyzeExhaust(
            @NotNull BindingContext bindingContext,
            @NotNull ModuleDescriptor moduleDescriptor,
            @Nullable Throwable error
    ) {
        this.bindingContext = bindingContext;
        this.error = error;
        this.moduleDescriptor = moduleDescriptor;
    }

    @NotNull
    public BindingContext getBindingContext() {
        return bindingContext;
    }

    @NotNull
    public Throwable getError() {
        if (error == null) throw new IllegalStateException("Should be called only for error analyze result");
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
    public ModuleDescriptor getModuleDescriptor() {
        return moduleDescriptor;
    }
}
