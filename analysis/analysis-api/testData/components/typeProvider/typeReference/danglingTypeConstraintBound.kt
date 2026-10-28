// IGNORE_FIR
fun interface TestInterface {
    fun test()
}

inline fun <reified A> A.unusedReceiver() where AA : Test<caret>Interface {
    println(A::class.simpleName)
}
