// FILE: 1.kt

package zzz

inline fun nothing() {}

// FILE: 2.kt

fun box(): String {
    return test {
        "K"
    }
}

inline fun test(p: () -> String): String {
    var pd = ""
    pd = "O"
    return pd + p()
}
