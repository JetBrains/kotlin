class A : <!CYCLIC_INHERITANCE_HIERARCHY!>B<!>() {
    open class Nested<T>
}

typealias ANested<T> = A.Nested<T>

open class B : <!CYCLIC_INHERITANCE_HIERARCHY!>ANested<Int><!>()