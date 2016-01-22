// !DIAGNOSTICS: -UNUSED_VALUE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE
// FILE: A.java

public class A<E> {
    public <T> A(T x, java.util.List<T> y) {}
}

// FILE: main.kt

fun test(x: List<Int>, y: List<String>) {
    var z: A<Double> = A("", x) // E inferred from expected type
    z = A("", y)

    z = A<Double, String>("", <!TYPE_MISMATCH!>x<!>)

    z = A<Double, Any>("", x)
    z = A<Double, String>("", y)
    z = A<Double, CharSequence>("", y)
}
