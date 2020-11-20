class A {
    sealed class Base
}

class Derived : <!SEALED_SUPERTYPE!>A.Base<!>()

fun test() {
    class DerivedLocal : <!SEALED_SUPERTYPE_IN_LOCAL_CLASS!>A.Base<!>()
}
