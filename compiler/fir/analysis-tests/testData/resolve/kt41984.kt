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

open <!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class B<!><V> : A<Any, V>() {
    <!NOTHING_TO_OVERRIDE!>override<!> fun take(value: V): String {
        return ""
    }

    <!NOTHING_TO_OVERRIDE!>override<!> fun takeInv(value: Inv<V>): String = ""
}

fun test_1(b: B<Int>, x: Int, inv: Inv<Int>) {
    b.<!OVERLOAD_RESOLUTION_AMBIGUITY!>take<!>(x)
    b.<!NONE_APPLICABLE!>take<!>(null)
    b.<!OVERLOAD_RESOLUTION_AMBIGUITY!>takeInv<!>(inv)
}
