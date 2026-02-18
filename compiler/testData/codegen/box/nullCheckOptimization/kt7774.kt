// FILE: lib.kt
var flag = false

inline fun foo(c: String? = null) {
    if (c != null) {
        flag = true
    }
}

// FILE: main.kt
fun box(): String {
    foo()
    return if (flag) "fail" else "OK"
}