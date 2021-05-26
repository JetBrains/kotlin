class A : <!CYCLIC_INHERITANCE_HIERARCHY!>B<!>() {
    open class Nested<T>
}

typealias ANested<T> = <!RECURSIVE_TYPEALIAS_EXPANSION!>A.Nested<T><!>

open class B : <!CYCLIC_INHERITANCE_HIERARCHY, UNRESOLVED_REFERENCE!>ANested<Int><!>()
