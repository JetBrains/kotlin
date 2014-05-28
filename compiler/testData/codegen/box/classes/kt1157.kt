public object SomeClass {
    private val work = object : Runnable {
        override fun run() {
            foo()
        }
    }

    private fun foo(): Unit {
    }

    public fun run(): Unit = work.run()
}

fun box(): String {
    SomeClass.run()
    return "OK"
}
