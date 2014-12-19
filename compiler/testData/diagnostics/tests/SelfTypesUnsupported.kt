// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(f: <!UNSUPPORTED!>This<!>) {}

trait C<T: C<<!UNSUPPORTED!>This<!>>> {
    val x: <!UNSUPPORTED!>This<!>
    val y: <!UNSUPPORTED!>This<!>?
}