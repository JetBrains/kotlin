// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

private class C

private inline fun privateFun() { C() }

internal fun test() {
    privateFun()
}
