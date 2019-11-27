// IGNORE_BACKEND_FIR: JVM_IR
fun f(b : Long.(Long)->Long) = 1L?.b(2L)

fun box(): String {
    val x = f { this + it }
    return if (x == 3L) "OK" else "fail $x"
}