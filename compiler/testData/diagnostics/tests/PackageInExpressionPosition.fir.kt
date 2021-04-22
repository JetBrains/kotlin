package foo

class X {}

val s = java
val ss = System
val sss = X
val x = "${System}"
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
    <!INVISIBLE_REFERENCE!>System<!>()
    (System)
    foo@ System
    null <!UNRESOLVED_REFERENCE!>in<!> System
}
