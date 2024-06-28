// FILE: main.kt

abstract class Outer {
    abstract class Nested(a: Any) : Outer()

    class OtherNested

    class Foo : Outer.Nested(<expr>Outer.OtherNested()</expr>)
}

