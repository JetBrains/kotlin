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

package org.jetbrains.jet.lang.resolve.calls.inference;

import com.google.common.collect.Maps;

import java.util.Map;

public class ConstraintPosition {
    public static final ConstraintPosition RECEIVER_POSITION = new ConstraintPosition("RECEIVER_POSITION");
    public static final ConstraintPosition EXPECTED_TYPE_POSITION = new ConstraintPosition("EXPECTED_TYPE_POSITION");
    public static final ConstraintPosition BOUND_CONSTRAINT_POSITION = new ConstraintPosition("BOUND_CONSTRAINT_POSITION");

    private static final Map<Integer, ConstraintPosition> valueParameterPositions = Maps.newHashMap();

    public static ConstraintPosition getValueParameterPosition(int index) {
        ConstraintPosition position = valueParameterPositions.get(index);
        if (position == null) {
            position = new ConstraintPosition("VALUE_PARAMETER_POSITION(" + index + ")");
            valueParameterPositions.put(index, position);
        }
        return position;
    }

    private final String debugName;

    private ConstraintPosition(String name) {
        debugName = name;
    }

    @Override
    public String toString() {
        return debugName;
    }
}
