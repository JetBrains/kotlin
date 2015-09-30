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
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.org.objectweb.asm.Type;

class ParameterInfo {

    public static final ParameterInfo STUB = new ParameterInfo(AsmTypes.OBJECT_TYPE, true, -1, -1, -1);

    protected final int index;

    protected final int declarationIndex;

    private boolean isCaptured;

    public final Type type;

    //for skipped parameter: e.g. inlined lambda
    public boolean isSkipped;

    //in case when parameter could be extracted from outer context (e.g. from local var)
    private StackValue remapValue;

    public LambdaInfo lambda;

    ParameterInfo(Type type, boolean skipped, int index, int remapValue, int declarationIndex) {
        this(type, skipped, index, remapValue == -1 ? null : StackValue.local(remapValue, type), declarationIndex);
    }

    ParameterInfo(@NotNull Type type, boolean skipped, int index, @Nullable StackValue remapValue, int declarationIndex) {
        this.type = type;
        this.isSkipped = skipped;
        this.remapValue = remapValue;
        this.index = index;
        this.declarationIndex = declarationIndex;
    }

    public boolean isSkippedOrRemapped() {
        return isSkipped || remapValue != null;
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

    public boolean isSkipped() {
        return isSkipped;
    }

    @NotNull
    public Type getType() {
        return type;
    }

    @Nullable
    public LambdaInfo getLambda() {
        return lambda;
    }

    public ParameterInfo setLambda(@Nullable LambdaInfo lambda) {
        this.lambda = lambda;
        return this;
    }

    public ParameterInfo setRemapValue(StackValue remapValue) {
        this.remapValue = remapValue;
        return this;
    }

    public boolean isCaptured() {
        return isCaptured;
    }

    public ParameterInfo setSkipped(boolean skipped) {
        isSkipped = skipped;
        return this;
    }

    public void setCaptured(boolean isCaptured) {
        this.isCaptured = isCaptured;
    }

}
