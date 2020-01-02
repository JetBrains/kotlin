// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_ANONYMOUS_PARAMETER

// FILE: A.java
import java.util.Comparator;

public class A<E> {
    public A(Comparator<? super E> comparator) {}
}

// FILE: main.kt

fun foo() {
    val result: A<String> = <!INAPPLICABLE_CANDIDATE!>A<!><String> { x, y -> 1 }
}
