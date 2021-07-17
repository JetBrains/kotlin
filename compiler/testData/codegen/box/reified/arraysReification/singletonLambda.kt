// IGNORE_BACKEND: JS, WASM
inline fun <reified U> bar() = U::class.simpleName!!

inline fun <reified T> foo(): String {
    val x = { bar<Array<T>>() }
    return x()
}

fun box(): String {
    val result = foo<Int>()
    return if (result == "Array") "OK" else result
}
