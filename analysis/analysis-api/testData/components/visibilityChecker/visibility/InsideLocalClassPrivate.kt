fun test() {
    class Local {
        fun foo() {
            p<caret>rintln()
        }

        private fun target() {}
    }
}