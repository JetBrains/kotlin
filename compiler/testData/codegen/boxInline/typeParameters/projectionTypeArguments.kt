// FILE: 1.kt
class Box<T>(
    private var t: T
) {
    fun set(t: T): T {
        this.t = t
        return t
    }
    fun get(): T = t
}

inline fun <U> Box<U>.act(): U {
    val u = get()
    return set(u)
}

fun foo(uuu: Box<*>): Any? {
    val x = uuu.act()
    return x
}

// This code is not compilable
// fun fooInlined(uuu: Box<*>) {
//    val u = get()
//    return set(u)
// }

// FILE: 2.kt
fun box(): String {
    return foo(Box("OK")).toString()
}
