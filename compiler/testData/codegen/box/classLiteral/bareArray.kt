// !LANGUAGE: +BareArrayClassLiteral
// IGNORE_BACKEND_FIR: JVM_IR

fun box(): String {
    val x = Array(1) { Any() }
    if (x::class != Array::class) return "Fail"

    return "OK"
}
