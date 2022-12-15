// WITH_STDLIB
class Outer {
    private inner class Inner {
        fun foo() {
            class ReplaceHappened : Throwable()
            try {
                throw <expr>ReplaceHappened()</expr>
            } catch (_: ReplaceHappened) {
            }
        }
    }
}

