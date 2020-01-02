// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// FILE: Outer.java

public class Outer<T> {
    public class Inner<E> {
        public <F extends E, G extends T> Inner(E x, java.util.List<F> y, G z) {}
    }
}

// FILE: main.kt
fun test(x: List<Int>, y: List<String>) {
    Outer<Int>().Inner("", y, 1) checkType { <!UNRESOLVED_REFERENCE!>_<!><Outer<Int>.Inner<String>>() }
    Outer<Int>().<!INAPPLICABLE_CANDIDATE!>Inner<!><CharSequence, String, Int>("", y, 1) <!INAPPLICABLE_CANDIDATE!>checkType<!> { <!UNRESOLVED_REFERENCE!>_<!><Outer<Int>.Inner<CharSequence>>() }

    Outer<Int>().Inner("", x, 1) checkType { <!UNRESOLVED_REFERENCE!>_<!><Outer<Int>.Inner<Any>>() }
    Outer<Int>().<!INAPPLICABLE_CANDIDATE!>Inner<!><CharSequence, String, Int>("", x, 1)
}
