// FIR_IDENTICAL
// !DIAGNOSTICS: -NON_TOPLEVEL_CLASS_DECLARATION

val foo: dynamic = 1

fun bar() {
    class C {
        val foo: dynamic = 1
    }
}