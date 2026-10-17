// KT-90199
data class X(val a: Int, val b: String)

class C {
    val (a, b) = X(1, "")

    fun use() = <caret>b
}
