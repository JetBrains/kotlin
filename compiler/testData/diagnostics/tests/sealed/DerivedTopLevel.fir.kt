sealed class Base

class Derived: Base() {
    class Derived2: Base()
}

fun test() {
    class Local: <!SEALED_SUPERTYPE_IN_LOCAL_CLASS!>Base<!>()
}

