// WITH_STDLIB

class A<caret_base>A : BB<Int>()

open class B<caret_super>B<S> {
    context(_: List<S>)
    fun Generic<S>.foo(): List<S> = TODO()

    context(_: List<S>)
    val Generic<S>.bar: List<S> get() = TODO()
}

class Generic<A>
