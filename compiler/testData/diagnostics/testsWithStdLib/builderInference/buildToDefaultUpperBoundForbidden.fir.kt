// !LANGUAGE: +ForbidInferringPostponedTypeVariableIntoDeclaredUpperBound

fun <R> build(block: TestInterface<R>.() -> Unit): R = TODO()

interface TestInterface<R> {
    fun set(r: R)
}

fun <S> mat(): S = null!!

fun test() {
    val ret = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> { // Shouldn't infer, see KT-47986
        set(mat())
    }
}
