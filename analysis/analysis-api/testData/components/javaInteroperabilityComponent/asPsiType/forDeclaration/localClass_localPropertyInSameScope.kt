fun foo() = run {
    class Local {
        fun bar(): Local {
            return this
        }
    }
    val p<caret> = Local().bar()
}
