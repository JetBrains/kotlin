// FILE: main.kt

abstract class Outer {
    abstract class Nested : Outer()

    class Foo<T : <expr>Outer.Nested</expr>> : Outer.Nested()
}
