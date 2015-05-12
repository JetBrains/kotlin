// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(f: <!UNSUPPORTED!>This<!>) {}

interface C<T: C<<!UNSUPPORTED!>This<!>>> {
    val x: <!UNSUPPORTED!>This<!>
    val y: <!UNSUPPORTED!>This<!>?
}