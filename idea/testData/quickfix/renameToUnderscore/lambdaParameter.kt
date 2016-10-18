// "Rename to _" "true"
fun foo(block: (String, Int) -> Unit) {
    block("", 1)
}

fun bar() {
    foo { x<caret>: String, y: Int ->
        y.hashCode()
    }
}
