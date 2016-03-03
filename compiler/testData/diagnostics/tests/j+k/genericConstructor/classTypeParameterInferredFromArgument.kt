// !CHECK_TYPE
// FILE: A.java

public class A<E> {
    public <T extends E> A(E x, java.util.List<T> y) {}
}

// FILE: main.kt

fun test(x: List<Int>, y: List<String>) {
    A("", x) checkType { _<A<Any?>>() }
    A("", y) checkType { _<A<String?>>() }

    A<CharSequence, String>("", <!TYPE_MISMATCH!>x<!>)
    A<CharSequence, String>("", y)
}
