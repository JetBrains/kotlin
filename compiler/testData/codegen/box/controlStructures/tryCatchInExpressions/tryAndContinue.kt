// IGNORE_BACKEND_FIR: JVM_IR
fun idiv(a: Int, b: Int): Int =
        if (b == 0) throw Exception("Division by zero") else a / b

fun foo(): Int {
    var sum = 0
    for (i in -10 .. 10) {
        sum += try { idiv(100, i) } catch (e: Exception) { continue }
    }
    return sum
}

fun box(): String {
    val test = foo()
    if (test != 0) return "Failed, test=$test"

    return "OK"
}