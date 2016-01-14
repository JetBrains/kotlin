// Base for KT-6227
enum class X { A, B, C, D }

fun foo(arg: X): String {
    val res: String
    <!DEBUG_INFO_IMPLICIT_EXHAUSTIVE!>when (arg) {
        X.A -> res = "A"
        X.B -> res = "B"
        X.C -> res = "C"
        X.D -> res = "D"
    }<!>
    return res
}