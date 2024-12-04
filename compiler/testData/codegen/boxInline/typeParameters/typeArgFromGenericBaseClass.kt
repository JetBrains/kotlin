// FILE: 1.kt
open class Box<T>(
    private var t: T
) {
    fun set(t: T): T {
        this.t = t
        return t
    }
    fun get(): T = t
    inline fun act(): T {
        val u = get()
        return set(u)
    }
}
class StringBox(t: String) : Box<String>(t)

fun foo(uuu: StringBox): String {
    val x = uuu.act()
    return x
}

// FILE: 2.kt
fun box(): String {
    return foo(StringBox("OK"))
}