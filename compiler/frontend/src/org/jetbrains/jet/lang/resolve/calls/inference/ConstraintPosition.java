/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

/**
 * @author svtk
 */
public abstract class ConstraintPosition {
    public static final ConstraintPosition RECEIVER_POSITION = new ConstraintPosition() {};
    public static final ConstraintPosition EXPECTED_TYPE_POSITION = new ConstraintPosition() {};

    private static final Map<Integer, ValueParameterPosition> valueParameterPositions = Maps.newHashMap();

    public static ConstraintPosition valueParameterPosition(int index) {
        ValueParameterPosition position = valueParameterPositions.get(index);
        if (position == null) {
            position = new ValueParameterPosition(index);
            valueParameterPositions.put(index, position);
        }
        return position;
    }

    private static class ValueParameterPosition extends ConstraintPosition {
        private final int index;

        private ValueParameterPosition(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }
}
