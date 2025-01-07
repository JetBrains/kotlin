// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-74024

class C

fun f1() {
    <!UNSUPPORTED!>typealias LocalTA = C<!>
}

fun f2() {
    class Local {
        <!UNSUPPORTED!>typealias LocalTA = C<!>
    }
}
