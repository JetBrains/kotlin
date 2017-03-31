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

package org.jetbrains.kotlin.resolve.calls.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.psi.ValueArgument;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DataFlowInfoForArgumentsImpl extends MutableDataFlowInfoForArguments {
    @Nullable private Map<ValueArgument, DataFlowInfo> infoMap = null;
    @Nullable private Map<ValueArgument, ValueArgument> nextArgument = null;
    @Nullable private DataFlowInfo resultInfo;

    public DataFlowInfoForArgumentsImpl(@NotNull DataFlowInfo initialInfo, @NotNull Call call) {
        super(initialInfo);
        initNextArgMap(call.getValueArguments());
    }

    private void initNextArgMap(@NotNull List<? extends ValueArgument> valueArguments) {
        Iterator<? extends ValueArgument> iterator = valueArguments.iterator();
        ValueArgument prev = null;
        while (iterator.hasNext()) {
            ValueArgument argument = iterator.next();
            if (prev != null) {
                if (nextArgument == null) {
                    nextArgument = new HashMap<>();
                }
                nextArgument.put(prev, argument);
            }
            prev = argument;
        }
    }

    @NotNull
    @Override
    public DataFlowInfo getInfo(@NotNull ValueArgument valueArgument) {
        DataFlowInfo infoForArgument = infoMap == null ? null : infoMap.get(valueArgument);
        if (infoForArgument == null) {
            return initialDataFlowInfo;
        }
        return initialDataFlowInfo.and(infoForArgument);
    }

    @Override
    public void updateInfo(@NotNull ValueArgument valueArgument, @NotNull DataFlowInfo dataFlowInfo) {
        ValueArgument next = nextArgument == null ? null : nextArgument.get(valueArgument);
        if (next != null) {
            if (infoMap == null) {
                infoMap = new HashMap<>();
            }
            infoMap.put(next, dataFlowInfo);
            return;
        }
        //TODO assert resultInfo == null
        resultInfo = dataFlowInfo;
    }

    @NotNull
    @Override
    public DataFlowInfo getResultInfo() {
        if (resultInfo == null) return initialDataFlowInfo;
        return initialDataFlowInfo.and(resultInfo);
    }
}
