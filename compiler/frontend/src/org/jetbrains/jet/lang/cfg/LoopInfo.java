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

package org.jetbrains.jet.lang.cfg;

import com.google.common.collect.Sets;
import org.jetbrains.jet.lang.psi.JetElement;

import java.util.Set;

public class LoopInfo extends BreakableBlockInfo {
    private final Label bodyEntryPoint;
    private final Label conditionEntryPoint;

    public LoopInfo(JetElement element, Label entryPoint, Label exitPoint, Label bodyEntryPoint, Label conditionEntryPoint) {
        super(element, entryPoint, exitPoint);
        this.bodyEntryPoint = bodyEntryPoint;
        this.conditionEntryPoint = conditionEntryPoint;
        referablePoints.add(bodyEntryPoint);
        referablePoints.add(conditionEntryPoint);
    }

    public Label getBodyEntryPoint() {
        return bodyEntryPoint;
    }

    public Label getConditionEntryPoint() {
        return conditionEntryPoint;
    }
}
