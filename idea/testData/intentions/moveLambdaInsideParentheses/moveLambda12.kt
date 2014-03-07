// IS_APPLICABLE: true
fun foo() {
    bar<String, Int, Int>("x", 1, 2) <caret>{ it }
}

fun bar<T, V, K>(t: T, v: V, k: K, a: (Int)->Int): Int {
    return a(1)
}