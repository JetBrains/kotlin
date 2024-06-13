// RENDER_DIAGNOSTICS_FULL_TEXT
class MutableVector<T>(
    var content: Array<T>,
) {
    inline fun <reified T: Any> foo(block: (T) -> Unit) {
        block(<!ARGUMENT_TYPE_MISMATCH!>content[0]<!>)
    }
}

interface Consumer<T> {
    fun accept(t: T): T
}

fun f(c: Consumer<in String>) {
    c.accept(<!ARGUMENT_TYPE_MISMATCH!>c.accept(<!NULL_FOR_NONNULL_TYPE!>null<!>)<!>)
}

fun test(mapper: Mapper.Impl<*, *>, out: Out<*>) {
    mapper.get().bar(<!ARGUMENT_TYPE_MISMATCH!>out<!>) // K2: argument type mismatch, K1: Ok
}

interface Out<out T> {
    val arg: T
}

abstract class Foo<out R> {
    fun bar(arg: @UnsafeVariance R) {}
}

sealed interface Mapper<out E> {
    sealed interface Impl<out I, F : Foo<Out<I>>> : Mapper<F>

    fun get(): E
}