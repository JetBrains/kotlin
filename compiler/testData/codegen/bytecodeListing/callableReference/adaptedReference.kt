class A {
    private fun defaultArgs(value: Int = 0, message: String = "hello"): String = message

    private fun myApply(f: () -> String) {}
    private fun myApplySuspend(f: suspend () -> String) {}

    fun testDefaultArguments() {
        myApply(::defaultArgs)
        myApplySuspend(::defaultArgs)
    }
}
