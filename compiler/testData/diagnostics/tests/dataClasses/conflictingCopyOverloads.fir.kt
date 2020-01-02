// !DIAGNOSTICS: -UNUSED_PARAMETER

data class A(val x: Int, val y: String) {
    fun copy(x: Int, y: String) = x
    fun copy(x: Int, y: String) = A(x, y)
}