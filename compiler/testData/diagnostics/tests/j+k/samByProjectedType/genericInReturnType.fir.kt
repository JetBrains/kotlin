// !WITH_NEW_INFERENCE
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
        x -> x.hashCode()
    }

    A.bar {
        x -> x.hashCode()
    }


    // baz
    A.baz {
        x -> x.toString() // OK
    }

    A.baz {
        x -> <!ARGUMENT_TYPE_MISMATCH, TYPE_MISMATCH!>x.hashCode()<!>
    }

    val block: (String) -> Any? = {
        x -> x.hashCode()
    }

    A().foo(block)
    A.bar(block)

    val block2: (String) -> CharSequence? = {
        x -> x.toString()
    }

    A.baz(<!ARGUMENT_TYPE_MISMATCH!>block<!>)
    A.baz(block2)
}
