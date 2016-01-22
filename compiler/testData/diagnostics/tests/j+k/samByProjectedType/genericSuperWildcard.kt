// !CHECK_TYPE
// FILE: EventListener.java
public interface EventListener<E> {
    void handle(E e);
}

// FILE: A.java
public class A {
    public void foo(EventListener<? super CharSequence> l) {
    }

    public static void bar(EventListener<? super String> l) {
    }
}

// FILE: main.kt
fun main() {
    A().foo {
        x -> x checkType { _<CharSequence?>() }
    }

    A.bar {
        x -> x checkType { _<String>() }
    }
}
