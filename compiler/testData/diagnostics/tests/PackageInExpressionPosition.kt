// !WITH_NEW_INFERENCE
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

fun main(args : Array<String>) {
    <!EXPRESSION_EXPECTED_PACKAGE_FOUND!>java<!> = null
    <!NO_COMPANION_OBJECT!>System<!> = null
    <!NO_COMPANION_OBJECT!>System<!>!!
    java.lang.<!NO_COMPANION_OBJECT!>System<!> = null
    java.lang.<!NO_COMPANION_OBJECT!>System<!>!!
    <!NO_COMPANION_OBJECT!>System<!> is Int
    <!INVISIBLE_MEMBER!>System<!>()
    (<!NO_COMPANION_OBJECT!>System<!>)
    foo@ <!NO_COMPANION_OBJECT!>System<!>
    null <!NI;RESULT_TYPE_MISMATCH!>in<!> <!NO_COMPANION_OBJECT!>System<!>
}