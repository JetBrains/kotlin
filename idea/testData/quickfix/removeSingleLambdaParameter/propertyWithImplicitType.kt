// "Remove single lambda parameter declaration" "false"
// ACTION: Remove explicit lambda parameter types (may break code)
// ACTION: Rename to _
// ACTION: Convert to also
// ACTION: Convert to apply
// ACTION: Convert to multi-line lambda
// ACTION: Enable a trailing comma by default in the formatter
fun test() {
    val f = { <caret>i: Int -> foo() }
    bar(f)
}

fun foo() {}
fun bar(f: (Int) -> Unit) {}
