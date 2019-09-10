// "Remove redundant label" "true"
fun foo() {
    <caret>L1@ val x = L2@ bar()
}

fun bar() {}