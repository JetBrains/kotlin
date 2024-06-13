// FIR_IDENTICAL
// LANGUAGE: +BreakContinueInInlineLambdas
// DIAGNOSTICS: -UNREACHABLE_CODE
// ISSUE: KT-1436

open class Test {
    open fun bar(block: () -> Unit) = block()
}
class Test2: Test(){
    <!OVERRIDE_BY_INLINE!>override inline fun bar(block: () -> Unit)<!> = block()
}
fun test(a: Test, b: Test2){
    for (i in 0..10) {
        a.bar {
            <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>continue<!>
            <!BREAK_OR_CONTINUE_JUMPS_ACROSS_FUNCTION_BOUNDARY!>break<!>
        }
        (a as Test2).bar {
            continue
            break
        }
        b.bar {
            continue
            break
        }
    }
}