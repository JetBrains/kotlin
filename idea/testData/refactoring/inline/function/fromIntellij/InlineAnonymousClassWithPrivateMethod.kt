object Foo {
    fun <caret>foo() {
        bar(object : Runnable {
            override fun run() {
                doRun()
            }

            private fun doRun() {
                // Woo-hoo
            }
        })
    }

    fun bar(runnable: Runnable) {
        runnable.run()
    }
}

internal object Bar {
    @JvmStatic fun main(args: Array<String>) {
        Foo.foo()
    }
}