interface A

open class Base {
    interface A
}

class Derived : Base(), A {
    class Nested : A
}
