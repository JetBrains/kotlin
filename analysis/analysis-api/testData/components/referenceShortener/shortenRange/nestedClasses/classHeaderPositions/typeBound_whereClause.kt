// FILE: main.kt

abstract class Outer {
    abstract class Nested : Outer()
}

class Foo<T> : Outer.Nested() where T : <expr>Outer.Nested</expr>
