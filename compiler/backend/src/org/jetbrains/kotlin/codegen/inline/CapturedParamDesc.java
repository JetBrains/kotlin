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
import org.jetbrains.org.objectweb.asm.Type;

public class CapturedParamDesc {
    private final Type containingLambdaType;
    private final String fieldName;
    private final Type type;

    public CapturedParamDesc(@NotNull Type containingLambdaType, @NotNull String fieldName, @NotNull Type type) {
        this.containingLambdaType = containingLambdaType;
        this.fieldName = fieldName;
        this.type = type;
    }

    @NotNull
    public String getContainingLambdaName() {
        return containingLambdaType.getInternalName();
    }

    @NotNull
    public String getFieldName() {
        return fieldName;
    }

    @NotNull
    public Type getType() {
        return type;
    }
}
