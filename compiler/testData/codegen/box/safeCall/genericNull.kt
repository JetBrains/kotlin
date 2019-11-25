// IGNORE_BACKEND_FIR: JVM_IR
fun <T : Number?> foo(t: T) {
    t?.toInt()
}

fun box(): String {
    foo<Int?>(null)
    return "OK"
}
