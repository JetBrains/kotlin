// ISSUE: KT-41984

// FILE: A.java

import org.jetbrains.annotations.NotNull;

public abstract class A<T, V> {
    @NotNull
    public abstract String take(@NotNull V value);

    @NotNull
    public abstract String takeInv(@NotNull Inv<@NotNull V> value);
}

// FILE: main.kt

class Inv<T>

open class B<V> : A<Any, V>() {
    override fun take(value: V): String {
        return ""
    }

    override fun takeInv(value: Inv<V>): String = ""
}

fun test_1(b: B<Int>, x: Int, inv: Inv<Int>) {
    b.take(x)
    b.<!INAPPLICABLE_CANDIDATE!>take<!>(null)
    b.takeInv(inv)
}
