// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER

fun baz(f: (Int) -> String) {}

object Foo {
    fun baz(vararg anys: Any?) {}

    fun testResolvedToMember() {
        baz({ <!CANNOT_INFER_PARAMETER_TYPE!>x<!> -> "" }) // should be an error
    }
}
