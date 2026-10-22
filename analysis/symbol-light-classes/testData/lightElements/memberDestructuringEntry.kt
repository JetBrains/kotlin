// KT-90199
data class X(val a: Int, val b: String)

class C {
    var (a, <caret>b) = X(1, "")
}
