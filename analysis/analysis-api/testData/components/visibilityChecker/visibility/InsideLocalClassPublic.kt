fun test() {
    class Local {
        fun foo() {
            p<caret>rintln()
        }

        fun target() {}
    }
}