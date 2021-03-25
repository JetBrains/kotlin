// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE
fun foo(f : () -> Unit) {
    val x : Unit = f()
}
