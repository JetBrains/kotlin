inline fun String.takeRef(ref: () -> Unit) = this

fun f1() {}
fun String.f2() {}

fun problematic(s: String) =
    s.takeRef(s.takeRef(::f1)::f2)

fun box() = problematic("OK")
