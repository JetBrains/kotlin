// !SKIP_JAVAC
// !LANGUAGE: -ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated
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

interface Q1<X> : SLRUMap<X> {
    <!NOTHING_TO_OVERRIDE!>override<!> fun takeV(x: X)
    override fun <E1> takeE(e: E1)

    <!NOTHING_TO_OVERRIDE!>override<!> fun takeVList(l: List<X>)
    override fun <E2> takeEList(l2: List<E2>)

    override fun <K2> id(k2: K2): K2
}

interface Q2<X : Any> : SLRUMap<X> {
    override fun takeV(x: X)
    override fun <E1 : Any> takeE(e: E1)

    override fun takeVList(l: List<X>)
    override fun <E2 : Any> takeEList(l2: List<E2>)

    override fun <K2> id(k2: K2): K2
}
