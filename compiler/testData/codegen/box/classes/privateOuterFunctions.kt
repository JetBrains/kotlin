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

interface Runnable {
    fun run(): Unit
}

fun box() = C().foo()
