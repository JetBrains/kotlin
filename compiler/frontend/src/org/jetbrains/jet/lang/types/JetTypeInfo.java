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

package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.calls.smartcasts.DataFlowInfo;

public class JetTypeInfo {

    @NotNull
    public static JetTypeInfo create(@Nullable JetType type, @NotNull DataFlowInfo dataFlowInfo) {
        return new JetTypeInfo(type, dataFlowInfo);
    }
    
    private final JetType type;
    private final DataFlowInfo dataFlowInfo;

    private JetTypeInfo(@Nullable JetType type, @NotNull DataFlowInfo dataFlowInfo) {
        this.type = type;
        this.dataFlowInfo = dataFlowInfo;
    }

    @Nullable
    public JetType getType() {
        return type;
    }

    @NotNull
    public DataFlowInfo getDataFlowInfo() {
        return dataFlowInfo;
    }
}
