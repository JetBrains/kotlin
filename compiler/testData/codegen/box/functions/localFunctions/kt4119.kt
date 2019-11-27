// IGNORE_BACKEND_FIR: JVM_IR
fun foo(f: (Int?) -> Int): Int {
    return f(0)
}

fun box() : String {
    infix operator fun Int?.plus(a: Int) : Int = a!! + 2

    if (foo { it + 1 } != 3) return "Fail 1"
    if (foo { it plus 1 } != 3) return "Fail 2"

    return "OK"
}