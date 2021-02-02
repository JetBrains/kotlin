// IGNORE_BACKEND: JS, JS_IR
// FILE: 1.kt
@Suppress("CANNOT_CHECK_FOR_ERASED") // TODO: should not be an error
inline fun <reified T> Any?.isArrayOf() = this is Array<T>

@Suppress("UNCHECKED_CAST") // TODO: should not be a warning
inline fun <reified T> Any?.asArrayOf() = this as Array<T>

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Any?.safeAsArrayOf() = this as? Array<T>

// FILE: 2.kt
fun box(): String {
    if (!emptyArray<Int>().isArrayOf<Int>()) return "Array<Int> is Array<Int>"
    if (emptyArray<Int>().isArrayOf<String>()) return "Array<Int> is Array<String>"

    try {
        emptyArray<Int>().asArrayOf<Int>()
    } catch (e: Exception) {
        return "Array<Int> as Array<Int>"
    }
    try {
        emptyArray<Int>().asArrayOf<String>()
        return "Array<Int> as Array<String>"
    } catch (e: Exception) { }

    if (emptyArray<Int>().safeAsArrayOf<Int>() == null) return "Array<Int> as? Array<Int>"
    if (emptyArray<Int>().safeAsArrayOf<String>() != null) return "Array<Int> as? Array<String>"
    return "OK"
}
