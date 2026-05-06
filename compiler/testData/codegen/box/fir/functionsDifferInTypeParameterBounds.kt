// ISSUE: KT-63846
// DUMP_IR
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Wasm-JS:2.0,2.1,2.2
// ^^^ KT-63846 is fixed in 2.1.0-Beta1

interface A
open class B : A
open class C : A

abstract class X {
    fun <S1 : A> foo(s: S1): String = when (s) {
        is B -> foo(s)
        is C -> foo(s)
        else -> throw AssertionError(s)
    }

    abstract fun <S2 : B> foo(s: S2): String
    abstract fun <S3 : C> foo(s: S3): String
}

class Y : X() {
    override fun <S4 : B> foo(s: S4): String = "O"
    override fun <S5 : C> foo(s: S5): String = "K"
}

fun box(): String = Y().foo(B()) + Y().foo(C())
