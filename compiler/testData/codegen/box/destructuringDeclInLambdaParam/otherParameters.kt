// IGNORE_BACKEND_FIR: JVM_IR
data class A(val x: String, val y: String)

fun foo(a: A, block: (Int, A, String) -> String): String = block(1, a, "#")

fun box(): String {
    val x = foo(A("O", "K")) { i, (x, y), v -> i.toString() + x + y + v }

    if (x != "1OK#") return "fail 1: $x"

    return "OK"
}
