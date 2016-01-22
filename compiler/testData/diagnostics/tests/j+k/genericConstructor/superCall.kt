// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE
// FILE: A.java

public class A<E> {
    public <T extends E> A(E x, java.util.List<T> y) {}
}

// FILE: main.kt

class B1(x: List<String>) : A<CharSequence>("", x)
class B2(x: List<Int>) : <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>A<CharSequence>("", x)<!>

class C : A<CharSequence> {
    constructor(x: List<String>) : super("", x)
    constructor(x: List<Int>, y: Int) : <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>super<!>("", x)
}
