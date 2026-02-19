// FILE: 1.kt

inline fun <T> foo(x: Any?): String {
    return { y: T -> "OK" }.invoke(x as T)
}



// FILE: 2.kt

fun box() = foo<Int>("")