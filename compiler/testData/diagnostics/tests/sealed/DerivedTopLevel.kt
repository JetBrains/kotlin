sealed class Base

class Derived: Base() {
    class Derived2: <!INVISIBLE_MEMBER, SEALED_SUPERTYPE!>Base<!>()
}

fun test() {
    class Local: <!INVISIBLE_MEMBER, SEALED_SUPERTYPE!>Base<!>()
}

