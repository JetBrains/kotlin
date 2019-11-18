// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

fun test(a: Any) = when (a::class) {
    String::class -> "String"
    Int::class -> "Int"
    Boolean::class -> "Boolean"
    else -> "Else"
}

fun box(): String {
    val s = ""
    val i = 0
    val b = false

    if (test(s) != "String") return "Fail 1"
    if (test(i) != "Int") return "Fail 2"
    if (test(b) != "Boolean") return "Fail 3"

    return "OK"
}