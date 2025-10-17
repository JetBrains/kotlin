// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

private class C {
    fun f(): C? = null
    internal inline fun test() { f() }
}
