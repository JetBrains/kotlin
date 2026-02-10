// FILE: lib.kt
package foo

object Obj

inline fun <reified T> check(a: Any): String {
    return if (a is T) "OK" else "fail"
}

// FILE: main.kt
package foo

fun box(): String {
    var x: Any = Obj
    return check<Obj>(x)
}
