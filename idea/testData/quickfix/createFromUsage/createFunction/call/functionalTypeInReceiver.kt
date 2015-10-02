// "Create extension function 'bar'" "true"

fun foo(block: (Int) -> String) {
    block.b<caret>ar()
}