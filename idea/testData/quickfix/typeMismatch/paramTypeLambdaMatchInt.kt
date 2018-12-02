// "Surround with lambda" "true"
fun int() {
    i(<caret>123)
}

fun i(block: () -> Long) {}