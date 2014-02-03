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

package org.jetbrains.jet.codegen.asm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;

public class CapturedParamInfo extends ParameterInfo {

    private final String fieldName;

    private int shift = 0;

    public static final CapturedParamInfo STUB = new CapturedParamInfo("", AsmTypeConstants.OBJECT_TYPE, true, -1, -1);

    public CapturedParamInfo(@NotNull String fieldName, @NotNull Type type, boolean skipped, int remapIndex, int index) {
        super(type, skipped, remapIndex, index);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public int getIndex() {
        return shift + super.getIndex();
    }

    public void setShift(int shift) {
        this.shift = shift;
    }
}
