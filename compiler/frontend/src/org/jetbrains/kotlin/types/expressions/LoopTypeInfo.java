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

package org.jetbrains.kotlin.types.expressions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.JetTypeInfo;

/**
 * A local descendant of JetTypeInfo. Stores simultaneously current data flow info
 * and jump point data flow info, together with information about possible jump inside. For example:
 * do {
 *     x!!.foo()
 *     if (bar()) break;
 *     y!!.gav()
 * } while (bis())
 * At the end current data flow info is x != null && y != null, but jump data flow info is x != null only.
 * Jump will be possible
 */
/*package*/ class LoopTypeInfo extends JetTypeInfo {

    private final DataFlowInfo jumpFlowInfo;

    private final boolean jumpOutPossible;

    LoopTypeInfo(@Nullable JetType type, @NotNull DataFlowInfo dataFlowInfo) {
        this(type, dataFlowInfo, false, dataFlowInfo);
    }

    LoopTypeInfo(
            @Nullable JetType type, @NotNull DataFlowInfo dataFlowInfo,
            boolean jumpOutPossible, @NotNull DataFlowInfo jumpFlowInfo
    ) {
        super(type, dataFlowInfo);
        this.jumpFlowInfo = jumpFlowInfo;
        this.jumpOutPossible = jumpOutPossible;
    }

    @NotNull
    DataFlowInfo getJumpFlowInfo() {
        return jumpFlowInfo;
    }

    /**
     * Returns true if jump to the end of the loop is possible inside the considered expression.
     * Break and continue are both counted as possible jump to the end, but return is not.
     */
    boolean isJumpOutPossible() {
        return jumpOutPossible;
    }
}
