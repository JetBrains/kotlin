class A<caret_base>A<T> : BB<T>()

open class B<caret_super>B<S> {
    context(_: S)
    fun foo(): S = TODO()

    fun S.foo2(): S = TODO()

    context(_: S)
    val bar: S get() = TODO()

    val S.bar2: S get() = TODO()
}
