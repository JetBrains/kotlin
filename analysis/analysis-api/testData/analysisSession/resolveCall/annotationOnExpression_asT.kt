class A<T>(val value : T)
class B<T>

fun <T> A<T>.toB(): B<T> {
    <expr>@Suppress("UNCHECKED_CAST")</expr>
    val v = (value as? Long)?.let { it.toInt() } as T ?: value
    return v
}
