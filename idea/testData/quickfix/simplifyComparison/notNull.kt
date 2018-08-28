// "Simplify comparison" "true"
fun foo(x: Int) {
    if (<caret>x != null) {
        bar()
    }
}

fun bar() {}