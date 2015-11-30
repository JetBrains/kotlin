package test

inline fun <reified T> f(x : () -> Unit) {
    object { init { arrayOf<T>() } }
}
