// FILE: 1.kt

inline fun <T> foo(x: Any?): String {
    return with(object {
        fun T.bar() = this
    }) { (x as T).bar() } as String
}


// FILE: 2.kt

fun box() = foo<Int>("OK")