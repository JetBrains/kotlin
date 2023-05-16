// !LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM_IR
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
