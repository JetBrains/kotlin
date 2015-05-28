// Base for KT-6227
enum class X { A, B, C, D }

fun foo(arg: X?): String {
    var res = "XXX"
    // Should we report something here? Probably not, null is not an enum entry
    when (arg) {
        X.A -> res = "A"
        X.B -> res = "B"
        X.C -> res = "C"
        X.D -> res = "D"
    }
    return res
}