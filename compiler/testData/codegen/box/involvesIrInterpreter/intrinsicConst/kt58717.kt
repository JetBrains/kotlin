// !LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: NATIVE

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
