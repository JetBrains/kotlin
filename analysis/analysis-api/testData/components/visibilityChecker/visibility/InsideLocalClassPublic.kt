fun test() {
    class Local {
        fun foo() {
            p<caret>rintln()
        }

        fun <caret_target>target() {}
    }
}