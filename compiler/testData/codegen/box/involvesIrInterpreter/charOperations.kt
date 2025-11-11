// WITH_STDLIB
// LANGUAGE: +IntrinsicConstEvaluation
fun <T> T.id() = this

const val x = <!EVALUATED("12")!>5 + 7<!>

// STOP_EVALUATION_CHECKS
fun box(): String {

    return "OK"
}
