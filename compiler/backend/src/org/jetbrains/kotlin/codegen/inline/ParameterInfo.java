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

package org.jetbrains.kotlin.codegen.inline;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.org.objectweb.asm.Type;

public class ParameterInfo {
    private final int index;
    public final int declarationIndex;
    public final Type type;
    //for skipped parameter: e.g. inlined lambda
    public final boolean isSkipped;

    private boolean isCaptured;
    private LambdaInfo lambda;
    //in case when parameter could be extracted from outer context (e.g. from local var)
    private StackValue remapValue;

    public ParameterInfo(@NotNull Type type, boolean skipped, int index, int remapValue, int declarationIndex) {
        this(type, skipped, index, remapValue == -1 ? null : StackValue.local(remapValue, type), declarationIndex);
    }

    public ParameterInfo(@NotNull Type type, boolean skipped, int index, @Nullable StackValue remapValue, int declarationIndex) {
        this.type = type;
        this.isSkipped = skipped;
        this.remapValue = remapValue;
        this.index = index;
        this.declarationIndex = declarationIndex;
    }

    public boolean isSkippedOrRemapped() {
        return isSkipped || isRemapped();
    }

    public boolean isRemapped() {
        return remapValue != null;
    }

    @Nullable
    public StackValue getRemapValue() {
        return remapValue;
    }

    public int getIndex() {
        return index;
    }

    @NotNull
    public Type getType() {
        return type;
    }

    @Nullable
    public LambdaInfo getLambda() {
        return lambda;
    }

    @NotNull
    public ParameterInfo setLambda(@Nullable LambdaInfo lambda) {
        this.lambda = lambda;
        return this;
    }

    @NotNull
    public ParameterInfo setRemapValue(@Nullable StackValue remapValue) {
        this.remapValue = remapValue;
        return this;
    }

    public boolean isCaptured() {
        return isCaptured;
    }

    public void setCaptured(boolean isCaptured) {
        this.isCaptured = isCaptured;
    }
}
