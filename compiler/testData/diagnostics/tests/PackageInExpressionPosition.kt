package foo

class X {}

val s = <!EXPRESSION_EXPECTED_PACKAGE_FOUND!>java<!>
val ss = <!NO_DEFAULT_OBJECT!>System<!>
val sss = <!NO_DEFAULT_OBJECT!>X<!>
val x = "${<!NO_DEFAULT_OBJECT!>System<!>}"
val xs = java.<!EXPRESSION_EXPECTED_PACKAGE_FOUND!>lang<!>
val xss = java.lang.<!NO_DEFAULT_OBJECT!>System<!>
val xsss = foo.<!NO_DEFAULT_OBJECT!>X<!>
val xssss = <!EXPRESSION_EXPECTED_PACKAGE_FOUND!>foo<!>
val f = { <!NO_DEFAULT_OBJECT!>System<!> }

fun main(args : Array<String>) {
    <!EXPRESSION_EXPECTED_PACKAGE_FOUND!>java<!> = null
    <!NO_DEFAULT_OBJECT!>System<!> = null
    <!NO_DEFAULT_OBJECT!>System<!>!!
    java.lang.<!NO_DEFAULT_OBJECT!>System<!> = null
    java.lang.<!NO_DEFAULT_OBJECT!>System<!>!!
    <!NO_DEFAULT_OBJECT!>System<!> is Int
    <!INVISIBLE_MEMBER!>System<!>()
    (<!NO_DEFAULT_OBJECT!>System<!>)
    @foo <!NO_DEFAULT_OBJECT!>System<!>
    null in <!NO_DEFAULT_OBJECT!>System<!>
}