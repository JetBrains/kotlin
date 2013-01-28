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

package org.jetbrains.jet.lang.resolve.calls.autocasts;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;

import java.util.List;

public class AutoCastServiceImpl implements AutoCastService {
    private final DataFlowInfo dataFlowInfo;
    private final BindingContext bindingContext;

    public AutoCastServiceImpl(DataFlowInfo dataFlowInfo, BindingContext bindingContext) {
        this.dataFlowInfo = dataFlowInfo;
        this.bindingContext = bindingContext;
    }

    @NotNull
    @Override
    public List<ReceiverValue> getVariantsForReceiver(@NotNull ReceiverValue receiverValue) {
        List<ReceiverValue> variants = Lists.newArrayList(AutoCastUtils.getAutoCastVariants(bindingContext, dataFlowInfo, receiverValue));
        variants.add(receiverValue);
        return variants;
    }

    @NotNull
    @Override
    public DataFlowInfo getDataFlowInfo() {
        return dataFlowInfo;
    }

    @Override
    public boolean isNotNull(@NotNull ReceiverValue receiver) {
        if (!receiver.getType().isNullable()) return true;

        List<ReceiverValue> autoCastVariants = AutoCastUtils.getAutoCastVariants(bindingContext, dataFlowInfo, receiver);
        for (ReceiverValue autoCastVariant : autoCastVariants) {
            if (!autoCastVariant.getType().isNullable()) return true;
        }
        return false;
    }
}
