// FILE: A.java

public class A {
    public <T> A(T x, Inv<T> y) {}
}

// FILE: main.kt

class Inv<T>

fun test(x: Inv<Int>, y: Inv<String>) {
    <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>A<!>("", x)
    A("", y)

    A<String>("", <!TYPE_MISMATCH!>x<!>)

    A<Any>("", <!TYPE_MISMATCH!>x<!>)
    A<String>("", y)
    A<CharSequence>("", <!TYPE_MISMATCH!>y<!>)
}
