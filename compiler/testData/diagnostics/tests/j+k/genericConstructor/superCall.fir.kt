// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE
// FILE: A.java

public class A<E> {
    public <T extends E> A(E x, java.util.List<T> y) {}
}

// FILE: main.kt

class B1(x: List<String>) : A<CharSequence>("", x)
class B2(x: List<Int>) : <!INAPPLICABLE_CANDIDATE!>A<CharSequence><!>("", x)

class C : A<CharSequence> {
    constructor(x: List<String>) : <!INAPPLICABLE_CANDIDATE!>super<!>("", x)
    constructor(x: List<Int>, y: Int) : <!INAPPLICABLE_CANDIDATE!>super<!>("", x)
}
