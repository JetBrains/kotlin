// FILE: A.java

public class A {
    public <T> A(T x, java.util.List<T> y) {}
}

// FILE: main.kt

fun test(x: List<Int>, y: List<String>) {
    A("", x) // inferred as Any!
    A("", y)

    <!INAPPLICABLE_CANDIDATE!>A<!><String>("", x)

    A<Any>("", x)
    A<String>("", y)
    A<CharSequence>("", y)
}
