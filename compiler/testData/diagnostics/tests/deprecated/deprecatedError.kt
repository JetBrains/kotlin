// FIR_IDENTICAL
// RENDER_DIAGNOSTICS_FULL_TEXT
// !DIAGNOSTICS: -UNUSED_PARAMETER
@Deprecated("alas", level = DeprecationLevel.ERROR)
fun foo(s: @Foo String) {}

@Deprecated("alas", level = DeprecationLevel.ERROR)
class C

@field:Foo
@Deprecated("alas", level = DeprecationLevel.ERROR)
val bar: Int = 42

fun test(c: <!DEPRECATION_ERROR!>C<!>) {
    <!DEPRECATION_ERROR!>foo<!>("")
    <!DEPRECATION_ERROR!>C<!>()
    <!DEPRECATION_ERROR!>bar<!>
}

@Target(AnnotationTarget.TYPE, AnnotationTarget.FIELD)
annotation class Foo