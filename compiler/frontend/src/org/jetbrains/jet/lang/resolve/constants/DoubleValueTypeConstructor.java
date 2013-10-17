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
import java.util.List;

public class DoubleValueTypeConstructor extends NumberValueTypeConstructor {
    private final double value;
    private final List<JetType> supertypes;


    public DoubleValueTypeConstructor(double value) {
        this.value = value;
        // order of types matters
        // 'getPrimitiveNumberType' returns first of supertypes that is a subtype of expected type
        // for expected type 'Any' result type 'Double' should be returned
        supertypes = Lists.newArrayList(KotlinBuiltIns.getInstance().getDoubleType(), KotlinBuiltIns.getInstance().getFloatType());
    }

    @NotNull
    @Override
    public Collection<JetType> getSupertypes() {
        return supertypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DoubleValueTypeConstructor that = (DoubleValueTypeConstructor) o;

        if (Double.compare(that.value, value) != 0) return false;

        return true;
    }
}
