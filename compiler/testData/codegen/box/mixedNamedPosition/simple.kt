// !LANGUAGE: +NewInference +MixedNamedArgumentsInTheirOwnPosition
// IGNORE_BACKEND_FIR: JVM_IR

fun foo(
    p1: Int,
    p2: String,
    p3: Double
) = "$p1 $p2 ${p3.toInt()}"

fun box(): String {
    if (foo(p1 = 1, "2", 3.0) != "1 2 3") return "fail 1"
    if (foo(1, "2", p3 = 3.0) != "1 2 3") return "fail 2"
    if (foo(p1 = 1, p2 = "2", 3.0) != "1 2 3") return "fail 3"

    return "OK"
}
