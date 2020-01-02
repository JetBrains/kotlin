// !CHECK_TYPE
// FILE: Function.java
public interface Function<E extends java.util.Map<String, Integer>, F extends CharSequence> {
    F handle(E e);
}

// FILE: A.java
public class A {
    public void foo(Function<?, ?> l) {
    }

    public static void bar(Function<?, ?> l) {
    }
}

// FILE: main.kt
fun main() {
    A().foo {
        x ->
        x checkType { <!INAPPLICABLE_CANDIDATE!>_<!><Map<String, Int>?>() }
        ""
    }

    A.bar {
        x ->
        x checkType { <!INAPPLICABLE_CANDIDATE!>_<!><Map<String, Int>>() }
        ""
    }

    val block: (Map<String, Int>) -> CharSequence = { x -> x.toString() }
    A().<!INAPPLICABLE_CANDIDATE!>foo<!>(block)
    A.<!INAPPLICABLE_CANDIDATE!>bar<!>(block)
}
