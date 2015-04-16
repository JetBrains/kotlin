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
    
    public static final DataFlowValue NULL = new DataFlowValue(new Object(), KotlinBuiltIns.getInstance().getNullableNothingType(), false, false, Nullability.NULL);
    public static final DataFlowValue NULLABLE = new DataFlowValue(new Object(), KotlinBuiltIns.getInstance().getNullableAnyType(), false, false, Nullability.UNKNOWN);
    public static final DataFlowValue ERROR = new DataFlowValue(new Object(), ErrorUtils.createErrorType("Error type for data flow"), false, false, Nullability.IMPOSSIBLE);

    private final boolean stableIdentifier;
    private final boolean uncapturedlocalVariable;
    private final JetType type;
    private final Object id;
    private final Nullability immanentNullability;

    // Use DataFlowValueFactory
    /*package*/ DataFlowValue(Object id, JetType type, boolean stableIdentifier, boolean uncapturedlocalVariable, Nullability immanentNullability) {
        assert !stableIdentifier || !uncapturedlocalVariable :
                "data flow value cannot be together a stable identifier and an uncaptured local variable";
        this.stableIdentifier = stableIdentifier;
        this.uncapturedlocalVariable = uncapturedlocalVariable;
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

    /**
     * Stable identifier is a non-literal value that is statically known to be immutable
     *
     * NB: this function is no longer public!
     * If you are checking for a possible smart cast, probably you need isPredictable() instead
     */
    private boolean isStableIdentifier() {
        return stableIdentifier;
    }

    /**
     * Identifier is considered a local variable here if it's mutable (var), local and not captured in a closure
     */
    public boolean isUncapturedLocalVariable() {
        return uncapturedlocalVariable;
    }

    /**
     * Both stable identifiers and uncaptured local variables are considered "predictable".
     * Predictable means here we do not expect some sudden change of their values,
     * like accessing mutable properties in another thread or mutable variables from closures.
     */
    public boolean isPredictable() {
        return stableIdentifier || uncapturedlocalVariable;
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

        if (stableIdentifier != that.stableIdentifier) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public String toString() {
        return (stableIdentifier ? "stable " : "unstable ") + (id == null ? null : id.toString()) + " " + immanentNullability;
    }

    @Override
    public int hashCode() {
        int result = (stableIdentifier ? 1 : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }
}
