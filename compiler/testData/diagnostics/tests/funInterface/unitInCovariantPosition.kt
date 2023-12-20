// ISSUE: KT-63379 (modified example, see original in unitInContravariantPosition.kt)

class TestDummyClass<T> {
    fun testFun(t: T) {}
}

fun more(t: TestDummyClass<out CustomRunnable>) {
    t.testFun <!TYPE_MISMATCH!>{ Unit }<!>
    t.testFun(<!TYPE_MISMATCH!>CustomRunnable { Unit }<!>)
    t.testFun(<!TYPE_MISMATCH!>object : CustomRunnable<!> {
        override fun run() {}
    })
}

fun interface CustomRunnable {
    fun run()
}
