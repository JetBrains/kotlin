// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER

fun baz(f: (Int) -> String) {}

object Foo {
    fun baz(vararg anys: Any?) {}

    fun testResolvedToMember() {
        baz({ x -> "" }) // should be an error
    }
}
