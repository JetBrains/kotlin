// !CHECK_TYPE
// FILE: EventListener.java
public interface EventListener<E> {
    void handle(E e);
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
        x -> x checkType { _<Any?>() }
    }

    A.bar {
        x -> x checkType { _<Any?>() }
    }

    A.baz {
        x -> x checkType { _<CharSequence?>() }
    }
}
