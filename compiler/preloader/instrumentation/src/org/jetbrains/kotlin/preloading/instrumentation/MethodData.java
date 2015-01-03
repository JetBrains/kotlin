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

package org.jetbrains.kotlin.preloading.instrumentation;

public class MethodData extends MemberData {
    private final FieldData ownerField;
    private final int thisParameterIndex;
    private final int classNameParameterIndex;
    private final int methodNameParameterIndex;
    private final int methodDescParameterIndex;
    private final int allArgsParameterIndex;

    MethodData(
            FieldData ownerField,
            String declaringClass,
            String name,
            String desc,
            int thisParameterIndex,
            int classNameParameterIndex,
            int methodNameParameterIndex,
            int methodDescParameterIndex,
            int allArgsParameterIndex
    ) {
        super(declaringClass, name, desc);
        this.ownerField = ownerField;
        this.thisParameterIndex = thisParameterIndex;
        this.classNameParameterIndex = classNameParameterIndex;
        this.methodNameParameterIndex = methodNameParameterIndex;
        this.methodDescParameterIndex = methodDescParameterIndex;
        this.allArgsParameterIndex = allArgsParameterIndex;
    }

    public FieldData getOwnerField() {
        return ownerField;
    }

    public int getThisParameterIndex() {
        return thisParameterIndex;
    }

    public int getClassNameParameterIndex() {
        return classNameParameterIndex;
    }

    public int getMethodNameParameterIndex() {
        return methodNameParameterIndex;
    }

    public int getMethodDescParameterIndex() {
        return methodDescParameterIndex;
    }

    public int getAllArgsParameterIndex() {
        return allArgsParameterIndex;
    }
}
