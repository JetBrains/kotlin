// IS_APPLICABLE: true
fun foo() {
      val z = <caret>bar("1", 1, 2, "x")
}

fun bar<T, V, R, K>(t: T, v: V, r: R, k: K): Int = 2