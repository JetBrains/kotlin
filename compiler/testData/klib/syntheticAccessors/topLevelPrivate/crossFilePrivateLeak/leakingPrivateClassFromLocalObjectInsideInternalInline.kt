// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// ^^^ Muted because a private type is leaked from the declaring file, and the visibility validator detects this.
//     This test should be converted to a test that checks reporting private types exposure. To be done in KT-69681 and KT-71416.

// KT-72862: Undefined symbols
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE

// FILE: A.kt
private open class A {
    val ok: String = "OK"
}

private inline fun privateInlineFun() = object : A() {
    fun foo() = super.ok
}.foo()

internal inline fun internalInlineFun() = privateInlineFun()

// FILE: main.kt
fun box(): String {
    return internalInlineFun()
}
