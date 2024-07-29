// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS

// MODULE: lib
// FILE: Outer.kt
class Outer {
    val ok = "OK"
    inner class Inner {
        inline fun publicInlineFun() = ok
    }
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    return Outer().Inner().publicInlineFun()
}
