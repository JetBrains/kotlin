// FILE: 1.kt

inline fun <T> foo(x: Any?): String {
    with(object {
        fun T.bar() = this
    }) { (x as T).bar() }
    return "OK"
}


// FILE: 2.kt

fun box() = foo<Int>(1)