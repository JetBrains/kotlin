fun foo() {
    class Local {
        fun b<caret>ar(): Local {
            return this
        }
    }
    val a = Local().bar()
}
