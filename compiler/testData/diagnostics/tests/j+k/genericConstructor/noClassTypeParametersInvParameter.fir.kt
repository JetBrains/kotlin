// !WITH_NEW_INFERENCE
// FILE: A.java

public class A {
    public <T> A(T x, Inv<T> y) {}
}

// FILE: main.kt

class Inv<T>

fun test(x: Inv<Int>, y: Inv<String>) {
    A("", <!ARGUMENT_TYPE_MISMATCH!>x<!>)
    A("", y)

    A<String>("", <!ARGUMENT_TYPE_MISMATCH!>x<!>)

    A<Any>("", <!ARGUMENT_TYPE_MISMATCH!>x<!>)
    A<String>("", y)
    A<CharSequence>("", <!ARGUMENT_TYPE_MISMATCH!>y<!>)
}
