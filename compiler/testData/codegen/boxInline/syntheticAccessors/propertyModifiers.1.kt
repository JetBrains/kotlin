import test.*

fun box() : String {
    val p = P()

    if (p.testPrivate() != "OK") return "fail 1 ${p.testPrivate()}"

    if (p.testFinal() != "OK") return "fail 2 ${p.testFinal()}"
    return "OK"
}