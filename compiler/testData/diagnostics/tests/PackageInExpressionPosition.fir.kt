package foo

class X {}

val s = java
val ss = System
val sss = X
val x = "${<!UNRESOLVED_REFERENCE!>System<!>}"
val xs = java.lang
val xss = java.lang.System
val xsss = foo.X
val xssss = foo
val f = { System }

fun main() {
    java = null
    System = null
    System!!
    java.lang.System = null
    java.lang.System!!
    System is Int
    <!INAPPLICABLE_CANDIDATE!>System<!>()
    (System)
    foo@ System
    null <!UNRESOLVED_REFERENCE!>in<!> System
}