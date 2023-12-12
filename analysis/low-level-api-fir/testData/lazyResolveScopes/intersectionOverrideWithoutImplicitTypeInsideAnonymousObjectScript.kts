package second

annotation class Anno(val str: String)
val constant = "const"

class MyClass {
    val prop = obje<caret>ct : B<@Anno("super $constant") String> {
        override fun foo(x: String): Unit = Unit
    }
}

interface B<T>: C<T>, D<T>

interface C<T> {
    fun foo(x: T) {}
    var bar: T
}

interface D<F> {
    fun foo(x: F) {}

    @Anno("property $constant")
    @get:Anno("property $constant")
    @set:Anno("property $constant")
    @setparam:Anno("property $constant")
    var bar: F
}
