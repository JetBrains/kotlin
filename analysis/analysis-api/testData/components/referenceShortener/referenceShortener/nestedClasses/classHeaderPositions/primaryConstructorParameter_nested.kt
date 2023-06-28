// FILE: main.kt

abstract class Outer {
    abstract class Nested : Outer()

    class Foo(<expr>val param: Outer.Nested = Outer.Nested()</expr>) : Outer.Nested()
}
