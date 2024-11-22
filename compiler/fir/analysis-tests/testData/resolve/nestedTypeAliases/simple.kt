// RUN_PIPELINE_TILL: BACKEND

/*
class C<T>(val t: T) {
    private inner class Inner {
        fun innerTest() = t
    }

    @Suppress(<!ERROR_SUPPRESSION!>"WRONG_MODIFIER_TARGET"<!>)
    private inner typealias InnerTA = Inner

    fun test(): T = InnerTA().innerTest()
}

fun box() = C<String>("OK").test()
*/

class A

class C {
    typealias TA = A

    fun test(): TA = TA()
}
