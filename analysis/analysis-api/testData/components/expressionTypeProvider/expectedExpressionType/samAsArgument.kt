fun runRunnable(r: java.lang.Runnable) = r()

fun foo() {
    runRunnable {<caret> /* Argument */ }
}
