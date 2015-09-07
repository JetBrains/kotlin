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

package org.jetbrains.kotlin.resolve.calls.smartcasts;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;

/**
 * This class describes an arbitrary object which has some value in data flow analysis.
 * In general case it's some r-value.
 */
public class DataFlowValue {

    public enum Kind {
        STABLE_VALUE("stable"),
        PREDICTABLE_VARIABLE("predictable"),
        UNPREDICTABLE_VARIABLE("unpredictable"),
        OTHER("other");

        @Override
        public String toString() {
            return name;
        }

        Kind(String name) {
            this.name = name;
        }

        private final String name;

        public boolean isStable() {
            return this == STABLE_VALUE;
        }
    }
    
    public static final DataFlowValue NULL = new DataFlowValue(new Object(), KotlinBuiltIns.getInstance().getNullableNothingType(), Kind.OTHER, Nullability.NULL);
    public static final DataFlowValue ERROR = new DataFlowValue(new Object(), ErrorUtils.createErrorType("Error type for data flow"), Kind.OTHER, Nullability.IMPOSSIBLE);

    private final Kind kind;
    private final JetType type;
    private final Object id;
    private final Nullability immanentNullability;

    // Use DataFlowValueFactory
    /*package*/ DataFlowValue(Object id, JetType type, Kind kind, Nullability immanentNullability) {
        this.kind = kind;
        this.type = type;
        this.id = id;
        this.immanentNullability = immanentNullability;
    }

    @Nullable
    public Object getId() {
        return id;
    }

    @NotNull
    public Nullability getImmanentNullability() {
        return immanentNullability;
    }

    public Kind getKind() {
        return kind;
    }

    /**
     * Both stable values and local variables (regardless captured or not) are considered "predictable".
     * Predictable means here we do not expect some sudden change of their values,
     * like accessing mutable properties in another thread.
     */
    public boolean isPredictable() {
        return kind == Kind.STABLE_VALUE || kind == Kind.PREDICTABLE_VARIABLE;
    }

    @NotNull
    public JetType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataFlowValue that = (DataFlowValue) o;

        if (kind.isStable() != that.kind.isStable()) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public String toString() {
        return kind.toString() + (id == null ? null : id.toString()) + " " + immanentNullability;
    }

    @Override
    public int hashCode() {
        int result = kind.isStable() ? 1 : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }
}
