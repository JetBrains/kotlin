// "Remove parameter 'x'" "false"
// ACTION: Move lambda argument into parentheses
// ACTION: Remove explicit lambda parameter types (may break code)
// ACTION: Rename to _

fun foo(block: (String, Int) -> Unit) {
    block("", 1)
}

fun bar() {
    foo { x<caret>: String, y: Int ->
        y.hashCode()
    }
}
