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

import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ConstraintPosition {
    public static final ConstraintPosition RECEIVER_POSITION = new ConstraintPosition("RECEIVER_POSITION", true);
    public static final ConstraintPosition EXPECTED_TYPE_POSITION = new ConstraintPosition("EXPECTED_TYPE_POSITION", true);
    public static final ConstraintPosition FROM_COMPLETER = new ConstraintPosition("FROM_COMPLETER", true);
    public static final ConstraintPosition SPECIAL = new ConstraintPosition("SPECIAL", true);

    private static final Map<Integer, ConstraintPosition> valueParameterPositions = new HashMap<Integer, ConstraintPosition>();
    private static final Map<Integer, ConstraintPosition> typeBoundPositions = new HashMap<Integer, ConstraintPosition>();

    public static ConstraintPosition getValueParameterPosition(int index) {
        ConstraintPosition position = valueParameterPositions.get(index);
        if (position == null) {
            position = new ConstraintPosition("VALUE_PARAMETER_POSITION(" + index + ")", true);
            valueParameterPositions.put(index, position);
        }
        return position;
    }

    public static ConstraintPosition getTypeBoundPosition(int index) {
        ConstraintPosition position = typeBoundPositions.get(index);
        if (position == null) {
            position = new ConstraintPosition("TYPE_BOUND_POSITION(" + index + ")", false);
            typeBoundPositions.put(index, position);
        }
        return position;
    }

    public static class CompoundConstraintPosition extends ConstraintPosition {
        private final Collection<ConstraintPosition> positions;

        public CompoundConstraintPosition(Collection<ConstraintPosition> positions) {
            super("COMPOUND_CONSTRAINT_POSITION", hasConstraint(positions, /*strong=*/true));
            this.positions = positions;
        }

        public boolean consistsOfOnlyStrongConstraints() {
            return !hasConstraint(positions, /*strong=*/false);
        }

        private static boolean hasConstraint(@NotNull Collection<ConstraintPosition> positions, final boolean strong) {
            return KotlinPackage.any(positions, new Function1<ConstraintPosition, Boolean>() {
                @Override
                public Boolean invoke(ConstraintPosition constraintPosition) {
                    return constraintPosition.isStrong() == strong;
                }
            });
        }
    }

    public static ConstraintPosition getCompoundConstraintPosition(ConstraintPosition... positions) {
        return new CompoundConstraintPosition(Arrays.asList(positions));
    }

    private final String debugName;
    private final boolean isStrong;

    private ConstraintPosition(String name, boolean isStrong) {
        debugName = name;
        this.isStrong = isStrong;
    }

    public boolean isStrong() {
        return isStrong;
    }

    @Override
    public String toString() {
        return debugName;
    }
}
