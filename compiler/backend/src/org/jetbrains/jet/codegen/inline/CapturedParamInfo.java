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
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.jet.codegen.StackValue;

public class CapturedParamInfo extends ParameterInfo {

    public static final CapturedParamInfo STUB = new CapturedParamInfo(CapturedParamDesc.createDesc(new CapturedParamOwner() {
        @Override
        public Type getType() {
            return Type.getObjectType("STUB");
        }
    }, "STUB", Type.getObjectType("STUB")), true, -1, -1);

    public final CapturedParamDesc desc;

    private final String newFieldName;

    public CapturedParamInfo(@NotNull CapturedParamDesc desc, boolean skipped, int index, int remapIndex) {
        this(desc, desc.getFieldName(), skipped, index, remapIndex);
    }

    public CapturedParamInfo(@NotNull CapturedParamDesc desc, @NotNull String newFieldName, boolean skipped, int index, int remapIndex) {
        super(desc.getType(), skipped, index, remapIndex);
        this.desc = desc;
        this.newFieldName = newFieldName;
    }

    public CapturedParamInfo(@NotNull CapturedParamDesc desc, @NotNull String newFieldName, boolean skipped, int index, StackValue remapIndex) {
        super(desc.getType(), skipped, index, remapIndex);
        this.desc = desc;
        this.newFieldName = newFieldName;
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
    public CapturedParamInfo newIndex(int newIndex) {
        return clone(newIndex, getRemapValue());
    }

    @NotNull
    public CapturedParamInfo clone(int newIndex, StackValue newRamapIndex) {
        CapturedParamInfo capturedParamInfo = new CapturedParamInfo(desc, newFieldName, isSkipped, newIndex, newRamapIndex);
        capturedParamInfo.setLambda(lambda);
        return capturedParamInfo;
    }

    @NotNull
    public String getContainingLambdaName() {
        return desc.getContainingLambdaName();
    }
}
