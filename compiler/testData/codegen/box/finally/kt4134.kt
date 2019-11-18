// IGNORE_BACKEND_FIR: JVM_IR
fun <T, R> io(s: R, a: (R) -> T): T {
    try {
        return a(s)
    } finally {
        try {
            s.toString()
        } catch(e: Exception) {
            //NOP
        }
    }
}

fun box() : String {
    return io(("OK"), {it})
}