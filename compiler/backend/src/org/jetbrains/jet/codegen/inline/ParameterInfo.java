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
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;

class ParameterInfo {

    public static final ParameterInfo STUB = new ParameterInfo(AsmTypeConstants.OBJECT_TYPE, true, -1, -1);

    protected final int index;

    public final Type type;

    //for skipped parameter: e.g. inlined lambda
    public final boolean isSkipped;

    //in case when parameter could be extracted from outer context (e.g. from local var)
    private StackValue remapIndex;

    public LambdaInfo lambda;

    ParameterInfo(Type type, boolean skipped, int index, int remapIndex) {
        this(type, skipped, index, remapIndex == -1 ? null : StackValue.local(remapIndex, type));
    }

    ParameterInfo(Type type, boolean skipped, int index, StackValue stackValue) {
        this.type = type;
        this.isSkipped = skipped;
        this.remapIndex = stackValue;
        this.index = index;
    }

    public boolean isSkippedOrRemapped() {
        return isSkipped || remapIndex != null;
    }

    public boolean isRemapped() {
        return remapIndex != null;
    }

    @Nullable
    public StackValue getRemapIndex() {
        return remapIndex;
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

    public void setRemapIndex(StackValue remapIndex) {
        this.remapIndex = remapIndex;
    }
}
