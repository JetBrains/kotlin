// !LANGUAGE: +NewInference +MixedNamedArgumentsInTheirOwnPosition
// IGNORE_BACKEND_FIR: JVM_IR

fun foo(
    p1: Int = 1,
    p2: String = "2",
    p3: Double = 3.0,
    p4: Char = '4'
) = "$p1 $p2 ${p3.toInt()} $p4"

fun box(): String {
    if (foo(p1 = 1, "2", 3.0) != "1 2 3 4") return "fail 1"
    if (foo(1, p2 = "2", 3.0) != "1 2 3 4") return "fail 2"
    if (foo(1, "2", p3 = 3.0) != "1 2 3 4") return "fail 3"

    if (foo(p1 = 1) != "1 2 3 4") return "fail 4"
    if (foo(1, p2 = "2") != "1 2 3 4") return "fail 5"
    
    if (foo(p1 = 1, p2 = "2", 3.0) != "1 2 3 4") return "fail 6"

    if (foo(1, p2 = "2") != "1 2 3 4") return "fail 7"

    return "OK"
}
