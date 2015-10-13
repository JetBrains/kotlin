// !DIAGNOSTICS: -UNUSED_PARAMETER
@Deprecated("alas", level = DeprecationLevel.ERROR)
fun foo() {}

@Deprecated("alas", level = DeprecationLevel.ERROR)
class C

fun test(c: <!DEPRECATION_ERROR!>C<!>) {
    <!DEPRECATION_ERROR!>foo<!>()
    <!DEPRECATION_ERROR!>C<!>()
}