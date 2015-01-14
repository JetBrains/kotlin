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

package org.jetbrains.kotlin.cfg;

import org.jetbrains.kotlin.psi.JetLoopExpression;

public class LoopInfo extends BreakableBlockInfo {
    private final Label bodyEntryPoint;
    private final Label bodyExitPoint;
    private final Label conditionEntryPoint;

    public LoopInfo(
            JetLoopExpression loopExpression,
            Label entryPoint,
            Label exitPoint,
            Label bodyEntryPoint,
            Label bodyExitPoint,
            Label conditionEntryPoint
    ) {
        super(loopExpression, entryPoint, exitPoint);
        this.bodyEntryPoint = bodyEntryPoint;
        this.bodyExitPoint = bodyExitPoint;
        this.conditionEntryPoint = conditionEntryPoint;
        markReferablePoints(bodyEntryPoint, bodyExitPoint, conditionEntryPoint);
    }

    @Override
    public JetLoopExpression getElement() {
        return (JetLoopExpression) super.getElement();
    }

    public Label getBodyEntryPoint() {
        return bodyEntryPoint;
    }

    public Label getBodyExitPoint() {
        return bodyExitPoint;
    }

    public Label getConditionEntryPoint() {
        return conditionEntryPoint;
    }
}
