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

public class CapturedParamInfo extends ParameterInfo {
    public final CapturedParamDesc desc;
    private final String newFieldName;
    private final boolean skipInConstructor;

    //Now used only for bound function reference receiver
    private boolean synthetic;

    public CapturedParamInfo(@NotNull CapturedParamDesc desc, @NotNull String newFieldName, boolean skipped, int index, int remapIndex) {
        super(desc.getType(), skipped, index, remapIndex, -1);
        this.desc = desc;
        this.newFieldName = newFieldName;
        this.skipInConstructor = false;
    }

    public CapturedParamInfo(
            @NotNull CapturedParamDesc desc,
            @NotNull String newFieldName,
            boolean skipped,
            int index,
            @Nullable StackValue remapIndex,
            boolean skipInConstructor,
            int declarationIndex
    ) {
        super(desc.getType(), skipped, index, remapIndex, declarationIndex);
        this.desc = desc;
        this.newFieldName = newFieldName;
        this.skipInConstructor = skipInConstructor;
    }

    @NotNull
    public String getNewFieldName() {
        return newFieldName;
    }

    @NotNull
    public String getOriginalFieldName() {
        return desc.getFieldName();
    }

    @NotNull
    public CapturedParamInfo cloneWithNewDeclarationIndex(int newDeclarationIndex) {
        CapturedParamInfo result = new CapturedParamInfo(
                desc, newFieldName, isSkipped, getIndex(), getRemapValue(), skipInConstructor, newDeclarationIndex
        );
        result.setLambda(getLambda());
        result.setSynthetic(synthetic);
        return result;
    }

    @NotNull
    public String getContainingLambdaName() {
        return desc.getContainingLambdaName();
    }

    public boolean isSkipInConstructor() {
        return skipInConstructor;
    }

    public boolean isSynthetic() {
        return synthetic;
    }

    public void setSynthetic(boolean synthetic) {
        this.synthetic = synthetic;
    }

    public static boolean isSynthetic(@NotNull ParameterInfo info) {
        return info instanceof CapturedParamInfo && ((CapturedParamInfo) info).isSynthetic();
    }
}
