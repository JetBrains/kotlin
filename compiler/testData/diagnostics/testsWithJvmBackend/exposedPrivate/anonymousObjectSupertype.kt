// FIR_IDENTICAL
// DIAGNOSTICS: -NOTHING_TO_INLINE

private open class C {
    val ok: String = "OK"
}

private inline fun privateFun() = object : C() {
    fun foo() = super.ok
}.foo()

internal inline fun test() = privateFun()
