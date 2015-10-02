// "Create function 'bar'" "true"

fun foo(block: (Int) -> String) {
    <caret>bar(block)
}