// FIR_IDENTICAL
// LANGUAGE: +BreakContinueInInlineLambdas
// DIAGNOSTICS: -UNREACHABLE_CODE
// ISSUE: KT-1436

open class Test {
    inline fun foo(block: () -> Unit) = block()
    fun bar(block: () -> Unit) = block()
}
class Test2: Test() {
    init {
        loop@ for (i in 0..10) {
            super.foo {
                break
                continue
            }
            super.bar {
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
                <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
            }
        }
    }
}