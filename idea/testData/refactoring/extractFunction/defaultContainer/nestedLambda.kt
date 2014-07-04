// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
class A {
    fun foo(a: Int, b: Int): Int {
        return {
            { <selection>a + b - 1</selection> }.invoke()
        }.invoke()
    }
}