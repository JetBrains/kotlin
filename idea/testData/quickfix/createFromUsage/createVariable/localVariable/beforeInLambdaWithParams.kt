// "Create local variable 'foo'" "true"
// ACTION: Create parameter 'foo'

fun test(n: Int) {
    val f: (Int, Int) -> Int = { (a, b) -> <caret>foo }
}