// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// FILE: A.java

public class A<E> {
    public <T extends E> A(E x, java.util.List<T> y) {}
}

// FILE: main.kt

fun test(x: List<Int>, y: List<String>) {
    A("", x) checkType { <!INAPPLICABLE_CANDIDATE!>_<!><A<Any?>>() }
    A("", y) checkType { _<A<String?>>() }

    <!INAPPLICABLE_CANDIDATE!>A<!><CharSequence, String>("", x)
    A<CharSequence, String>("", y)
}
