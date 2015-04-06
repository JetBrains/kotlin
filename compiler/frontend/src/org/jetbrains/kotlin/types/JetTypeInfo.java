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

package org.jetbrains.kotlin.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;

/**
 * This class is intended to transfer data flow analysis information in bottom-up direction,
 * from AST children to parents. It stores a type of expression under analysis,
 * current information about types and nullabilities.
 *
 * NB: it must be immutable together with all its descendants!
 */
public class JetTypeInfo {
    @NotNull
    public static JetTypeInfo create(@Nullable JetType type, @NotNull DataFlowInfo dataFlowInfo) {
        return new JetTypeInfo(type, dataFlowInfo);
    }

    private final JetType type;
    private final DataFlowInfo dataFlowInfo;

    protected JetTypeInfo(@Nullable JetType type, @NotNull DataFlowInfo dataFlowInfo) {
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
