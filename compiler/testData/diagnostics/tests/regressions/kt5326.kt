class A<T> {
    fun size() = 0
}

class Foo<T>(<!UNUSED_PARAMETER!>i<!>: Int)

public fun <E> Foo(c: A<E>) {
    val <!UNUSED_VARIABLE!>a<!> = Foo<E>(c.size())       // Check no overload resolution ambiguity
    val <!UNUSED_VARIABLE!>b<!>: Foo<E> = Foo(c.size())  // OK
    val <!NAME_SHADOWING, UNUSED_VARIABLE!>c<!>: Foo<Int> = Foo(c.size()) // OK
}