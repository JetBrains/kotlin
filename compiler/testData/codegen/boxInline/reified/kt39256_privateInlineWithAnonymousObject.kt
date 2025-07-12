// FILE: lib.kt
interface I
class C<T>

inline fun <reified T> C<T>.f() = object : I {
    val unused = T::class
}

// FILE: main.kt
fun box(): String {
    val t1 = C<String>().f()
    val t2 = C<String>().f()
    arrayOf(t1, t2)
    return "OK"
}
