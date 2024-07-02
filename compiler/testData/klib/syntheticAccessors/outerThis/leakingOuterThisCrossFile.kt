// FILE: Outer.kt
class Outer {
    val ok = "OK"
    inner class Inner {
        inline fun publicInlineFun() = ok
    }
}

// FILE: main.kt
fun box(): String {
    return Outer().Inner().publicInlineFun()
}
