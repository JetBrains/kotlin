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

import org.jetbrains.asm4.Type;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;

class ParameterInfo {

    public static final ParameterInfo STUB = new ParameterInfo(AsmTypeConstants.OBJECT_TYPE, true, -1, -1);

    public final int index;

    public final Type type;

    public final boolean isSkipped;

    public final int remapIndex;

    ParameterInfo(Type type, boolean skipped, int remapIndex, int index) {
        this.type = type;
        this.isSkipped = skipped;
        this.remapIndex = remapIndex;
        this.index = index;
    }

    public boolean isSkippedOrRemapped() {
        return isSkipped || remapIndex != -1;
    }

    public int getInlinedIndex() {
        return remapIndex != -1 ? remapIndex : index;
    }

    public boolean isSkipped() {
        return isSkipped;
    }

    public Type getType() {
        return type;
    }
}
