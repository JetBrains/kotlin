// FIR_IDENTICAL
// RENDER_DIAGNOSTICS_FULL_TEXT
// !DIAGNOSTICS: -UNUSED_PARAMETER
@Deprecated("alas", level = DeprecationLevel.ERROR)
fun foo(s: @Foo String) {}

@Deprecated("alas", level = DeprecationLevel.ERROR)
class C

fun test(c: <!DEPRECATION_ERROR!>C<!>) {
    <!DEPRECATION_ERROR!>foo<!>("")
    <!DEPRECATION_ERROR!>C<!>()
}

@Target(AnnotationTarget.TYPE)
annotation class Foo