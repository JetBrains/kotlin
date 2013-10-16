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

package org.jetbrains.jet.lang.resolve.constants;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Collection;

public class IntegerValueTypeConstructor extends NumberValueTypeConstructor {
    private final long value;
    private final Collection<JetType> supertypes = Lists.newArrayList();

    public IntegerValueTypeConstructor(long value) {
        // order of types matters
        // 'getPrimitiveNumberType' returns first of supertypes that is a subtype of expected type
        // for expected type 'Any' result type 'Int' should be returned
        this.value = value;
        checkBoundsAndAddSuperType(value, (long) Integer.MIN_VALUE, (long) Integer.MAX_VALUE, KotlinBuiltIns.getInstance().getIntType());
        checkBoundsAndAddSuperType(value, (long) Byte.MIN_VALUE, (long) Byte.MAX_VALUE, KotlinBuiltIns.getInstance().getByteType());
        checkBoundsAndAddSuperType(value, (long) Short.MIN_VALUE, (long) Short.MAX_VALUE, KotlinBuiltIns.getInstance().getShortType());
        supertypes.add(KotlinBuiltIns.getInstance().getLongType());
    }

    private void checkBoundsAndAddSuperType(long value, long minValue, long maxValue, JetType kotlinType) {
        if (value >= minValue && value <= maxValue) {
            supertypes.add(kotlinType);
        }
    }

    @NotNull
    @Override
    public Collection<JetType> getSupertypes() {
        return supertypes;
    }

    @Override
    public String toString() {
        return "IntegerValueType(" + value + ")";
    }
}
