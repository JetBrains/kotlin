
package foo

class X {}

val s = <!EXPRESSION_EXPECTED_NAMESPACE_FOUND!>java<!>
val ss = <!NO_CLASS_OBJECT!>System<!>
val sss = <!NO_CLASS_OBJECT!>X<!>
val xs = java.<!EXPRESSION_EXPECTED_NAMESPACE_FOUND!>lang<!>
val xss = java.lang.<!NO_CLASS_OBJECT!>System<!>
val xsss = foo.<!NO_CLASS_OBJECT!>X<!>
val xssss = <!EXPRESSION_EXPECTED_NAMESPACE_FOUND!>foo<!>
