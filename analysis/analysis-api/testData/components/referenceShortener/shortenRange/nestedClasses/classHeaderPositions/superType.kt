// FILE: main.kt

abstract class Outer {
    abstract class Nested : Outer()
}

<expr>class Foo : Outer.Nested { constructor(): super() }</expr>