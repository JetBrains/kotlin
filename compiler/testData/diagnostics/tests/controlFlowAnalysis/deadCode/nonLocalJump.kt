// LANGUAGE: +BreakContinueInInlineLambdas
// WITH_STDLIB
// ISSUE: KT-68277

fun main() {
    while(true) {
        run {
            <!UNSUPPORTED_FEATURE!>break<!>
        }
    }
    <!UNREACHABLE_CODE!>println("hi!")<!>
}
