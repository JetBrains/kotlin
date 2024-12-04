// FILE: main.kt

abstract class Outer {
    abstract class Nested : Outer(), Marker

    interface class Marker

    class MarkerImpl() : Marker

    class Foo : Outer.Nested(), <expr>Outer.Marker by Outer.MarkerImpl()</expr>
}
