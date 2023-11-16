// !LANGUAGE: +IntrinsicConstEvaluation
// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JS

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
