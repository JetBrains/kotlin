// "Remove single lambda parameter declaration" "false"
// ACTION: Remove explicit lambda parameter types (may break code)
// ACTION: Rename to _
fun test() {
    val f = { <caret>i: Int -> foo() }
    bar(f)
}

fun foo() {}
fun bar(f: (Int) -> Unit) {}
