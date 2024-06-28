// FIR_IDENTICAL
class Key<T>
class Box<T>

fun <T : Any> get(key: Key<in T>): T? = null
fun <T> acceptBox(box: Box<T>) {}

fun <V> test(key: Key<in Box<V>>) {
    // CS of get:
    // Key<CapturedType(in Box<V>)> <: Key<in TypeVariable(T)>
    // If the type is not approximated to subtype before fixation, the type of the lambda parameter `it` becomes Any?
    get(key)?.let { acceptBox(it) }

    val x = get(key)
    x?.let { acceptBox(it) }
}