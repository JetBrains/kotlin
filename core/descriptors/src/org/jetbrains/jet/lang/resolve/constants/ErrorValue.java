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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationArgumentVisitor;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

public abstract class ErrorValue extends CompileTimeConstant<Void> {

    public ErrorValue() {
        super(null, true);
    }

    @Override
    @Deprecated // Should not be called, for this is not a real value, but a indication of an error
    public Void getValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R, D> R accept(AnnotationArgumentVisitor<R, D> visitor, D data) {
        return visitor.visitErrorValue(this, data);
    }

    @NotNull
    public static ErrorValue create(@NotNull String message) {
        return new ErrorValueWithMessage(message);
    }

    public static class ErrorValueWithMessage extends ErrorValue {
        private final String message;

        public ErrorValueWithMessage(@NotNull String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @NotNull
        @Override
        public JetType getType(@NotNull KotlinBuiltIns kotlinBuiltIns) {
            return ErrorUtils.createErrorType(message);
        }

        @Override
        public String toString() {
            return getMessage();
        }
    }
}
