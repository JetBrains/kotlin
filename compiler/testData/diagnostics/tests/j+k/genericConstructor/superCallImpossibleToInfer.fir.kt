// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// CHECK_TYPE
// FILE: A.java

public class A<E> {
    public <T extends E, Q> A(E x, java.util.List<E> y) {}
}

// FILE: main.kt

// TODO: It's effectively impossible to perform super call to such constructor
// if there is not enough information to infer corresponding arguments
// May be we could add some special syntax for such arguments
class B1(x: List<String>) : <!CANNOT_INFER_PARAMETER_TYPE("Q")!>A<CharSequence><!>("", x)
class B2(x: List<Int>) : A<CharSequence>("", <!ARGUMENT_TYPE_MISMATCH!>x<!>)

class C : A<CharSequence> {
    constructor(x: List<String>) : <!CANNOT_INFER_PARAMETER_TYPE("Q")!>super<!>("", x)
    constructor(x: List<Int>, y: Int) : super("", <!ARGUMENT_TYPE_MISMATCH!>x<!>)
}
