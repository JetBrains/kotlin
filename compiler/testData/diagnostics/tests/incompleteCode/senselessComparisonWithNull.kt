// !DIAGNOSTICS: -UNUSED_EXPRESSION
package d

fun foo(a : IntArray) {
    if (<!SENSELESS_COMPARISON!>null == <!FUNCTION_EXPECTED!>a<!>()<!><!SYNTAX!><!>
<!SYNTAX!><!>}