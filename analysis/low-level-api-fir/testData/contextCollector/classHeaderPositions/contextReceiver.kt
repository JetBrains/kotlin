package test

open class Base {
    class Nested
}

context(<expr>Base</expr>)
class Child : Base() {}