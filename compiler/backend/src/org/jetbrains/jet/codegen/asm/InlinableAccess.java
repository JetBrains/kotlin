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

import org.jetbrains.annotations.Nullable;

import java.util.List;

class InlinableAccess {

    public final int index;

    public final boolean inlinable;

    private final List<ParameterInfo> parameters;

    private LambdaInfo info;

    InlinableAccess(int index, boolean isInlinable, List<ParameterInfo> parameterInfos) {
        this.index = index;
        inlinable = isInlinable;
        this.parameters = parameterInfos;
    }

    public boolean isInlinable() {
        return inlinable;
    }

    @Nullable
    public LambdaInfo getInfo() {
        return info;
    }

    public void setInfo(LambdaInfo info) {
        this.info = info;
    }

    public List<ParameterInfo> getParameters() {
        return parameters;
    }
}
