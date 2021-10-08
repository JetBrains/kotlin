private fun f<caret>oo() = run {
    class Local {
        fun bar(): Local {
            return this
        }
    }
    val p = Local().bar()
}
