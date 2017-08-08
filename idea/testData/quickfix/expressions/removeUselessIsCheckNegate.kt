// "Remove useless is check" "true"
fun foo(a: String) {
    if (<caret>1 !is Int) {

    }
}
