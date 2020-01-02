// !WITH_NEW_INFERENCE
// FILE: A.java

public class A {
    public <T> A(T x, Inv<T> y) {}
}

// FILE: main.kt

class Inv<T>

fun test(x: Inv<Int>, y: Inv<String>) {
    <!INAPPLICABLE_CANDIDATE!>A<!>("", x)
    A("", y)

    <!INAPPLICABLE_CANDIDATE!>A<!><String>("", x)

    <!INAPPLICABLE_CANDIDATE!>A<!><Any>("", x)
    A<String>("", y)
    <!INAPPLICABLE_CANDIDATE!>A<!><CharSequence>("", y)
}
