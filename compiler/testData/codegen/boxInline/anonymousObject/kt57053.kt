// FILE: 1.kt
inline fun foo(): String {
    val obj = object {
        fun localDefault(s: String = "OK") = s
        fun local() = localDefault()
    }
    return obj.local()
}

// FILE: 2.kt
fun box() = foo()