package second

annotation class Anno(val str: String)
val constant = "const"

class MyClass {
    val prop = obje<caret>ct : B<@Anno("object $constant") String> {
        override fun foo(x: String) = Unit
    }
}

interface B<T>: C<T>, D<T>

interface C<T> {
    fun foo(x: T = genericCall<T>()) = Unit
    var bar = genericCall<T>()
}

interface D<F> {
    fun foo(x: F = genericCall<F>()) = Unit

    @Anno("property $constant")
    @get:Anno("property $constant")
    @set:Anno("property $constant")
    @setparam:Anno("property $constant")
    var bar = genericCall<T>()
}

fun <T> genericCall(): T = null!!