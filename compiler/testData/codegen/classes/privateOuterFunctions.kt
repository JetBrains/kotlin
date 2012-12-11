class C {
    private fun String.ext() : String = ""
    private fun f() {}

    public fun foo() : String {
        {
            "".ext()
            f()
        }.invoke()

        object : Runnable {
            public override fun run() {
                "".ext()
                f()
            }
        }.run()

        Inner().innerFun()

        return "OK"
    }

    private inner class Inner() {
        fun innerFun() {
            "".ext()
            f()
        }
    }
}

fun box() = C().foo()