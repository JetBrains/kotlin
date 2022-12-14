fun dummy() = Any()

fun runRunnables(vararg rs : java.lang.Runnable) {
    rs.forEach {
        it.run()
    }
}

fun test() {
    runRunnables(:<caret>:dummy)
}