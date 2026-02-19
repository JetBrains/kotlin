// FILE: main.kt

abstract class Outer {
    annotation class Ann
}

<expr>@Outer.Ann</expr>
class Foo : Outer()
