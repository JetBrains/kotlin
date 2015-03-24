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
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue;
import org.jetbrains.kotlin.resolve.calls.smartcasts.Nullability;

import java.util.*;

/**
 * The purpose of this class is to find all variable assignments
 * <b>before</b> loop analysis
 */
class PreliminaryLoopVisitor extends JetTreeVisitor<Void> {

    // loop under analysis
    private final JetLoopExpression loopExpression;

    private Set<Name> assignedNames = new LinkedHashSet<Name>();

    public PreliminaryLoopVisitor(JetLoopExpression loopExpression) {
        this.loopExpression = loopExpression;
    }

    public void launch() {
        loopExpression.accept(this, null);
    }

    public DataFlowInfo clearDataFlowInfoForAssignedLocalVariables(DataFlowInfo dataFlowInfo) {
        Map<DataFlowValue, Nullability> nullabilityMap = dataFlowInfo.getCompleteNullabilityInfo();
        Set<DataFlowValue> valueSetToClear = new LinkedHashSet<DataFlowValue>();
        for (DataFlowValue value: nullabilityMap.keySet()) {
            // Only local variables which are not stable are under interest
            if (value.isStableIdentifier() || !value.isLocalVariable())
                continue;
            if (value.getId() instanceof LocalVariableDescriptor) {
                LocalVariableDescriptor descriptor = (LocalVariableDescriptor)value.getId();
                if (assignedNames.contains(descriptor.getName())) {
                    valueSetToClear.add(value);
                }
            }
        }
        for (DataFlowValue valueToClear: valueSetToClear) {
            dataFlowInfo = dataFlowInfo.clearValueInfo(valueToClear);
        }
        return dataFlowInfo;
    }

    @Override
    public Void visitLoopExpression(@NotNull JetLoopExpression loopExpression, Void arg) {
        return super.visitLoopExpression(loopExpression, arg);
    }

    @Override
    public Void visitBinaryExpression(@NotNull JetBinaryExpression binaryExpression, Void arg) {
        if (binaryExpression.getOperationToken() == JetTokens.EQ && binaryExpression.getLeft() instanceof JetNameReferenceExpression) {
            assignedNames.add(((JetNameReferenceExpression) binaryExpression.getLeft()).getReferencedNameAsName());
        }
        return null;
    }

}
