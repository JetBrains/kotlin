/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BodiesResolveContext;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

/**
 * @author Stepan Koltsov
 */
public class AnalyzeExhaust {
    @NotNull
    private final BindingContext bindingContext;
    private final Throwable error;

    @Nullable
    private final BodiesResolveContext bodiesResolveContext;

    private AnalyzeExhaust(@NotNull BindingContext bindingContext,
            @Nullable BodiesResolveContext bodiesResolveContext, @Nullable Throwable error) {
        this.bindingContext = bindingContext;
        this.error = error;
        this.bodiesResolveContext = bodiesResolveContext;
    }

    public static AnalyzeExhaust success(@NotNull BindingContext bindingContext) {
        return new AnalyzeExhaust(bindingContext, null, null);
    }

    public static AnalyzeExhaust success(@NotNull BindingContext bindingContext,
            BodiesResolveContext bodiesResolveContext) {
        return new AnalyzeExhaust(bindingContext, bodiesResolveContext, null);
    }

    public static AnalyzeExhaust error(@NotNull BindingContext bindingContext, @NotNull Throwable error) {
        return new AnalyzeExhaust(bindingContext, null, error);
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
}
