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
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;

public class CapturedParamInfo extends ParameterInfo {

    public static final CapturedParamInfo STUB = new CapturedParamInfo(CapturedParamDesc.createDesc(new CapturedParamOwner() {
        @Override
        public Type getType() {
            return Type.getObjectType("STUB");
        }
    }, "STUB", Type.getObjectType("STUB")), true, -1, -1);

    public final CapturedParamDesc desc;

    private int shift = 0;

    public CapturedParamInfo(@NotNull CapturedParamDesc desc, boolean skipped, int index, int remapIndex) {
        super(desc.getType(), skipped, index, remapIndex);
        this.desc = desc;
    }

    public CapturedParamInfo(@NotNull CapturedParamDesc desc, boolean skipped, int index, StackValue remapIndex) {
        super(desc.getType(), skipped, index, remapIndex);
        this.desc = desc;
    }

    public String getFieldName() {
        return desc.getFieldName();
    }

    @Override
    public int getIndex() {
        return shift + super.getIndex();
    }

    public void setShift(int shift) {
        this.shift = shift;
    }

    public CapturedParamInfo newIndex(int newIndex) {
        return clone(newIndex, getRemapValue());
    }

    public CapturedParamInfo clone(int newIndex, StackValue newRamapIndex) {
        CapturedParamInfo capturedParamInfo = new CapturedParamInfo(desc, isSkipped, newIndex, newRamapIndex);
        capturedParamInfo.setLambda(lambda);
        return capturedParamInfo;
    }

    public String getContainingLambdaName() {
        return desc.getContainingLambda().getType().getInternalName();
    }
}
