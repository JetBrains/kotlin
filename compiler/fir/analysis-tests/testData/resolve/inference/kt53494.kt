// ISSUE: KT-53494

open class C<out T>
data class Wrapped<out T>(val value: T) : C<T>()
object Default : C<Nothing>()

inline fun <reified T> test_1(t: T?): C<T> {
    if (t != null) return Wrapped(t)
    return if (t is T) Wrapped(t) else Default
}

inline fun <reified T> test_2(t: T?): C<T> {
    return if (t != null) Wrapped(t) else if (t is T) Wrapped(t) else Default
}
