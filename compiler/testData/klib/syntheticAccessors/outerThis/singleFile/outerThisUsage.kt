// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS

class Outer {
    val ok = "OK"
    inner class Inner {
        inline fun publicInlineFun() = ok
    }
}

fun box(): String {
    return Outer().Inner().publicInlineFun()
}
