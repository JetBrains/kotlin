// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    val r1 = listOf("O", "K", "fail").let {
        (x, y) -> x + y
    }


    if (r1 != "OK") return "fail 1: $r1"

    val r2 = listOf(Pair("O", "K")).map { (x, y) -> x + y }[0]

    if (r2 != "OK") return "fail 2: $r2"

    return "OK"
}
