// PARAM_TYPES: kotlin.Int
fun bar(n: Int) {
    fun foo(a: Int, b: Int = { <selection>a + n</selection> }.invoke()) = a + b - n - 1
}