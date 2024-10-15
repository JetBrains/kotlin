// RUN_PIPELINE_TILL: SOURCE
// DIAGNOSTICS: -UNUSED_EXPRESSION
package d

fun foo(a : IntArray) {
    if (null == <!UNRESOLVED_REFERENCE!>a<!>()<!SYNTAX!><!>
<!SYNTAX!><!>}