// SKIP_JAVAC
// LANGUAGE: +ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated -JavaTypeParameterDefaultRepresentationWithDNN
// FILE: SLRUMap.java

import org.jetbrains.annotations.NotNull;
import java.util.List;

public interface SLRUMap<V> {
    void takeV(@NotNull V value);
    <E> void takeE(@NotNull E value);

    void takeVList(@NotNull List<@NotNull V> value);
    <E> void takeEList(@NotNull List<@NotNull E> value);

    public <K> K id(K value) { return null; }
}

// FILE: main.kt

fun <V> SLRUMap<V>.getOrPut(value: V, l: List<V>) {
    takeV(<!ARGUMENT_TYPE_MISMATCH!>value<!>)
    takeVList(<!ARGUMENT_TYPE_MISMATCH!>l<!>)

    <!CANNOT_INFER_PARAMETER_TYPE!>takeE<!>(<!ARGUMENT_TYPE_MISMATCH!>value<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeEList<!>(<!ARGUMENT_TYPE_MISMATCH!>l<!>)
    <!CANNOT_INFER_PARAMETER_TYPE!>takeE<!>(<!ARGUMENT_TYPE_MISMATCH!>id(value)<!>)

    if (value != null) {
        takeV(value)
        takeE(value)
        takeE(id(value))
    }
}

fun <V : Any> SLRUMap<V>.getOrPutNN(value: V, l: List<V>) {
    takeV(value)
    takeVList(l)

    takeE(value)
    takeEList(l)
    takeE(id(value))
}
