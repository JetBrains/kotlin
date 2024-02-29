fun runRunnable(r: () -> Unit) = r()

fun foo() {
    runRunnable {<caret> /* Argument */ }
}
