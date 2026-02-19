fun test() {
    class Local {
        fun foo() {
            p<caret>rintln()
        }

        protected fun <caret_target>target() {}
    }
}