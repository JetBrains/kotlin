// "Remove useless elvis operator" "true"
fun foo() {}

fun test() {
    foo()
    // comment
    ((({ "" } <caret>?: null)))
}
