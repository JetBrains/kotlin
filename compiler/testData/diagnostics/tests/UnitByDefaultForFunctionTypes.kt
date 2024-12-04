// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
fun foo(f : () -> Unit) {
    val x : Unit = f()
}
