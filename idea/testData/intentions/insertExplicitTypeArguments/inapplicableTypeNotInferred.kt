// IS_APPLICABLE: false
// ERROR: Unresolved reference: s
fun foo() {
    val x = <caret>bar(s) // s not definded, can't be inferred
}

fun bar<T>(t: T): Int = 1