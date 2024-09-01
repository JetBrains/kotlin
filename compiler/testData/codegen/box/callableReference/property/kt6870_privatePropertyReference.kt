// TARGET_BACKEND: JVM

class Test {
    private var iv = 1

    public fun exec() {
        val t = object : Runnable {
            override fun run() {
                Test::iv.get(this@Test)
                Test::iv.set(this@Test, 2)
            }
        }
        t.run()
    }

    fun result() = if (iv == 2) "OK" else "Fail $iv"
}

fun box(): String {
    val t = Test()
    t.exec()
    return t.result()
}
