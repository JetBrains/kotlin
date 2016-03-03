// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE
// FILE: A.java

public class A<E> {
    public <T extends E, Q> A(E x, java.util.List<E> y) {}
}

// FILE: main.kt

// TODO: It's effectively impossible to perform super call to such constructor
// if there is not enough information to infer corresponding arguments
// May be we could add some special syntax for such arguments
class B1(x: List<String>) : <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>A<!><CharSequence>("", x)
class B2(x: List<Int>) : <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>A<CharSequence>("", x)<!>

class C : A<CharSequence> {
    constructor(x: List<String>) : <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>super<!>("", x)
    constructor(x: List<Int>, y: Int) : <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>super<!>("", x)
}
