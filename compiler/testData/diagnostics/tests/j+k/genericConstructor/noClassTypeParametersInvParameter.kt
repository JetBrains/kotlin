// !WITH_NEW_INFERENCE
// FILE: A.java

public class A {
    public <T> A(T x, Inv<T> y) {}
}

// FILE: main.kt

class Inv<T>

fun test(x: Inv<Int>, y: Inv<String>) {
    <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>A<!>("", <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH!>x<!>)
    A("", y)

    A<String>("", <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, TYPE_MISMATCH!>x<!>)

    A<Any>("", <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, NI;TYPE_MISMATCH, TYPE_MISMATCH!>x<!>)
    A<String>("", y)
    A<CharSequence>("", <!TYPE_MISMATCH!>y<!>)
}
