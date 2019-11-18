// !LANGUAGE: +NewInference +MixedNamedArgumentsInTheirOwnPosition
// IGNORE_BACKEND_FIR: JVM_IR

fun foo1(
    vararg p1: Int,
    p2: String,
    p3: Double
) = "${p1[0]} ${p1[1]} $p2 ${p3.toInt()}"

fun foo2(
    p1: Int,
    vararg p2: String,
    p3: Double
) = "$p1 ${p2[0]} ${p2[1]} ${p3.toInt()}"

fun foo3(
    p1: Int,
    p2: String,
    vararg p3: Double
) = "$p1 $p2 ${p3[0].toInt()} ${p3[1].toInt()}"

fun box(): String {
    if (foo1(p1 = *intArrayOf(1, 2), "3", p3 = 4.0) != "1 2 3 4") return "fail 2"

    if (foo2(p1 = 1, "2", "3", p3 = 4.0) != "1 2 3 4") return "fail 3"
    if (foo2(1, p2 = *arrayOf("2", "3"), 4.0) != "1 2 3 4") return "fail 4"

    if (foo3(p1 = 1, "2", 3.0, 4.0) != "1 2 3 4") return "fail 5"
    if (foo3(p1 = 1, "2", p3 = *doubleArrayOf(3.0, 4.0)) != "1 2 3 4") return "fail 6"

    return "OK"
}
