class A {
    sealed class Base
}

class Derived : A.Base()

fun test() {
    class DerivedLocal : A.Base()
}
