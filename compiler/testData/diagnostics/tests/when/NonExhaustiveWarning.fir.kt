// Base for KT-6227
enum class X { A, B, C, D }

fun foo(arg: X): String {
    var res = "XXX"
    when (arg) {
        X.A -> res = "A"
        X.B -> res = "B"
    }
    return res
}