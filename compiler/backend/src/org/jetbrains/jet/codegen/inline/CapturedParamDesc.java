/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
import org.jetbrains.jet.codegen.context.EnclosedValueDescriptor;

public class CapturedParamDesc {

    private final CapturedParamOwner containingLambda;

    private final String fieldName;

    private final Type type;

    public CapturedParamDesc(@NotNull CapturedParamOwner containingLambda, @NotNull EnclosedValueDescriptor descriptor) {
        this.containingLambda = containingLambda;
        this.type = descriptor.getType();
        this.fieldName = descriptor.getFieldName();
    }

    public CapturedParamDesc(@NotNull CapturedParamOwner containingLambda, @NotNull String fieldName, @NotNull Type type) {
        this.containingLambda = containingLambda;
        this.fieldName = fieldName;
        this.type = type;
    }

    public CapturedParamOwner getContainingLambda() {
        return containingLambda;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Type getType() {
        return type;
    }


    public static CapturedParamDesc createDesc(@NotNull CapturedParamOwner containingLambdaInfo, @NotNull String fieldName, @NotNull Type type) {
        return new CapturedParamDesc(containingLambdaInfo, fieldName, type);
    }

    @NotNull
    public String getContainingLambdaName() {
        return containingLambda.getType().getInternalName();
    }
}
