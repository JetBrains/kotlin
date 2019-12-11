sealed class Base

class Derived: Base() {
    class Derived2: Base()
}

fun test() {
    class Local: Base()
}

