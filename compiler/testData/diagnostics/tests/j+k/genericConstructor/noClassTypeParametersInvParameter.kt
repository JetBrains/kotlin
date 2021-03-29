// !WITH_NEW_INFERENCE
// FILE: A.java

public class A {
    public <T> A(T x, Inv<T> y) {}
}

// FILE: main.kt

class Inv<T>

fun test(x: Inv<Int>, y: Inv<String>) {
    <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS{OI}!>A<!>("", <!TYPE_MISMATCH{NI}!>x<!>)
    A("", y)

    A<String>("", <!TYPE_MISMATCH, TYPE_MISMATCH, TYPE_MISMATCH!>x<!>)

    A<Any>("", <!TYPE_MISMATCH, TYPE_MISMATCH!>x<!>)
    A<String>("", y)
    A<CharSequence>("", <!TYPE_MISMATCH!>y<!>)
}
