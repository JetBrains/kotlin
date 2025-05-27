fun test() {
    class Local {
        fun foo() {
            p<caret>rintln()
        }

        protected fun target() {}
    }
}