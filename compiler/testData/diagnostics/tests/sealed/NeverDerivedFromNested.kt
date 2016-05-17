class A {
    sealed class Base
}

class Derived : <!INVISIBLE_MEMBER, SEALED_SUPERTYPE!>A.Base<!>()

fun test() {
    class DerivedLocal : <!INVISIBLE_MEMBER, SEALED_SUPERTYPE!>A.Base<!>()
}
