// FILE: main.kt

abstract class Outer {
    abstract class Nested<T> : Outer()

    class OtherNested
}

class Foo : <expr>Outer.Nested<Outer.OtherNested></expr> { constructor(): super() }
