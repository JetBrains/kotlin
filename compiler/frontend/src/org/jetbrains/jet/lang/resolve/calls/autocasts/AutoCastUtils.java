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
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.scopes.receivers.*;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.AUTOCAST_IMPOSSIBLE;
import static org.jetbrains.jet.lang.resolve.BindingContext.AUTOCAST;
import static org.jetbrains.jet.lang.resolve.BindingContext.EXPRESSION_TYPE;

public class AutoCastUtils {

    private AutoCastUtils() {}

    /**
     * @return variants @param receiverToCast may be cast to according to @param dataFlowInfo, @param receiverToCast itself is NOT included
     */
    public static List<ReceiverValue> getAutoCastVariants(
            @NotNull final BindingContext bindingContext,
            @NotNull final DataFlowInfo dataFlowInfo, @NotNull ReceiverValue receiverToCast
    ) {
        return receiverToCast.accept(new ReceiverValueVisitor<List<ReceiverValue>, Object>() {
            @Override
            public List<ReceiverValue> visitNoReceiver(ReceiverValue noReceiver, Object data) {
                return Collections.emptyList();
            }

            @Override
            public List<ReceiverValue> visitTransientReceiver(TransientReceiver receiver, Object data) {
                return Collections.emptyList();
            }

            @Override
            public List<ReceiverValue> visitExtensionReceiver(ExtensionReceiver receiver, Object data) {
                return castThis(dataFlowInfo, receiver);
            }

            @Override
            public List<ReceiverValue> visitClassReceiver(ClassReceiver receiver, Object data) {
                return castThis(dataFlowInfo, receiver);
            }

            @Override
            public List<ReceiverValue> visitScriptReceiver(ScriptReceiver receiver, Object data) {
                return Collections.emptyList();
            }

            @Override
            public List<ReceiverValue> visitExpressionReceiver(ExpressionReceiver receiver, Object data) {
                DataFlowValue dataFlowValue = DataFlowValueFactory.INSTANCE.createDataFlowValue(receiver.getExpression(), receiver.getType(),
                                                                                                bindingContext);
                List<ReceiverValue> result = Lists.newArrayList();
                for (JetType possibleType : dataFlowInfo.getPossibleTypes(dataFlowValue)) {
                    result.add(new AutoCastReceiver(receiver, possibleType, dataFlowValue.isStableIdentifier()));
                }
                return result;
            }
        }, null);
    }

    private static List<ReceiverValue> castThis(@NotNull DataFlowInfo dataFlowInfo, @NotNull ThisReceiver receiver) {
        assert receiver.exists();
        List<ReceiverValue> result = Lists.newArrayList();
        for (JetType possibleType : dataFlowInfo.getPossibleTypes(DataFlowValueFactory.INSTANCE.createDataFlowValue(receiver))) {
            result.add(new AutoCastReceiver(receiver, possibleType, true));
        }
        return result;
    }

    public static void recordAutoCastIfNecessary(ReceiverValue receiver, @NotNull BindingTrace trace) {
        if (receiver instanceof AutoCastReceiver) {
            AutoCastReceiver autoCastReceiver = (AutoCastReceiver) receiver;
            ReceiverValue original = autoCastReceiver.getOriginal();
            if (original instanceof ExpressionReceiver) {
                ExpressionReceiver expressionReceiver = (ExpressionReceiver) original;
                if (autoCastReceiver.canCast()) {
                    trace.record(AUTOCAST, expressionReceiver.getExpression(), autoCastReceiver.getType());
                    trace.record(EXPRESSION_TYPE, expressionReceiver.getExpression(), autoCastReceiver.getType());
                }
                else {
                    trace.report(AUTOCAST_IMPOSSIBLE.on(expressionReceiver.getExpression(), autoCastReceiver.getType(), expressionReceiver.getExpression().getText()));
                }
            }
            else {
                assert autoCastReceiver.canCast() : "A non-expression receiver must always be autocastabe: " + original;
            }
        }
    }
}
