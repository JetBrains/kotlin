// FILE: SLRUMap.java
// !LANGUAGE: +ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated

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
    takeV(<!TYPE_MISMATCH!>value<!>)
    takeVList(<!TYPE_MISMATCH!>l<!>)

    takeE(<!TYPE_MISMATCH!>value<!>)
    takeEList(<!TYPE_MISMATCH!>l<!>)
    takeE(<!TYPE_MISMATCH!>id(value)<!>)

    if (value != null) {
        takeV(<!DEBUG_INFO_SMARTCAST!>value<!>)
        takeE(<!DEBUG_INFO_SMARTCAST!>value<!>)
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
