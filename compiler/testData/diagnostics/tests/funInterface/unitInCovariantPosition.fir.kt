// ISSUE: KT-63379 (modified example, see original in unitInContravariantPosition.kt)

class TestDummyClass<T> {
    fun testFun(t: T) {}
}

fun more(t: TestDummyClass<out CustomRunnable>) {
    t.testFun <!MEMBER_PROJECTED_OUT!>{ Unit }<!>
    t.testFun(<!MEMBER_PROJECTED_OUT!>CustomRunnable { Unit }<!>)
    t.testFun(<!MEMBER_PROJECTED_OUT!>object : CustomRunnable<!> {
        override fun run() {}
    })
}

fun interface CustomRunnable {
    fun run()
}
