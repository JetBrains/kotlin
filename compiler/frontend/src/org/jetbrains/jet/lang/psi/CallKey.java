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

package org.jetbrains.jet.lang.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.Call.CallType;

public class CallKey {
    private final CallType callType;
    private final JetExpression element;

    public CallKey(CallType callType, JetExpression element) {
        this.callType = callType;
        this.element = element;
    }

    @NotNull
    public static CallKey create(@NotNull CallType callType, @NotNull JetExpression element) {
        return new CallKey(callType, element);
    }

    public static CallKey create(JetExpression element) {
        return new CallKey(CallType.DEFAULT, element);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CallKey)) return false;

        CallKey key = (CallKey) o;

        if (callType != key.callType) return false;
        if (element != null ? !element.equals(key.element) : key.element != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = callType != null ? callType.hashCode() : 0;
        result = 31 * result + (element != null ? element.hashCode() : 0);
        return result;
    }
}
