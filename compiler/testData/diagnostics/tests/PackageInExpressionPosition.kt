package foo

class X {}

val s = <!EXPRESSION_EXPECTED_PACKAGE_FOUND!>java<!>
val ss = <!NO_CLASS_OBJECT!>System<!>
val sss = <!NO_CLASS_OBJECT!>X<!>
val x = "${<!NO_CLASS_OBJECT!>System<!>}"
val xs = java.<!EXPRESSION_EXPECTED_PACKAGE_FOUND!>lang<!>
val xss = java.lang.<!NO_CLASS_OBJECT!>System<!>
val xsss = foo.<!NO_CLASS_OBJECT!>X<!>
val xssss = <!EXPRESSION_EXPECTED_PACKAGE_FOUND!>foo<!>
val f = { <!NO_CLASS_OBJECT!>System<!> }

fun main(args : Array<String>) {
    <!EXPRESSION_EXPECTED_PACKAGE_FOUND, VARIABLE_EXPECTED!>java<!> = null
    <!NO_CLASS_OBJECT!>System<!> = null
    <!NO_CLASS_OBJECT!>System<!>!!
    java.lang.<!NO_CLASS_OBJECT!>System<!> = null
    java.lang.<!NO_CLASS_OBJECT!>System<!>!!
    <!NO_CLASS_OBJECT!>System<!> is Int
    <!INVISIBLE_MEMBER!>System<!>()
    (<!NO_CLASS_OBJECT!>System<!>)
    @foo <!NO_CLASS_OBJECT!>System<!>
    null in <!NO_CLASS_OBJECT!>System<!>
}
