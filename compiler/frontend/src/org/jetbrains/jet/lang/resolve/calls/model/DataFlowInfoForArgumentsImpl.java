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

package org.jetbrains.jet.lang.resolve.calls.model;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.calls.smartcasts.DataFlowInfo;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DataFlowInfoForArgumentsImpl implements MutableDataFlowInfoForArguments {
    private final Call call; //for better debug messages only
    private final Map<ValueArgument, DataFlowInfo>  infoMap = Maps.newHashMap();
    private final Map<ValueArgument, ValueArgument> nextArgument = Maps.newHashMap();
    private DataFlowInfo initialInfo;
    private DataFlowInfo resultInfo;

    public DataFlowInfoForArgumentsImpl(@NotNull Call call) {
        this.call = call;
        initNextArgMap(call.getValueArguments());
    }

    private void initNextArgMap(@NotNull List<? extends ValueArgument> valueArguments) {
        Iterator<? extends ValueArgument> iterator = valueArguments.iterator();
        ValueArgument prev = null;
        while (iterator.hasNext()) {
            ValueArgument argument = iterator.next();
            if (prev != null) {
                nextArgument.put(prev, argument);
            }
            prev = argument;
        }
    }

    @Override
    public void setInitialDataFlowInfo(@NotNull DataFlowInfo dataFlowInfo) {
        //TODO assert initialInfo == null
        initialInfo = dataFlowInfo;
    }

    @NotNull
    @Override
    public DataFlowInfo getInfo(@NotNull ValueArgument valueArgument) {
        DataFlowInfo infoForArgument = infoMap.get(valueArgument);
        if (infoForArgument == null) {
            return initialInfo;
        }
        return initialInfo.and(infoForArgument);
    }

    @Override
    public void updateInfo(@NotNull ValueArgument valueArgument, @NotNull DataFlowInfo dataFlowInfo) {
        ValueArgument next = nextArgument.get(valueArgument);
        if (next != null) {
            infoMap.put(next, dataFlowInfo);
            return;
        }
        //TODO assert resultInfo == null
        resultInfo = dataFlowInfo;
    }

    @NotNull
    @Override
    public DataFlowInfo getResultInfo() {
        if (resultInfo == null) return initialInfo;
        assert initialInfo != null : "Initial data flow info was not set for call: " + call;
        return initialInfo.and(resultInfo);
    }
}
