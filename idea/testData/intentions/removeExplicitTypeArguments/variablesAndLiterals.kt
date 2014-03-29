// IS_APPLICABLE: true
fun foo() {
    val x = "1"
    val y = 2
    val z = <caret>bar<String, Int, Int, String>(x, 1, y, "x")
}

fun bar<T, V, R, K>(t: T, v: V, r: R, k: K): Int = 2