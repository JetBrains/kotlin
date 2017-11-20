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
    Outer<Int>().Inner("", y, 1) checkType { _<Outer<Int>.Inner<String>>() }
    Outer<Int>().Inner<CharSequence, String, Int>("", y, 1) checkType { _<Outer<Int>.Inner<CharSequence>>() }

    Outer<Int>().Inner("", x, 1) checkType { _<Outer<Int>.Inner<Any>>() }
    Outer<Int>().Inner<CharSequence, String, Int>("", <!TYPE_MISMATCH!>x<!>, 1)
}
