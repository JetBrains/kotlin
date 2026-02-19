fun test() {
    class Local {
        fun foo() {
            object {
                fun foo2() {
                    p<caret>rintln()
                }
            }
        }

        private fun <caret_target>target() {}
    }
}