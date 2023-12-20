// ISSUE: KT-63379

class TestDummyClass<T> {
    fun testFun(t: T) {}
}

fun more(t: TestDummyClass<in CustomRunnable>) {
    t.testFun <!ARGUMENT_TYPE_MISMATCH!>{ Unit }<!>
    t.testFun(CustomRunnable { Unit })
    t.testFun(object : CustomRunnable {
        override fun run() {}
    })
}

fun interface CustomRunnable {
    fun run()
}
