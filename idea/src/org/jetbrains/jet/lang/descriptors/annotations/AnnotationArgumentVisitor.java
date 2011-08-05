package org.jetbrains.jet.lang.descriptors.annotations;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.constants.*;

/**
 * @author abreslav
 */
public interface AnnotationArgumentVisitor<R, D> {
    R visitLongValue(@NotNull LongValue value, D data);

    R visitIntValue(IntValue value, D data);

    R visitErrorValue(ErrorValue value, D data);

    R visitShortValue(ShortValue value, D data);

    R visitByteValue(ByteValue value, D data);

    R visitDoubleValue(DoubleValue value, D data);

    R visitBooleanValue(BooleanValue value, D data);

    R visitCharValue(CharValue value, D data);

    R visitStringValue(StringValue value, D data);

    R visitNullValue(NullValue value, D data);
}
