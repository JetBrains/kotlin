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

interface Runnable {
    fun run(): Unit
}

fun box(): String {
    SomeClass.run()
    return "OK"
}
