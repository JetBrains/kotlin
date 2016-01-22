// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE
// FILE: A.java

public class A<E> {
    public <T extends E> A(E x, java.util.List<T> y) {}
}

// FILE: main.kt

class B1(x: List<String>) : A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><CharSequence><!>("", x)
class B2(x: List<Int>) : A<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><CharSequence><!>("", x)

class C : A<CharSequence> {
    constructor(x: List<String>) : super("", <!TYPE_MISMATCH(kotlin.collections.\(Mutable\)List<T!>!; kotlin.collections.List<kotlin.String>)!>x<!>)
    constructor(x: List<Int>, y: Int) : super("", <!TYPE_MISMATCH!>x<!>)
}
