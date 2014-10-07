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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.calls.smartcasts.DataFlowInfo;

public interface MutableDataFlowInfoForArguments extends DataFlowInfoForArguments {

    void setInitialDataFlowInfo(@NotNull DataFlowInfo dataFlowInfo);

    void updateInfo(@NotNull ValueArgument valueArgument, @NotNull DataFlowInfo dataFlowInfo);

    MutableDataFlowInfoForArguments WITHOUT_ARGUMENTS_CHECK = new MutableDataFlowInfoForArguments() {
        private DataFlowInfo dataFlowInfo;

        @Override
        public void setInitialDataFlowInfo(@NotNull DataFlowInfo dataFlowInfo) {
            this.dataFlowInfo = dataFlowInfo;
        }

        @Override
        public void updateInfo(
                @NotNull ValueArgument valueArgument, @NotNull DataFlowInfo dataFlowInfo
        ) {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public DataFlowInfo getInfo(@NotNull ValueArgument valueArgument) {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public DataFlowInfo getResultInfo() {
            return dataFlowInfo;
        }
    };
}
