// IGNORE_BACKEND_K1: ANY
var result = ""

class A

fun A?.foo(): String { return "O" }

fun <T> T?.bar(): String { return "K" }

fun box(): String {
    val a = A?::foo
    result += null.a()

    val b = Int?::bar
    result += b(null)
    return result
}