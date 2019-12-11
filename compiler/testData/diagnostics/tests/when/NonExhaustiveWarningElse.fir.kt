// Base for KT-6227
enum class X { A, B, C, D }

fun foo(arg: X): String {
    val res: String
    when (arg) {
        X.A -> res = "A"
        X.B -> res = "B"
        else -> res = "CD"
    }
    return res
}