abstract class C<A> {
    abstract fun f(a: A)
}

fun f() {
    class R

    object : C<R> {
        <caret>
    }
}
