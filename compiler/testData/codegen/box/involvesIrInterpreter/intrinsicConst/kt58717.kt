// LANGUAGE: +IntrinsicConstEvaluation
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// ^^^ KT-73621: EVALUATED{FIR} is shown instead of EVALUATED

var result = "Fail"

object O {
    fun foo() {}

    init {
        result = "OK"
    }
}

fun box(): String {
    O::foo.<!EVALUATED("foo")!>name<!>
    return result
}
