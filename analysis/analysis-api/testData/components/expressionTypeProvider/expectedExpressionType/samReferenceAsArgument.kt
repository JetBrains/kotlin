fun dummy() = Any()

fun runRunnable(r : java.lang.Runnable) {}

fun test() {
    runRunnable(:<caret>:dummy)
}