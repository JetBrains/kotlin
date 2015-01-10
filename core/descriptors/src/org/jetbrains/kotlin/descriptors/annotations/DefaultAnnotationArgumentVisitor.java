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

package org.jetbrains.kotlin.descriptors.annotations;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.constants.*;
import org.jetbrains.jet.lang.resolve.constants.StringValue;

public abstract class DefaultAnnotationArgumentVisitor<R, D> implements AnnotationArgumentVisitor<R, D> {
    public abstract R visitValue(@NotNull CompileTimeConstant<?> value, D data);

    @Override
    public R visitLongValue(@NotNull LongValue value, D data) {
        return visitValue(value, data);
    }

    @Override
    public R visitIntValue(IntValue value, D data) {
        return visitValue(value, data);
    }

    @Override
    public R visitShortValue(ShortValue value, D data) {
        return visitValue(value, data);
    }

    @Override
    public R visitByteValue(ByteValue value, D data) {
        return visitValue(value, data);
    }

    @Override
    public R visitDoubleValue(DoubleValue value, D data) {
        return visitValue(value, data);
    }

    @Override
    public R visitFloatValue(FloatValue value, D data) {
        return visitValue(value, data);
    }

    @Override
    public R visitBooleanValue(BooleanValue value, D data) {
        return visitValue(value, data);
    }

    @Override
    public R visitCharValue(CharValue value, D data) {
        return visitValue(value, data);
    }

    @Override
    public R visitStringValue(StringValue value, D data) {
        return visitValue(value, data);
    }

    @Override
    public R visitNullValue(NullValue value, D data) {
        return visitValue(value, data);
    }

    @Override
    public R visitErrorValue(ErrorValue value, D data) {
        return visitValue(value, data);
    }

    @Override
    public R visitEnumValue(EnumValue value, D data) {
        return visitValue(value, data);
    }

    @Override
    public R visitArrayValue(ArrayValue value, D data) {
        return visitValue(value, data);
    }

    @Override
    public R visitAnnotationValue(AnnotationValue value, D data) {
        return visitValue(value, data);
    }

    @Override
    public R visitJavaClassValue(JavaClassValue value, D data) {
        return visitValue(value, data);
    }

    @Override
    public R visitNumberTypeValue(IntegerValueTypeConstant value, D data) {
        return visitValue(value, data);
    }
}
