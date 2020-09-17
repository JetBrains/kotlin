// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER
// !LANGUAGE: +NewInference

fun <T, VR : T> foo(x: T, fn: (VR?/*T?*/, T) -> Unit) {}

fun takeInt(x: Int) {}

fun main(x: Int) {
    foo(x) { prev: Int?, new -> takeInt(new) } // `new` is `Int` in OI, `Int?` in NI
    // It seems, `VR` has been fixed to `Int?` instead of `Int`
}