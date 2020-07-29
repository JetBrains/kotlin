// FILE: SLRUMap.java
// !LANGUAGE: -ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated

import org.jetbrains.annotations.NotNull;
import java.util.List;

public interface SLRUMap<V> {
    void takeV(@NotNull V value);
    <E> void takeE(@NotNull E value);

    void takeVList(@NotNull List<@NotNull V> value);
    <E> void takeEList(@NotNull List<@NotNull E> value);
}

// FILE: main.kt

fun <V> SLRUMap<V>.getOrPut(value: V, l: List<V>) {
    <!INAPPLICABLE_CANDIDATE!>takeV<!>(value)
    takeVList(l)

    takeE(value)
    takeEList(l)

    if (value != null) {
        takeV(value)
        takeE(value)
    }
}
