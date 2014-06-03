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

package org.jetbrains.jet.codegen.inline;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;

class ParameterInfo {

    public static final ParameterInfo STUB = new ParameterInfo(AsmTypeConstants.OBJECT_TYPE, true, -1, -1);

    protected final int index;

    private boolean isCaptured;

    public final Type type;

    //for skipped parameter: e.g. inlined lambda
    public final boolean isSkipped;

    //in case when parameter could be extracted from outer context (e.g. from local var)
    private StackValue remapValue;

    public LambdaInfo lambda;

    ParameterInfo(Type type, boolean skipped, int index, int remapValue) {
        this(type, skipped, index, remapValue == -1 ? null : StackValue.local(remapValue, type));
    }

    ParameterInfo(@NotNull Type type, boolean skipped, int index, @Nullable StackValue remapValue) {
        this.type = type;
        this.isSkipped = skipped;
        this.remapValue = remapValue;
        this.index = index;
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

    public void setLambda(LambdaInfo lambda) {
        this.lambda = lambda;
    }

    public void setRemapValue(StackValue remapValue) {
        this.remapValue = remapValue;
    }

    public boolean isCaptured() {
        return isCaptured;
    }

    public void setCaptured(boolean isCaptured) {
        this.isCaptured = isCaptured;
    }
}
