// LANGUAGE: +IntrinsicConstEvaluation

var result = "Fail"

object O {
    fun foo() {}

    init {
        result = "OK"
    }
}

fun box(): String {
    O::foo.name
    return result
}
