// IS_APPLICABLE: true
fun foo() {
    val z = bar<caret><String, Int, Int, String>("1", 1, 2, "x")
}

fun <T, V, R, K> bar(t: T, v: V, r: R, k: K): Int = 2