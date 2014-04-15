// IS_APPLICABLE: true
fun foo() {
    <caret>bar<(Int) -> Int> { (it:Int) -> it }
}

fun bar<T>(t: T): Int = 1