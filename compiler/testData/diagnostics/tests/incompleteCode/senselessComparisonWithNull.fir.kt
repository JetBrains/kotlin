// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_EXPRESSION
package d

fun foo(a : IntArray) {
    if (null == <!UNRESOLVED_REFERENCE!>a<!>()<!SYNTAX!><!>
<!SYNTAX!><!>}