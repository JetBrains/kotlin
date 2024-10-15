// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
object X

class Y {
    fun f(op: X.() -> Unit) {
        X.op()
    }
}