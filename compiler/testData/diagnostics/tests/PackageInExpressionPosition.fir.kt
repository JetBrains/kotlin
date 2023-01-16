// COMPARE_WITH_LIGHT_TREE
package foo

class X {}

val s = <!EXPRESSION_EXPECTED_PACKAGE_FOUND!>java<!>
val ss = <!NO_COMPANION_OBJECT!>System<!>
val sss = <!NO_COMPANION_OBJECT!>X<!>
val x = "${<!NO_COMPANION_OBJECT!>System<!>}"
val xs = java.<!EXPRESSION_EXPECTED_PACKAGE_FOUND!>lang<!>
val xss = java.lang.<!NO_COMPANION_OBJECT!>System<!>
val xsss = foo.<!NO_COMPANION_OBJECT!>X<!>
val xssss = <!EXPRESSION_EXPECTED_PACKAGE_FOUND!>foo<!>
val f = { <!NO_COMPANION_OBJECT!>System<!> }

fun main() {
    <!EXPRESSION_EXPECTED_PACKAGE_FOUND, VARIABLE_EXPECTED!>java<!> = <!NULL_FOR_NONNULL_TYPE!>null<!>
    <!NO_COMPANION_OBJECT, VARIABLE_EXPECTED!>System<!> = <!NULL_FOR_NONNULL_TYPE!>null<!>
    <!NO_COMPANION_OBJECT!>System<!>!!
    java.lang.<!NO_COMPANION_OBJECT, VARIABLE_EXPECTED!>System<!> = <!NULL_FOR_NONNULL_TYPE!>null<!>
    java.lang.<!NO_COMPANION_OBJECT!>System<!>!!
    <!NO_COMPANION_OBJECT!>System<!> is Int
    <!INVISIBLE_REFERENCE!>System<!>()
    (<!NO_COMPANION_OBJECT!>System<!>)
    foo@ <!NO_COMPANION_OBJECT!>System<!>
    null <!UNRESOLVED_REFERENCE!>in<!> System
}
