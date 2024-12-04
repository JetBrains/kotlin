fun dummy() = Any()

fun runRunnable(r : java.lang.Runnable) = r()

fun test() {
    runRunnable(:<caret>:dummy)
}