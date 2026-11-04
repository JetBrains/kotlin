// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtTypeReference

fun interface TestInterface {
    fun test()
}

inline fun <reified A> A.unusedReceiver() where AA : <expr>TestInterface</expr> {
    println(A::class.simpleName)
}
