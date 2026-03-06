// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

interface IFoo<T> {
    fun foo(x: T): String = "O"
    fun T.bar(): String = "K"
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class L<T: Long>(val x: T) : IFoo<L<T>>

fun box(): String {
    val z = L(0L)
    return with(z) {
        foo(z) + z.bar()
    }
}