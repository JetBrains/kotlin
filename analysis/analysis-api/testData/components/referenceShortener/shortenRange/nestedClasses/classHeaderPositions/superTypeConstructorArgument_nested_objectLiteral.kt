// FILE: main.kt

abstract class Outer {
    abstract class Nested(a: Any) : Outer()

    class OtherNested

    val foo = object : Outer.Nested(<expr>Outer.OtherNested()</expr>) {}
}

