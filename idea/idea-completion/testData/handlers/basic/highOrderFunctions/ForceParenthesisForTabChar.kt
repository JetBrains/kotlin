package testing

fun some(f: () -> Unit) = 12
fun other() = 12

fun test() {
    som<caret>other()
}

// ELEMENT: some
// CHAR: '\t'
