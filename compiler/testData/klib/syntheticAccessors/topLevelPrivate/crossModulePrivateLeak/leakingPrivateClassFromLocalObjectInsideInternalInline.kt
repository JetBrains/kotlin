// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// ^^^ Muted because a private type is leaked from the declaring file, and the visibility validator detects this.
//     This test should be converted to a test that checks reporting private types exposure. To be done in KT-69681 and KT-71416.

// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE
// ^^^ To be fixed in KT-72862: java.lang.IllegalStateException: No class fields for CLASS CLASS name:A

// IGNORE_BACKEND: JVM_IR
// ^^^ java.lang.AssertionError: Trying to inline an anonymous object which is not part of the public ABI: AKt$privateInlineFun$1.
// To be fixed in KT-69666, and the test will not be needed.

// MODULE: lib
// FILE: A.kt
private open class A {
    val ok: String = "OK"
}

private inline fun privateInlineFun() = object : A() {
    fun foo() = super.ok
}.foo()

internal inline fun internalInlineFun() = privateInlineFun()

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    return internalInlineFun()
}
