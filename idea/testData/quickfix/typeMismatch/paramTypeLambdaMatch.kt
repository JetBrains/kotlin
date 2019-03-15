// "Surround with lambda" "true"
fun simple() {
    str(<caret>"foo")
}

fun str(block: () -> String) {}