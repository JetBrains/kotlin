// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER
// FILE: Outer.java

public class Outer<E> {
    public class Inner<F> {
        E foo() {}
        F bar() {}

        Outer<E> outer() {}
    }

    Inner<E> baz() { }
    void set(Inner<String> x) {}
}

// FILE: main.kt

fun main() {
    var outerStr: Outer<String> = Outer()
    outerStr.baz().checkType { <!INAPPLICABLE_CANDIDATE!>_<!><Outer<String>.Inner<String>>() }

    val strInt: Outer<String>.Inner<Int> = outerStr.Inner()

    strInt.foo().checkType { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
    strInt.bar().checkType { <!INAPPLICABLE_CANDIDATE!>_<!><Int>() }
    strInt.outer().checkType { <!INAPPLICABLE_CANDIDATE!>_<!><Outer<String>>() }
}
