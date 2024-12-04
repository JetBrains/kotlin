// FILE: main.kt

abstract class Outer {
    annotation class Ann
}

class Foo <expr>@Outer.Ann</expr> constructor() : Outer()
