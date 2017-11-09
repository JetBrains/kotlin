class A {
    fun foo() {}
    val bar = 42
}

val aFoo = A()::foo
val aBar = A()::bar
val A_foo = A::foo
val A_bar = A::bar
