class My(protected val x: Int) {
    class Her(protected val x: Int)

    inner class Its(protected val x: Int)
}

object Your {
    protected fun foo() = 3
}

annotation class His(protected val x: Int)

enum class Our(protected val x: Int) {
    FIRST(42) {
        protected fun foo() = 13
    }
}

interface Their {
    protected fun foo() = 7
}