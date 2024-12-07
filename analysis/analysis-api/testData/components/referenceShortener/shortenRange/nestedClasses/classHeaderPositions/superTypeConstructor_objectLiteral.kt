// FILE: main.kt

abstract class Outer {
    abstract class Nested : Outer()
}

<expr>val foo = object : Outer.Nested() {}</expr>