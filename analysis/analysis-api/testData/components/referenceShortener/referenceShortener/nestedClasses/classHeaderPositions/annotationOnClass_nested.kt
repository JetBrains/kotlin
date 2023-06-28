// FILE: main.kt

abstract class Outer {
    abstract class Nested : Outer()

    annotation class Ann

    <expr>@Outer.Ann</expr>
    class Foo : Outer()
}

