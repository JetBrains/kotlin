// !DIAGNOSTICS: -NON_TOPLEVEL_CLASS_DECLARATION

val foo: dynamic = 1

fun foo() {
    class C {
        val foo: dynamic = 1
    }
}
