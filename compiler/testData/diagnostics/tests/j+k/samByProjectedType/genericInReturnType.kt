// !CHECK_TYPE
// FILE: EventListener.java
public interface EventListener<E> {
    E handle(String x);
}

// FILE: A.java
public class A {
    public void foo(EventListener<?> l) {
    }

    public static void bar(EventListener<?> l) {
    }

    public static void baz(EventListener<? extends CharSequence> l) {
    }
}

// FILE: main.kt
fun main() {
    A().foo {
        x -> 1
    }

    A.bar {
        x -> 1
    }


    // baz
    A.baz {
        x -> "" // OK
    }

    A.baz {
        x -> <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>
    }

    val block: (String) -> Any? = {
        x -> 1
    }

    A().foo(block)
    A.bar(block)

    val block2: (String) -> CharSequence? = {
        x -> ""
    }

    A.<!NONE_APPLICABLE!>baz<!>(block)
    A.baz(block2)
}
