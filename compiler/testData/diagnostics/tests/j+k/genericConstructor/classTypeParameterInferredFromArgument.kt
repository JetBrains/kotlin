// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// FILE: A.java

public class A<E> {
    public <T extends E> A(E x, java.util.List<T> y) {}
}

// FILE: main.kt

fun test(x: List<Int>, y: List<String>) {
    A("", x) checkType { <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><A<Any?>>() }
    A("", y) checkType { _<A<String?>>() }

    A<CharSequence, String>("", <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, TYPE_MISMATCH!>x<!>)
    A<CharSequence, String>("", y)
}
