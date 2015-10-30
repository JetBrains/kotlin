package test

inline fun f<reified T>(x : () -> Unit) {
    object { init { arrayOf<T>() } }
}
