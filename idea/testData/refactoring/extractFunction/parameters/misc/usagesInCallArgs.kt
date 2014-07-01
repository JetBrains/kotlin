// PARAM_TYPES: A
// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: value-parameter val a: A defined in foo
// PARAM_DESCRIPTOR: val t: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: val u: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: value-parameter val x: kotlin.Int defined in foo
// SIBLING:
public class A() {
    fun bar(a: Int, b: Int): Int {
        return a + b
    }
}

fun foo(a: A, x: Int): Int {
    val t = 10
    val u = 20
    return <selection>a.bar(t - x, u + x)</selection>
}
