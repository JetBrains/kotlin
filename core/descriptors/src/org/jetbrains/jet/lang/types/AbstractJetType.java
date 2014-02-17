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

package org.jetbrains.jet.lang.types;

import org.jetbrains.jet.lang.types.checker.JetTypeChecker;

import java.util.Iterator;
import java.util.List;

public abstract class AbstractJetType implements JetType {
    @Override
    public final int hashCode() {
        int result = getConstructor().hashCode();
        result = 31 * result + getArguments().hashCode();
        result = 31 * result + (isNullable() ? 1 : 0);
        return result;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JetType)) return false;

        JetType type = (JetType) obj;

        return isNullable() == type.isNullable() && JetTypeChecker.INSTANCE.equalTypes(this, type);
    }

    @Override
    public String toString() {
        List<TypeProjection> arguments = getArguments();
        return getConstructor() + (arguments.isEmpty() ? "" : "<" + argumentsToString(arguments) + ">") + (isNullable() ? "?" : "");
    }

    private static StringBuilder argumentsToString(List<TypeProjection> arguments) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Iterator<TypeProjection> iterator = arguments.iterator(); iterator.hasNext();) {
            TypeProjection argument = iterator.next();
            stringBuilder.append(argument);
            if (iterator.hasNext()) {
                stringBuilder.append(", ");
            }
        }
        return stringBuilder;
    }
}
