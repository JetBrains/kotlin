// FILE: main.kt

abstract class Outer {
    abstract class Nested : Outer()

    context(<expr>Outer.Nested</expr>)
    class Foo : Outer.Nested()
}

